package edu.illinois.library.cantaloupe.resource;

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

    private boolean bypassCache = false;
    private ImageInfo imageInfo;
    private OperationList opList;
    private Processor processor;

    /**
     * @param imageInfo
     * @param processor Processor configured for writing the image.
     * @param opList
     * @param disposition
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
     * Writes the image requested in the constructor to the given output
     * stream, either retrieving it from the derivative cache, or getting it
     * from a processor (and caching it if so configured) as appropriate.
     *
     * @param outputStream Response body output stream.
     * @throws IOException
     */
    @Override
    public void write(OutputStream outputStream) throws IOException {
        // N.B. We don't need to close outputStream after writing to it;
        // Restlet will take care of that.
        if (!bypassCache) {
            // The cache will be null if caching is disabled.
            final DerivativeCache cache = CacheFactory.getDerivativeCache();
            if (cache != null) {
                // Try to get the image from the cache.
                try (InputStream inputStream = cache.getImageInputStream(opList)) {
                    if (inputStream != null) {
                        // The image is available in the cache; write it to the
                        // response output stream.
                        IOUtils.copy(inputStream, outputStream);
                    } else {
                        // Create a TeeOutputStream to write to the response
                        // output stream and the cache pseudo-simultaneously.
                        // Restlet will close outputStream, but cacheOutputStream
                        // is our responsibility. (teeStream doesn't matter,
                        // although the finalizer may close it, so it's important
                        // that these two output streams' close() method can deal
                        // with being called twice.)
                        try (OutputStream cacheOutputStream =
                                     cache.getImageOutputStream(opList)) {
                            OutputStream teeStream = new TeeOutputStream(
                                    outputStream, cacheOutputStream);
                            doWrite(teeStream);
                        } catch (IOException e) {
                            // This typically happens when the connection has
                            // been closed prematurely, as in the case of e.g.
                            // the client hitting the stop button. The cached
                            // image has been incompletely written and is
                            // corrupt, so it must be purged.
                            logger.info("write(): {}", e.getMessage());
                            cache.purge(opList);
                        }
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                }
            } else {
                try {
                    doWrite(outputStream);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        } else {
            try {
                doWrite(outputStream);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * @param outputStream Either the response output stream, or a tee stream
     *                     for writing to the response and the cache
     *                     pseudo-simultaneously. Will not be closed.
     * @throws Exception
     */
    private void doWrite(OutputStream outputStream) throws Exception {
        final Stopwatch watch = new Stopwatch();
        // If the operations are effectively a no-op, the source image can be
        // streamed through with no processing.
        if (opList.isNoOp(processor.getSourceFormat())) {
            if (processor instanceof FileProcessor &&
                    ((FileProcessor) processor).getSourceFile() != null) {
                final File sourceFile =
                        ((FileProcessor) processor).getSourceFile();
                try (InputStream inputStream = new FileInputStream(sourceFile)) {
                    IOUtils.copy(inputStream, outputStream);
                }
            } else {
                final StreamSource streamSource =
                        ((StreamProcessor) processor).getStreamSource();
                try (InputStream inputStream = streamSource.newInputStream()) {
                    IOUtils.copy(inputStream, outputStream);
                }
            }
            logger.debug("Streamed with no processing in {} msec: {}",
                    watch.timeElapsed(), opList);
        } else {
            processor.process(opList, imageInfo, outputStream);

            logger.debug("{} processed in {} msec: {}",
                    processor.getClass().getSimpleName(),
                    watch.timeElapsed(), opList);
        }
    }

}
