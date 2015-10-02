package edu.illinois.library.cantaloupe.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.media.imageioimpl.plugins.jpeg2000.ImageInputStreamWrapper;
import edu.illinois.library.cantaloupe.ImageServerApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.resolver.FileResolver;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resolver.StreamResolver;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.restlet.data.CacheDirective;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.OutputRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;

/**
 * Handles IIIF image requests.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#image-request-parameters">Image
 * Request Parameters</a>
 */
public class ImageResource extends AbstractResource {

    /**
     * Restlet representation for images, returned by ImageResource.doGet().
     *
     * <em>Note:</em> doGet() should handle all preflight checks. Once it has
     * returned an instance of this class, it will no longer be possible to
     * render the error page, as response headers will have already been sent.
     */
    private class ImageRepresentation extends OutputRepresentation {

        File file;
        ImageInputStream inputStream;
        Parameters params;
        SourceFormat sourceFormat;

        /**
         * Constructor for images from InputStreams.
         *
         * @param mediaType
         * @param sourceFormat
         * @param params
         * @param inputStream
         */
        public ImageRepresentation(MediaType mediaType,
                                   SourceFormat sourceFormat,
                                   Parameters params,
                                   ImageInputStream inputStream) {
            super(mediaType);
            this.inputStream = inputStream;
            this.params = params;
            this.sourceFormat = sourceFormat;
        }

        /**
         * Constructor for images from Files.
         *
         * @param mediaType
         * @param sourceFormat
         * @param params
         * @param file
         */
        public ImageRepresentation(MediaType mediaType,
                                   SourceFormat sourceFormat,
                                   Parameters params, File file) {
            super(mediaType);
            this.file = file;
            this.params = params;
            this.sourceFormat = sourceFormat;
        }

        /**
         * Writes the source image to the given output stream.
         *
         * @param outputStream Response body output stream supplied by Restlet
         * @throws IOException
         */
        public void write(OutputStream outputStream) throws IOException {
            Cache cache = CacheFactory.getInstance();
            if (cache != null) {
                try (InputStream cacheStream = cache.getImageInputStream(this.params)) {
                    if (cacheStream != null) {
                        IOUtils.copy(cacheStream, outputStream);
                    } else {
                        TeeOutputStream tos = new TeeOutputStream(outputStream,
                                cache.getImageOutputStream(this.params));
                        doWrite(tos, cache);
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                }
            } else {
                doWrite(outputStream);
            }
        }

        private void doWrite(OutputStream outputStream) throws IOException {
            try {
                long msec = System.currentTimeMillis();
                // if the parameters request an unmodified source image, it can
                // be streamed right through
                if (this.params.isUnmodified()) {
                    if (this.file != null) {
                        IOUtils.copy(new FileInputStream(this.file),
                                outputStream);
                    } else {
                        IOUtils.copy(
                                new ImageInputStreamWrapper(this.inputStream),
                                outputStream);
                    }
                    logger.debug("Streamed with no processing in {} msec",
                            System.currentTimeMillis() - msec);
                } else {
                    Processor proc = ProcessorFactory.
                            getProcessor(this.sourceFormat);
                    if (proc instanceof FileProcessor) {
                        ((FileProcessor) proc).process(this.params,
                                this.sourceFormat, this.file, outputStream);
                    } else if (proc instanceof StreamProcessor) {
                        ((StreamProcessor) proc).process(this.params,
                                this.sourceFormat, this.inputStream, outputStream);
                    }
                    logger.debug("{} processed in {} msec",
                            proc.getClass().getSimpleName(),
                            System.currentTimeMillis() - msec);
                }
            } catch (Exception e) {
                throw new IOException(e);
            } /*finally {
                TODO: doesn't work with Java2dProcessor.process()
                try {
                    if (this.inputStream != null) {
                        this.inputStream.close();
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }*/
        }

        /**
         * Variant of doWrite() that cleans up incomplete cached images when
         * the connection has been interrupted.
         *
         * @param outputStream
         * @param cache
         * @throws IOException
         */
        private void doWrite(OutputStream outputStream, Cache cache)
                throws IOException {
            try {
                doWrite(outputStream);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                cache.flush(this.params);
            }
        }

    }

    private static Logger logger = LoggerFactory.getLogger(ImageResource.class);

    private static final List<CacheDirective> CACHE_DIRECTIVES =
            getCacheDirectives();

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        getResponseCacheDirectives().addAll(CACHE_DIRECTIVES);
    }

    /**
     * Responds to IIIF Image requests.
     *
     * @return ImageRepresentation
     * @throws Exception
     */
    @Get
    public ImageRepresentation doGet() throws Exception {
        // Assemble the URI parameters into a Parameters object
        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = Reference.decode((String) attrs.get("identifier"));
        String format = (String) attrs.get("format");
        String region = (String) attrs.get("region");
        String size = (String) attrs.get("size");
        String rotation = (String) attrs.get("rotation");
        String quality = (String) attrs.get("quality");
        Parameters params = new Parameters(identifier, region, size, rotation,
                quality, format);
        // Get a reference to the source image (this will also cause an
        // exception if not found)
        Resolver resolver = ResolverFactory.getResolver();
        // Determine the format of the source image
        SourceFormat sourceFormat = resolver.getSourceFormat(identifier);
        // Obtain an instance of the processor assigned to that format in
        // the config file
        Processor proc = ProcessorFactory.getProcessor(sourceFormat);

        File inputFile = null;
        ImageInputStream inputStream = null;
        if (resolver instanceof FileResolver) {
            inputFile = ((FileResolver)resolver).getFile(identifier);
            inputStream = ((StreamResolver)resolver).
                    getInputStream(identifier);
        } else {
            if (!(proc instanceof StreamProcessor)) {
                // StreamProcessors require StreamResolvers
                throw new UnsupportedSourceFormatException(
                        String.format("%s is not compatible with %s",
                                proc.getClass().getSimpleName(),
                                resolver.getClass().getSimpleName()));
            }
            inputStream = ((StreamResolver)resolver).
                    getInputStream(identifier);
        }
        // Find out whether the processor supports that source format by
        // asking it whether it offers any output formats for it
        Set availableOutputFormats = proc.getAvailableOutputFormats(sourceFormat);
        if (!availableOutputFormats.contains(params.getOutputFormat())) {
            String msg = String.format("%s does not support the \"%s\" output format",
                    proc.getClass().getSimpleName(),
                    params.getOutputFormat().getExtension());
            logger.warn(msg + ": " + this.getReference());
            throw new UnsupportedSourceFormatException(msg);
        }
        // 6. All checks made; at this point, we are pretty sure we can fulfill
        // the request
        this.addHeader("Link", String.format("<%s%s/%s>;rel=\"canonical\"",
                this.getRootRef().toString(),
                ImageServerApplication.BASE_IIIF_PATH,
                params.toString()));

        MediaType mediaType = new MediaType(
                OutputFormat.valueOf(format.toUpperCase()).getMediaType());
        if (resolver instanceof StreamResolver &&
                !(proc instanceof StreamProcessor)) {
            return new ImageRepresentation(mediaType, sourceFormat, params,
                    inputFile);
        }
        return new ImageRepresentation(mediaType, sourceFormat, params,
                inputStream);
    }

}
