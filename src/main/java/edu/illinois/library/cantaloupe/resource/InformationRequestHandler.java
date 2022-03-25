package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorConnector;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.source.StatResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * <p>High-level information request handler. Use the return value of {@link
 * #builder()} to create new instances.</p>
 *
 * <p>This class provides a simple interface that endpoints can use to retrieve
 * {@link Info image information}. Simplicity is achieved by abstracting away
 * as much of the tediousness of information request handling (caching,
 * connecting {@link Source sources} to {@link Processor processors}, etc.) as
 * possible. There is also no coupling to any particular protocol.</p>
 *
 * @author Alex Dolski UIUC
 * @since 5.0
 */
public class InformationRequestHandler extends AbstractRequestHandler
        implements AutoCloseable {

    /**
     * Builds {@link InformationRequestHandler} instances.
     */
    public static final class Builder {

        private final InformationRequestHandler handler;

        private Builder(InformationRequestHandler handler) {
            this.handler = handler;
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
        public Builder withCallback(InformationRequestHandler.Callback callback) {
            handler.callback = callback;
            return this;
        }

        /**
         * @param delegateProxy Delegate proxy. If set to a non-{@code null}
         *                      value, a {@link
         *                      #withRequestContext(RequestContext) request
         *                      context must also be set}.
         */
        public Builder withDelegateProxy(DelegateProxy delegateProxy) {
            handler.delegateProxy = delegateProxy;
            return this;
        }

        public Builder withIdentifier(Identifier identifier) {
            handler.identifier = identifier;
            return this;
        }

        /**
         * @param requestContext Request context. If set to a non-{@code null}
         *                       value, a {@link
         *                       #withDelegateProxy(DelegateProxy) delegate
         *                       proxy must also be set}.
         */
        public Builder withRequestContext(RequestContext requestContext) {
            handler.requestContext = requestContext;
            return this;
        }

        /**
         * @return New instance.
         * @throws IllegalArgumentException if any of the required builder
         *         methods have not been called.
         */
        public InformationRequestHandler build() {
            if (handler.identifier == null) {
                throw new IllegalArgumentException("Identifier cannot be null.");
            } else if (handler.delegateProxy != null &&
                    handler.requestContext == null) {
                throw new IllegalArgumentException("If a delegate proxy is " +
                        "set, a request context must also be set.");
            }
            if (handler.requestContext == null) {
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
         * <p>Performs authorization using an {@link
         * edu.illinois.library.cantaloupe.auth.Authorizer}.</p>
         *
         * <p>Any exceptions will bubble up through {@link #handle()}. This
         * feature can be used to support e.g. a {@link ResourceException} with
         * a custom status depending on the authorization result.</p>
         *
         * <p>This is the first callback to get called.</p>
         *
         * @return Authorization result.
         */
        boolean authorize() throws Exception;

        /**
         * Called immediately after a source image has been accessed.
         *
         * @param result Information about the source image.
         */
        void sourceAccessed(StatResult result);

        /**
         * Called when a list of available output formats has been obtained
         * from a {@link Processor}.
         */
        void knowAvailableOutputFormats(Set<Format> availableOutputFormats);

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InformationRequestHandler.class);

    // No-op callback to avoid having to check for one.
    private InformationRequestHandler.Callback callback = new Callback() {
        @Override
        public boolean authorize() {
            return true;
        }
        @Override
        public void sourceAccessed(StatResult sourceAvailable) {
        }
        @Override
        public void knowAvailableOutputFormats(Set<Format> availableOutputFormats) {
        }
    };
    private DelegateProxy delegateProxy;
    private Identifier identifier;
    private RequestContext requestContext;
    private Future<Path> tempFileFuture;

    public static Builder builder() {
        return new Builder(new InformationRequestHandler());
    }

    protected InformationRequestHandler() {}

    /**
     * Closes the instance.
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
     * Handles an information request.
     */
    public Info handle() throws Exception {
        if (!callback.authorize()) {
            return null;
        }

        final Configuration config    = Configuration.getInstance();
        final CacheFacade cacheFacade = new CacheFacade();

        // If we are using a cache, and don't need to resolve first, and the
        // cache contains an info matching the request, skip all the setup and
        // just return the cached info.
        if (!isBypassingCache && !isBypassingCacheRead &&
                !isResolvingFirst()) {
            try {
                Optional<Info> optInfo = cacheFacade.getInfo(identifier);
                if (optInfo.isPresent()) {
                    final Info info = optInfo.get();
                    // The source format will be null or UNKNOWN if the info was
                    // serialized in version < 3.4.
                    final Format format = info.getSourceFormat();
                    if (format != null && !Format.UNKNOWN.equals(format)) {
                        setRequestContextKeys(info);
                        return info;
                    }
                }
            } catch (IOException e) {
                // Don't rethrow -- it's still possible to service the request.
                LOGGER.error(e.getMessage());
            }
        }

        final Source source = new SourceFactory().newSource(
                identifier, delegateProxy);

        // If we are resolving first, or if the source image is not present in
        // the source cache (if enabled), check access to it in preparation for
        // retrieval.
        final Optional<Path> optSrcImage = cacheFacade.getSourceCacheFile(identifier);
        if (optSrcImage.isEmpty() || isResolvingFirst()) {
            try {
                StatResult result = source.stat();
                callback.sourceAccessed(result);
            } catch (NoSuchFileException e) { // this needs to be rethrown!
                if (config.getBoolean(Key.CACHE_SERVER_PURGE_MISSING, false)) {
                    // If the image was not found, purge it from the cache.
                    cacheFacade.purgeAsync(identifier);
                }
                throw e;
            }
        }

        // Get the format of the source image.
        // If we are not resolving first, and there is a hit in the source
        // cache, read the format from the source-cached-file, as we will
        // expect source cache access to be more efficient.
        // Otherwise, read it from the source.
        Iterator<Format> formatIterator = Collections.emptyIterator();
        if (!isResolvingFirst() && optSrcImage.isPresent()) {
            List<MediaType> mediaTypes = MediaType.detectMediaTypes(optSrcImage.get());
            if (!mediaTypes.isEmpty()) {
                formatIterator = mediaTypes
                        .stream()
                        .map(MediaType::toFormat)
                        .iterator();
            }
        } else {
            formatIterator = source.getFormatIterator();
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
                callback.knowAvailableOutputFormats(
                        processor.getAvailableOutputFormats());
                Info info = getOrReadInfo(identifier, processor);
                setRequestContextKeys(info);
                return info;
            } catch (SourceFormatException e) {
                LOGGER.debug("Format inferred by {} disagrees with the one " +
                                "supplied by {} ({}) for {}; trying again",
                        processorName, source.getClass().getSimpleName(),
                        format, identifier);
            }
        }
        throw new SourceFormatException();
    }

    private void setRequestContextKeys(Info info) {
        requestContext.setFullSize(info.getSize());
        requestContext.setPageCount(info.getNumPages());
        requestContext.setMetadata(info.getMetadata());
    }

}
