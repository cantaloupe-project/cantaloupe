package edu.illinois.library.cantaloupe.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import edu.illinois.library.cantaloupe.ImageServerApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
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
         * Writes the source image to the given output stream.
         *
         * @param outputStream Response body output stream supplied by Restlet
         * @throws IOException
         */
        public void write(OutputStream outputStream) throws IOException {
            try {
                Cache cache = CacheFactory.getCache();
                if (cache != null) {
                    InputStream cacheStream = cache.get(this.params);
                    if (cacheStream != null) {
                        IOUtils.copy(cacheStream, outputStream);
                    } else {
                        TeeOutputStream tos = new TeeOutputStream(outputStream,
                                cache.getOutputStream(this.params));
                        doWrite(tos, cache);
                    }
                } else {
                    doWrite(outputStream);
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        private void doWrite(OutputStream outputStream) throws IOException {
            try {
                Processor proc = ProcessorFactory.
                        getProcessor(this.sourceFormat);
                long msec = System.currentTimeMillis();
                proc.process(this.params, this.sourceFormat,
                        this.inputStream, outputStream);
                logger.debug("{} processed in {} msec",
                        proc.getClass().getSimpleName(),
                        System.currentTimeMillis() - msec);
            } catch (Exception e) {
                throw new IOException(e);
            }
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
                cache.flush(this.params);
            }
        }

    }

    private static Logger logger = LoggerFactory.getLogger(ImageResource.class);

    @Get
    public Representation doGet() throws Exception {
        // 1. Assemble the URI parameters into a Parameters object
        Map<String,Object> attrs = this.getRequest().getAttributes();
        String identifier = Reference.decode((String) attrs.get("identifier"));
        String format = (String) attrs.get("format");
        String region = (String) attrs.get("region");
        String size = (String) attrs.get("size");
        String rotation = (String) attrs.get("rotation");
        String quality = (String) attrs.get("quality");
        Parameters params = new Parameters(identifier, region, size, rotation,
                quality, format);
        // 2. Get a reference to the source image (this will also cause an
        // exception if not found)
        Resolver resolver = ResolverFactory.getResolver();
        ImageInputStream inputStream = resolver.getInputStream(identifier);
        // 3. Determine the format of the source image
        SourceFormat sourceFormat = resolver.getSourceFormat(identifier);
        // 4. Obtain an instance of the processor assigned to that format in
        // the config file
        Processor proc = ProcessorFactory.getProcessor(sourceFormat);
        // 5. Find out whether the processor supports that source format by
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
        return new ImageRepresentation(mediaType, sourceFormat, params,
                inputStream);
    }

}
