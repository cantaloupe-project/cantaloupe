package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.CompletableOutputStream;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.FormatException;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Representation that {@link Processor#process} processes} an image and writes
 * the result to the response (possibly also caching it).
 */
public class ImageRepresentation implements Representation {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageRepresentation.class);

    private final boolean bypassCacheRead, bypassCacheWrite;
    private final Info imageInfo;
    private final OperationList opList;
    private final Processor processor;

    /**
     * @param imageInfo        Info corresponding to the source image.
     * @param processor        Processor configured for writing the image.
     * @param opList           Instance describing the image.
     * @param bypassCacheRead  If {@code true}, the cache will not be read
     *                         from.
     * @param bypassCacheWrite If {@code true}, the cache will not be written
     *                         to.
     */
    public ImageRepresentation(final Info imageInfo,
                               final Processor processor,
                               final OperationList opList,
                               final boolean bypassCacheRead,
                               final boolean bypassCacheWrite) {
        this.imageInfo        = imageInfo;
        this.processor        = processor;
        this.opList           = opList;
        this.bypassCacheRead  = bypassCacheRead;
        this.bypassCacheWrite = bypassCacheWrite;
    }

    /**
     * Writes the image requested in the constructor to the given output
     * stream, either retrieving it from the derivative cache, or getting it
     * from a processor (and caching it if so configured) as appropriate.
     */
    @Override
    public void write(OutputStream responseOS) throws IOException {
        // If we are bypassing the cache, write directly to the response.
        if (bypassCacheWrite) {
            LOGGER.debug("Bypassing the cache and writing only to the response");
            copyOrProcess(responseOS);
            return;
        }

        // If no derivative cache is available, write directly to the response.
        final CacheFacade cacheFacade = new CacheFacade();
        if (!cacheFacade.isDerivativeCacheAvailable()) {
            LOGGER.debug("Derivative cache not available; writing directly " +
                    "to the response");
            copyOrProcess(responseOS);
            return;
        }

        // If we aren't bypassing cache reads, and a derivative cache is
        // available, try to copy the image from the cache to the response.
        if (!bypassCacheRead) {
            final Optional<DerivativeCache> optCache = cacheFacade.getDerivativeCache();
            if (optCache.isPresent()) {
                DerivativeCache cache = optCache.get();
                try (InputStream cacheIS = cache.newDerivativeImageInputStream(opList)) {
                    if (cacheIS != null) {
                        // The image is available, so write it to the response.
                        final Stopwatch watch = new Stopwatch();
                        cacheIS.transferTo(responseOS);

                        LOGGER.debug("Streamed from {} in {}: {}",
                                cache.getClass().getSimpleName(), watch, opList);
                        return;
                    }
                } catch (IOException e) {
                    LOGGER.debug("Error while streaming from {} to the response: {}",
                            cache.getClass().getSimpleName(),
                            e.getMessage());
                    // It may still be possible to fulfill the request.
                    copyOrProcess(responseOS);
                    return;
                }
            }
        }

        // At this point, a derivative cache may be available, but it doesn't
        // contain an image that can fulfill the request. So, we will create a
        // TeeOutputStream to write to the response output stream and the cache
        // pseudo-simultaneously.
        //
        // N.B.: Closing responseOS is the Servlet container's responsibility.
        // This means we also can't close teeOS, because doing so would close
        // its wrapped streams. So, we have to leave its closure up to the
        // finalizer. But, when teeOS is closed, its wrapped streams' close()
        // methods will have been called twice, so it's important that these
        // two streams' close() methods can deal with that.
        try (CompletableOutputStream cacheOS =
                     cacheFacade.newDerivativeImageOutputStream(opList)) {
            if (cacheOS != null) {
                OutputStream teeOS = new TeeOutputStream(responseOS, cacheOS);
                LOGGER.debug("Writing to the response & derivative " +
                        "cache simultaneously");
                copyOrProcess(teeOS);
                cacheOS.flush();
                cacheOS.setComplete(true);
            } else {
                copyOrProcess(responseOS);
            }
        } catch (IOException e) {
            LOGGER.debug("write(): {}", e.getMessage(), e);
            // TODO: uncommenting this can cause KakaduNativeProcessor to crash
            //  the JVM (see
            //  https://github.com/cantaloupe-project/cantaloupe/issues/303).
            //  First, we need to reset the processor correctly. Then, we
            //  should move this into a catch block for a cache-specific
            //  exception (which does not exist yet as of 4.1.x).
            //copyOrProcess(responseOS);
        } catch (Throwable t) {
            LOGGER.error("write(): {}", t.getMessage(), t);
            throw t;
        }
    }

    /**
     * If {@link #opList} {@link OperationList#hasEffect(Dimension, Format) has
     * no effect}, streams the image from its source. Otherwise, invokes
     * {@link Processor#process}. The output is either the response output
     * stream, or a tee stream that writes to the response and cache
     * pseudo-simultaneously.
     *
     * @param responseOS Will not be closed.
     */
    private void copyOrProcess(OutputStream responseOS) throws IOException {
        // If the operations are effectively a no-op, the source image can be
        // streamed through with no processing.
        if (!opList.hasEffect(imageInfo.getSize(), imageInfo.getSourceFormat())) {
            copy(responseOS);
        } else {
            try {
                process(responseOS);
            } catch (ProcessorException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    /**
     * Copies a source resource directly to a given {@link OutputStream} with
     * no processing or caching.
     */
    private void copy(OutputStream responseOS) throws IOException {
        boolean done = false;
        final Stopwatch watch = new Stopwatch();

        if (processor instanceof FileProcessor) {
            Path sourceFile = ((FileProcessor) processor).getSourceFile();
            if (sourceFile != null) {
                Files.copy(sourceFile, responseOS);
                done = true;
            }
        }
        if (!done && processor instanceof StreamProcessor) {
            StreamFactory streamFactory =
                    ((StreamProcessor) processor).getStreamFactory();
            if (streamFactory != null) {
                try (InputStream sourceIS = streamFactory.newInputStream()) {
                    sourceIS.transferTo(responseOS);
                }
            }
        }
        LOGGER.debug("Streamed with no processing in {}: {}", watch, opList);
    }

    private void process(OutputStream outputStream)
            throws FormatException, ProcessorException {
        final Stopwatch watch = new Stopwatch();

        processor.process(opList, imageInfo, outputStream);

        LOGGER.debug("{} processed in {}: {}",
                processor.getClass().getSimpleName(), watch, opList);
    }

}
