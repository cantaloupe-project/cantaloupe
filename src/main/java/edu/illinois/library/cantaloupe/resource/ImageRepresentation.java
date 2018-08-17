package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.restlet.data.Disposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Restlet representation for images.
 */
public class ImageRepresentation extends CustomOutputRepresentation {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageRepresentation.class);

    private boolean bypassCache;
    private Info imageInfo;
    private OperationList opList;
    private Processor processor;

    /**
     * @param imageInfo   Info corresponding to the source image.
     * @param processor   Processor configured for writing the image.
     * @param opList      Will be frozen, if it isn't already.
     * @param disposition
     * @param bypassCache If {@literal true}, the cache will not be written to
     *                    nor read from, regardless of whether caching is
     *                    enabled in the application configuration.
     */
    public ImageRepresentation(final Info imageInfo,
                               final Processor processor,
                               final OperationList opList,
                               final Disposition disposition,
                               final boolean bypassCache,
                               final Callable<?> onRelease) {
        super(new org.restlet.data.MediaType(
                opList.getOutputFormat().getPreferredMediaType().toString()));
        this.imageInfo = imageInfo;
        this.processor = processor;
        this.opList = opList;
        this.bypassCache = bypassCache;
        this.setDisposition(disposition);
        this.onRelease = onRelease;
    }

    @Override
    public void release() {
        super.release();
        processor.close();
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
        // N.B.: Restlet will close responseOutputStream.

        // If we are bypassing the cache, write directly to the response.
        if (bypassCache) {
            LOGGER.debug("Bypassing the cache and writing directly to the response");
            doWrite(responseOutputStream);
            return;
        }

        // If no derivative cache is available, write directly to the response.
        final CacheFacade cacheFacade = new CacheFacade();
        if (!cacheFacade.isDerivativeCacheAvailable()) {
            LOGGER.debug("Derivative cache not available; writing directly " +
                    "to the response");
            doWrite(responseOutputStream);
            return;
        }

        // A derivative cache is available, so try to copy the image from the
        // cache to the response.
        final DerivativeCache cache = cacheFacade.getDerivativeCache();
        try (InputStream cacheInputStream = cache.newDerivativeImageInputStream(opList)) {
            if (cacheInputStream != null) {
                // The image is available, so write it to the response.
                final Stopwatch watch = new Stopwatch();
                IOUtils.copy(cacheInputStream, responseOutputStream);

                LOGGER.debug("Streamed from {} in {}: {}",
                        cache.getClass().getSimpleName(), watch, opList);
                return;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read from {}: {}",
                    cache.getClass().getSimpleName(), e.getMessage(), e);
            doWrite(responseOutputStream);
            return;
        }

        // At this point, a derivative cache is available, but it doesn't
        // contain an image that can fulfill the request. So, we will create a
        // TeeOutputStream to write to the response output stream and the cache
        // pseudo-simultaneously.
        //
        // N.B.: The contract for this method says we can't close
        // responseOutputStream, which means we also can't close
        // teeOutputStream, because that would close its wrapped streams. So,
        // we have to leave it up to the finalizer. But, when the finalizer
        // closes teeOutputStream, its close() method will have been called
        // twice on both of its wrapped streams. It's therefore important that
        // these two output streams' close() methods can deal with being called
        // twice.
        try (OutputStream cacheOutputStream =
                     cacheFacade.newDerivativeImageOutputStream(opList)) {
            OutputStream teeOutputStream = new TeeOutputStream(
                    responseOutputStream, cacheOutputStream);
            LOGGER.debug("Writing to the response & derivative " +
                    "cache simultaneously");
            doWrite(teeOutputStream);
        } catch (Throwable e) {
            // The cached image has been incompletely written and is corrupt,
            // so it must be purged. This may happen in response to a VM error
            // like OutOfMemoryError, or when the connection has been closed
            // prematurely, as in the case of e.g. the client hitting the stop
            // button.
            if (e.getMessage() != null && e.getMessage().contains("heap space")) {
                LOGGER.error("write(): out of heap space! " +
                        "Consider adjusting your -Xmx JVM argument.");
            } else {
                LOGGER.debug("write(): {}", e.getMessage());
            }
            cacheFacade.purge(opList);

            doWrite(responseOutputStream);
        }
    }

    /**
     * @param outputStream Either the response output stream, or a tee stream
     *                     for writing to the response and the cache
     *                     pseudo-simultaneously. Will not be closed.
     */
    private void doWrite(OutputStream outputStream) throws IOException {
        final Stopwatch watch = new Stopwatch();
        // If the operations are effectively a no-op, the source image can be
        // streamed through with no processing.
        if (!opList.hasEffect(imageInfo.getSize(), imageInfo.getSourceFormat())) {
            if (processor instanceof FileProcessor &&
                    ((FileProcessor) processor).getSourceFile() != null) {
                Path sourceFile = ((FileProcessor) processor).getSourceFile();
                Files.copy(sourceFile, outputStream);
            } else {
                StreamFactory streamFactory =
                        ((StreamProcessor) processor).getStreamFactory();
                try (InputStream inputStream = streamFactory.newInputStream()) {
                    IOUtils.copy(inputStream, outputStream);
                }
            }
            LOGGER.debug("Streamed with no processing in {}: {}",
                    watch, opList);
        } else {
            try {
                processor.process(opList, imageInfo, outputStream);

                LOGGER.debug("{} processed in {}: {}",
                        processor.getClass().getSimpleName(), watch, opList);
            } catch (ProcessorException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

}
