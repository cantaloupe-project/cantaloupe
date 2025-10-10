package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.source.StatResult;
import edu.illinois.library.cantaloupe.status.HealthChecker;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * <p>High-level image request handler. Use the return value of {@link
 * #builder()} to create new instances.</p>
 *
 * <p>This class provides a simple interface that endpoints can use to convert
 * client arguments into images. Simplicity is achieved by abstracting away as
 * much of the tediousness of image request handling (caching, format
 * detection, connecting {@link Source sources} to {@link Processor
 * processors}, etc.) as possible. There is also no coupling to any particular
 * protocol.</p>
 *
 * @author Alex Dolski UIUC
 * @since 5.0
 */
public class ImageRequestHandler extends AbstractRequestHandler
        implements AutoCloseable {

    /**
     * Callback for various events that occur during a call to {@link
     * ImageRequestHandler#handle(OutputStream)}.
     */
    public interface Callback {

        /**
         * <p>Performs pre-authorization using an {@link
         * edu.illinois.library.cantaloupe.auth.Authorizer}.</p>
         *
         * <p>{@link #willProcessImage(Processor, Info)} has not yet been
         * called.</p>
         *
         * @return Authorization result.
         */
        boolean preAuthorize() throws Exception;

        /**
         * <p>Performs authorization using an {@link
         * edu.illinois.library.cantaloupe.auth.Authorizer}.</p>
         *
         * <p>{@link #willProcessImage(Processor, Info)} has not yet been
         * called.</p>
         *
         * @return Authorization result.
         */
        boolean authorize() throws Exception;

        /**
         * Called immediately after the source image has first been accessed.
         *
         * @param result Information about the source image.
         */
        void sourceAccessed(StatResult result);

        /**
         * Called when image information is available; always before {@link
         * #willProcessImage(Processor, Info)} and {@link
         * #willStreamImageFromDerivativeCache()}.
         *
         * @param info Efficiently obtained instance.
         */
        void infoAvailable(Info info) throws Exception;

        /**
         * <p>Called when a hit is found in the derivative cache. In this case,
         * no further processing will be necessary and the streaming will begin
         * very soon after this method returns.</p>
         *
         * <p>If a hit is not found in the derivative cache, this method is not
         * called.</p>
         *
         * <p>This method tends to be called relatively early. No other
         * callback methods will be called after this one.</p>
         */
        void willStreamImageFromDerivativeCache() throws Exception;

        /**
         * <p>All setup is complete and processing will begin very soon after
         * this method returns.</p>
         *
         * <p>This method tends to be called last.</p>
         *
         * @param processor Instance that will do the processing.
         * @param info      Efficiently obtained instance.
         */
        void willProcessImage(Processor processor, Info info) throws Exception;

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageRequestHandler.class);

    private Callback callback;
    private OperationList operationList;
    private Future<Path> tempFileFuture;


    /**
     * Creates a new ImageRequestHandler with full configuration options.
     *
     * @param operationList        Operation list to process.
     * @param delegateProxy        Delegate proxy.
     * @param requestContext       Request context.
     * @param callback             Callback to receive events during request handling.
     * @param isBypassingCache     True to bypass cache reads and writes.
     * @param isBypassingCacheRead True to bypass cache reads only.
     */
    public ImageRequestHandler(OperationList operationList,
                               DelegateProxy delegateProxy,
                               RequestContext requestContext,
                               Callback callback,
                               boolean isBypassingCache,
                               boolean isBypassingCacheRead) {
        this.operationList = operationList;
        this.delegateProxy = delegateProxy;
        this.requestContext = requestContext;
        this.callback = callback;
        this.isBypassingCache = isBypassingCache;
        this.isBypassingCacheRead = isBypassingCacheRead;
    }

    /**
     * Closes the instance. N.B.: this does not close the {@link OutputStream}
     * supplied to {@link #handle(OutputStream)}.
     */
    @Override
    public void close() {
        // If a temporary file was created in the course of handling the
        // request, it will need to be deleted.
        if (tempFileFuture != null) {
            try {
                Path tempFile = tempFileFuture.get();
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            } catch (Exception e) {
                LOGGER.error("destroy(): {}", e.getMessage(), e);
            }
        }
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    /**
     * Handles an image request.
     *
     * @param outputStream Stream to write the resulting image to. Will not be
     *                     closed.
     */
    public void handle(OutputStream outputStream) throws Exception {
        if (!callback.preAuthorize()) {
            return;
        }

        final Identifier identifier   = operationList.getIdentifier();
        final Configuration config    = Configuration.getInstance();
        final CacheFacade cacheFacade = new CacheFacade();

        Iterator<Format> formatIterator = Collections.emptyIterator();
        boolean isFormatKnownYet = false;

        // If we are using a cache, and don't need to resolve first:
        // 1. If the cache contains an image matching the request, skip all the
        //    setup and just return the cached image.
        // 2. Otherwise, if the cache contains a relevant info, get it to avoid
        //    having to get it from a source later.
        if (!isBypassingCache && !isBypassingCacheRead && !isResolvingFirst()) {
            final Optional<Info> optInfo = cacheFacade.getInfo(identifier);
            if (optInfo.isPresent()) {
                Info info = optInfo.get();
                operationList.applyNonEndpointMutations(info, delegateProxy);

                InputStream cacheStream = null;
                try {
                    cacheStream = cacheFacade.newDerivativeImageInputStream(operationList);
                } catch (IOException e) {
                    // Don't rethrow -- it's still possible to service the
                    // request.
                    LOGGER.error(e.getMessage());
                }

                if (cacheStream != null) {
                    callback.infoAvailable(info);
                    callback.willStreamImageFromDerivativeCache();
                    new InputStreamRepresentation(cacheStream).write(outputStream);
                    return;
                } else {
                    Format infoFormat = info.getSourceFormat();
                    if (infoFormat != null) {
                        formatIterator = Collections.singletonList(infoFormat).iterator();
                        isFormatKnownYet = true;
                    }
                }
            }
        }

        final Source source = new SourceFactory().newSource(
                identifier, delegateProxy);

        // If we are resolving first, or if the source image is not present in
        // the source cache (if enabled), check access to it in preparation for
        // retrieval.
        final Optional<Path> sourceImage = cacheFacade.getSourceCacheFile(identifier);
        if (sourceImage.isEmpty() || isResolvingFirst()) {
            try {
                StatResult result = source.stat();
                callback.sourceAccessed(result);
            } catch (NoSuchFileException e) { // this needs to be rethrown!
                if (config.getBoolean(Key.CACHE_SERVER_PURGE_MISSING, false)) {
                    // If the image was not found, purge it from the cache.
                    cacheFacade.purgeAsync(operationList.getIdentifier());
                }
                throw e;
            }
        }

        if (!isFormatKnownYet) {
            // If we are not resolving first, and there is a hit in the source
            // cache, read the format from the source-cached-file, as we expect
            // source cache access to be more efficient.
            // Otherwise, read it from the source.
            if (!isResolvingFirst() && sourceImage.isPresent()) {
                List<MediaType> mediaTypes =
                        MediaType.detectMediaTypes(sourceImage.get());
                if (!mediaTypes.isEmpty()) {
                    formatIterator = mediaTypes
                            .stream()
                            .map(MediaType::toFormat)
                            .iterator();
                }
            } else {
                formatIterator = source.getFormatIterator();
            }
        }

        while (formatIterator.hasNext()) {
            final Format format = formatIterator.next();
            // Obtain an instance of the processor assigned to this format.
            String processorName = "unknown processor";
            try (Processor processor = new ProcessorFactory().newProcessor(format)) {
                processorName = processor.getClass().getSimpleName();

                // Connect it to the source.
                tempFileFuture = new ProcessorConnector().connect(
                        source, processor, identifier, format);

                final Info info = getOrReadInfo(
                        operationList.getIdentifier(),
                        processor);
                callback.infoAvailable(info);

                Dimension fullSize;
                try {
                    fullSize = info.getSize(operationList.getPageIndex());
                    requestContext.setMetadata(info.getMetadata());
                    requestContext.setOperationList(operationList, fullSize);
                    requestContext.setPageCount(info.getNumPages());
                    // This must be done *after* the request context is fully
                    // populated, as some of the mutations may depend on it.
                    operationList.applyNonEndpointMutations(info, delegateProxy);
                    operationList.freeze();
                } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                    throw new IllegalClientArgumentException(e);
                }

                if (!callback.authorize()) {
                    return;
                }

                processor.validate(operationList, fullSize);

                callback.willProcessImage(processor, info);

                new ImageRepresentation(info, processor, operationList,
                        isBypassingCacheRead, isBypassingCache)
                        .write(outputStream);

                // Notify the health checker of a successful response.
                HealthChecker.addSourceUsage(source);
                return;
            } catch (SourceFormatException e) {
                LOGGER.debug("Format inferred by {} disagrees with the one " +
                                "supplied by {} ({}) for {}; trying again",
                        processorName, source.getClass().getSimpleName(),
                        format, identifier);
            }
        }
        if (config.getBoolean(Key.PROCESSOR_PURGE_INCOMPATIBLE_FROM_SOURCE_CACHE, false)) {
            TaskQueue.getInstance().submit(() -> {
                try {
                    cacheFacade.getSourceCacheFile(identifier).ifPresent(file -> {
                        try {
                            getLogger().debug("Deleting {}", file);
                            Files.delete(file);
                        } catch (IOException e) {
                            getLogger().warn("Failed to delete file from source cache: {}",
                                    e.getMessage());
                        }
                    });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        throw new SourceFormatException();
    }

}
