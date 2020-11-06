package edu.illinois.library.cantaloupe.resource;

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
import edu.illinois.library.cantaloupe.status.HealthChecker;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
     * Builds {@link ImageRequestHandler} instances.
     */
    public static final class Builder {

        private final ImageRequestHandler handler;

        private Builder(ImageRequestHandler handler) {
            this.handler = handler;
        }

        /**
         * Variant of {@link #withDelegateProxy(DelegateProxy, RequestContext)}
         * that does nothing if either argument is {@code null}.
         *
         * @param delegateProxy  Delegate proxy. If {@code null}, both it and
         *                       {@code requestContext} are set to {@code null}.
         * @param requestContext Request context. If {@code null}, both it and
         *                       {@code delegateProxy} are set to {@code null}.
         * @see #withDelegateProxy(DelegateProxy, RequestContext)
         */
        public Builder optionallyWithDelegateProxy(DelegateProxy delegateProxy,
                                                   RequestContext requestContext) {
            if (delegateProxy != null && requestContext != null) {
                handler.delegateProxy  = delegateProxy;
                handler.requestContext = requestContext;
            } else {
                handler.delegateProxy  = null;
                handler.requestContext = null;
            }
            return this;
        }

        /**
         * @param isBypassingCache Supply {@code true} to bypass cache reads
         *                         and writes.
         */
        public Builder withBypassingCache(boolean isBypassingCache) {
            handler.isBypassingCache = isBypassingCache;
            return this;
        }

        /**
         * @param isBypassingCacheRead Supply {@code true} to bypass cache
         *                             reads only.
         */
        public Builder withBypassingCacheRead(boolean isBypassingCacheRead) {
            handler.isBypassingCacheRead = isBypassingCacheRead;
            return this;
        }

        /**
         * @param callback Callback to receive events during request handling.
         */
        public Builder withCallback(Callback callback) {
            handler.callback = callback;
            return this;
        }

        /**
         * Variant of {@link #optionallyWithDelegateProxy(DelegateProxy,
         * RequestContext)} for which both arguments must be either {@code
         * null} or not-{@code null}.
         *
         * @see #optionallyWithDelegateProxy(DelegateProxy, RequestContext)
         */
        public Builder withDelegateProxy(DelegateProxy delegateProxy,
                                         RequestContext requestContext) {
            if (delegateProxy != null && requestContext == null) {
                throw new IllegalArgumentException("If a delegate proxy is " +
                        "set, a request context must also be set.");
            } else if (delegateProxy == null && requestContext != null) {
                throw new IllegalArgumentException("If a request context is " +
                        "set, a delegate proxy must also be set.");
            }
            handler.delegateProxy  = delegateProxy;
            handler.requestContext = requestContext;
            return this;
        }

        public Builder withOperationList(OperationList opList) {
            handler.operationList = opList;
            return this;
        }

        /**
         * @return New instance.
         * @throws NullPointerException if any of the required builder methods
         *                              have not been called.
         */
        public ImageRequestHandler build() {
            if (handler.operationList == null) {
                throw new NullPointerException("Operation list cannot be null.");
            }
            if (handler.requestContext == null) {
                // Set the requestContext to an unused object that will prevent
                // having to do null checks.
                handler.requestContext = new RequestContext();
            }
            return handler;
        }

    }

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
         * <p>Called when a hit is found in the derivative cache. In this case,
         * no further processing will be necessary and the streaming will begin
         * very soon after this method returns.</p>
         *
         * <p>If a hit is not found in the derivative cache, this method is not
         * called.</p>
         *
         * <p>This method tends to be called relatively early. No other
         * callback methods have been or will be called.</p>
         */
        void willStreamImageFromDerivativeCache() throws Exception;

        /**
         * Called when image information is available. Always called before
         * {@link #willProcessImage(Processor, Info)}, but not called if {@link
         * #willStreamImageFromDerivativeCache()} is called.
         *
         * @param info Efficiently obtained instance.
         */
        void infoAvailable(Info info) throws Exception;

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

    // No-op callback to avoid having to check for one.
    private Callback callback = new Callback() {
        @Override
        public boolean preAuthorize() {
            return true;
        }
        @Override
        public boolean authorize() {
            return true;
        }
        @Override
        public void willStreamImageFromDerivativeCache() {
        }
        @Override
        public void infoAvailable(Info info) {
        }
        @Override
        public void willProcessImage(Processor processor, Info info) {
        }
    };
    private OperationList operationList;
    private Future<Path> tempFileFuture;

    public static Builder builder() {
        return new Builder(new ImageRequestHandler());
    }

    protected ImageRequestHandler() {}

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
                source.checkAccess();
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
                    operationList.applyNonEndpointMutations(info, delegateProxy);
                    operationList.freeze();
                    requestContext.setOperationList(operationList, fullSize);
                    requestContext.setPageCount(info.getImages().size());
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
                HealthChecker.addSourceProcessorPair(
                        source, processor, operationList);
                return;
            } catch (SourceFormatException e) {
                LOGGER.debug("Format inferred by {} disagrees with the one " +
                                "supplied by {} ({}) for {}; trying again",
                        processorName, source.getClass().getSimpleName(),
                        format, identifier);
            }
        }
        throw new SourceFormatException();
    }

}
