package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Restlet representation for images.
 */
public class ImageRepresentation extends OutputRepresentation {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(ImageRepresentation.class);

    private boolean bypassCache = false;
    private Info imageInfo;
    private OperationList opList;
    private Processor processor;

    /**
     * @param imageInfo
     * @param processor   Processor configured for writing the image.
     * @param opList      Will be frozen, if it isn't already.
     * @param disposition
     * @param bypassCache If true, the cache will not be written to nor read
     *                    from, regardless of whether caching is enabled in the
     *                    application configuration.
     */
    public ImageRepresentation(final Info imageInfo,
                               final Processor processor,
                               final OperationList opList,
                               final Disposition disposition,
                               final boolean bypassCache) {
        super(new org.restlet.data.MediaType(
                opList.getOutputFormat().getPreferredMediaType().toString()));
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
     * @param responseOutputStream Response body output stream.
     */
    @Override
    public void write(OutputStream responseOutputStream) throws IOException {
        // N.B. We don't need to close outputStream after writing to it;
        // Restlet will take care of that.
        if (!bypassCache) {
            final CacheFacade cacheFacade = new CacheFacade();
            if (cacheFacade.isDerivativeCacheAvailable()) {
                // Try to get the image from the cache.
                try (InputStream cacheInputStream =
                             cacheFacade.newDerivativeImageInputStream(opList)) {
                    if (cacheInputStream != null) {
                        // The image is available in the cache; write it to the
                        // response output stream.
                        final Stopwatch watch = new Stopwatch();
                        IOUtils.copy(cacheInputStream, responseOutputStream);

                        LOGGER.debug("Streamed from {} in {} msec: {}",
                                cacheFacade.getDerivativeCache().getClass().getSimpleName(),
                                watch.timeElapsed(),
                                opList);
                    } else {
                        // Create a TeeOutputStream to write to the response
                        // output stream and the cache pseudo-simultaneously.
                        //
                        // N.B.: The contract for this method says we can't
                        // close responseOutputStream, which means we also
                        // can't close teeOutputStream (because that would
                        // close its wrapped streams). So, we have to leave it
                        // up to the finalizer. But, when the finalizer closes
                        // teeOutputStream, the end result will be close()
                        // having been called twice on both of its wrapped
                        // streams. So, it's important that these two output
                        // streams' close() methods can deal with being called
                        // twice.
                        try (OutputStream cacheOutputStream =
                                     cacheFacade.newDerivativeImageOutputStream(opList)) {
                            OutputStream teeStream = new TeeOutputStream(
                                    responseOutputStream, cacheOutputStream);
                            LOGGER.debug("Writing to the response & " +
                                    "derivative cache simultaneously");
                            doWrite(teeStream);
                        } catch (Throwable e) {
                            // The cached image has been incompletely written
                            // and is corrupt, so it must be purged. This may
                            // happen in response to a VM error like
                            // OutOfMemoryError, or when the connection has
                            // been closed prematurely, as in the case of e.g.
                            // the client hitting the stop button.
                            LOGGER.info("write(): {}", e.getMessage());
                            cacheFacade.purge(opList);

                            doWrite(responseOutputStream);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to read from the derivative cache: {}",
                            e.getMessage(), e);
                    doWrite(responseOutputStream);
                }
            } else {
                LOGGER.debug("Derivative cache not available; writing " +
                        "directly to the response");
                doWrite(responseOutputStream);
            }
        } else {
            LOGGER.debug("Writing directly to the response, bypassing the cache");
            doWrite(responseOutputStream);
        }
    }

    /**
     * @param outputStream Either the response output stream, or a tee stream
     *                     for writing to the response and the cache
     *                     pseudo-simultaneously. Will not be closed.
     */
    private void doWrite(OutputStream outputStream)
            throws IOException {
        final Stopwatch watch = new Stopwatch();
        // If the operations are effectively a no-op, the source image can be
        // streamed through with no processing.
        if (!opList.hasEffect(processor.getSourceFormat())) {
            if (processor instanceof FileProcessor &&
                    ((FileProcessor) processor).getSourceFile() != null) {
                File sourceFile = ((FileProcessor) processor).getSourceFile();
                Files.copy(sourceFile.toPath(), outputStream);
            } else {
                StreamSource streamSource =
                        ((StreamProcessor) processor).getStreamSource();
                try (InputStream inputStream = streamSource.newInputStream()) {
                    IOUtils.copy(inputStream, outputStream);
                }
            }
            LOGGER.debug("Streamed with no processing in {} msec: {}",
                    watch.timeElapsed(), opList);
        } else {
            try {
                processor.process(opList, imageInfo, outputStream);

                LOGGER.debug("{} processed in {} msec: {}",
                        processor.getClass().getSimpleName(),
                        watch.timeElapsed(), opList);
            } catch (ProcessorException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

}
