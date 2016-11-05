package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheException;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.ImageInfo;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.restlet.data.Disposition;
import org.restlet.representation.OutputRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Restlet representation for images.
 */
public class ImageRepresentation extends OutputRepresentation {

    private static Logger logger = LoggerFactory.
            getLogger(ImageRepresentation.class);

    static final String FILENAME_CHARACTERS = "[^A-Za-z0-9._-]";

    private boolean bypassCache = false;
    private ImageInfo imageInfo;
    private OperationList opList;
    private Processor processor;

    /**
     * @param imageInfo
     * @param opList
     * @param disposition
     * @param processor
     * @param bypassCache If true, the cache will not be written to nor read
     *                    from, regardless of whether caching is enabled in the
     *                    application configuration.
     */
    public ImageRepresentation(final ImageInfo imageInfo,
                               final Processor processor,
                               final OperationList opList,
                               final Disposition disposition,
                               final boolean bypassCache) {
        super(opList.getOutputFormat().getPreferredMediaType());
        this.imageInfo = imageInfo;
        this.processor = processor;
        this.opList = opList;
        this.bypassCache = bypassCache;
        this.setDisposition(disposition);
    }

    /**
     * Writes the source image to the given output stream.
     *
     * @param outputStream Response body output stream supplied by Restlet
     * @throws IOException
     */
    @Override
    public void write(OutputStream outputStream) throws IOException {
        if (!bypassCache) {
            final DerivativeCache cache = CacheFactory.getDerivativeCache();
            if (cache != null) {
                OutputStream cacheOutputStream = null;
                try (InputStream inputStream = cache.getImageInputStream(opList)) {
                    if (inputStream != null) {
                        // A cached image is available; write it to the response
                        // output stream.
                        IOUtils.copy(inputStream, outputStream);
                    } else {
                        // Create a TeeOutputStream to write to the response
                        // output stream and the cache pseudo-simultaneously.
                        cacheOutputStream = cache.getImageOutputStream(opList);
                        OutputStream teeStream = new TeeOutputStream(
                                outputStream, cacheOutputStream);
                        doCacheAwareWrite(teeStream, cache);
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                } finally {
                    if (cacheOutputStream != null) {
                        cacheOutputStream.close();
                    }
                }
            } else {
                doWrite(outputStream);
            }
        } else {
            doWrite(outputStream);
        }
    }

    /**
     * Variant of doWrite() that cleans up incomplete cached images when
     * the connection has been broken.
     *
     * @param outputStream
     * @param cache
     * @throws CacheException
     */
    private void doCacheAwareWrite(OutputStream outputStream,
                                   DerivativeCache cache)
            throws CacheException {
        try {
            doWrite(outputStream);
        } catch (IOException e) {
            logger.info(e.getMessage());
            cache.purge(this.opList);
        }
    }

    private void doWrite(OutputStream outputStream) throws IOException {
        try {
            final Stopwatch watch = new Stopwatch();
            // If the operations are effectively a no-op, the source image can
            // be streamed through with no processing.
            if (opList.isNoOp(processor.getSourceFormat())) {
                if (processor instanceof FileProcessor &&
                        ((FileProcessor) processor).getSourceFile() != null) {
                    final File sourceFile = ((FileProcessor) processor).getSourceFile();
                    final InputStream inputStream = new FileInputStream(sourceFile);
                    IOUtils.copy(inputStream, outputStream);
                } else {
                    final StreamSource streamSource = ((StreamProcessor) processor).getStreamSource();
                    final InputStream inputStream = streamSource.newInputStream();
                    IOUtils.copy(inputStream, outputStream);
                }
                logger.debug("Streamed with no processing in {} msec: {}",
                        watch.timeElapsed(), opList);
            } else {
                processor.process(opList, imageInfo, outputStream);

                logger.debug("{} processed in {} msec: {}",
                        processor.getClass().getSimpleName(),
                        watch.timeElapsed(), opList);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
