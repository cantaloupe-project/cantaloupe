package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Simplified interface to the caching architecture.
 */
public final class CacheFacade {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(CacheFacade.class);

    /**
     * @see Cache#cleanUp
     */
    public void cleanUp() throws IOException {
        // Clean up the derivative cache.
        Optional<DerivativeCache> optDerivativeCache = getDerivativeCache();
        if (optDerivativeCache.isPresent()) {
            optDerivativeCache.get().cleanUp();
        }

        // Clean up the source cache.
        Optional<SourceCache> optSourceCache = getSourceCache();
        if (optSourceCache.isPresent()) {
            optSourceCache.get().cleanUp();
        }
    }

    /**
     * @see CacheFactory#getDerivativeCache
     */
    public Optional<DerivativeCache> getDerivativeCache() {
        DerivativeCache cache = CacheFactory.getDerivativeCache();
        if (cache != null) {
            return Optional.of(cache);
        }
        return Optional.empty();
    }

    /**
     * Retrieves an info corresponding to the given identifier from the info
     * or derivative cache.
     *
     * @see #getOrReadInfo(Identifier, Processor)
     */
    public Optional<Info> getInfo(Identifier identifier) throws IOException {
        return InfoService.getInstance().getInfo(identifier);
    }

    /**
     * Retrieves an info corresponding to the given identifier from the info
     * or derivative cache, falling back to reading it from a processor, if
     * necessary.
     *
     * @see #getInfo(Identifier)
     */
    public Optional<Info> getOrReadInfo(Identifier identifier,
                                        Processor processor) throws IOException {
        return InfoService.getInstance().getOrReadInfo(identifier, processor);
    }

    /**
     * @see CacheFactory#getSourceCache
     */
    public Optional<SourceCache> getSourceCache() {
        SourceCache srcCache = CacheFactory.getSourceCache();
        if (srcCache != null) {
            return Optional.of(srcCache);
        }
        return Optional.empty();
    }

    /**
     * @param identifier Image identifier.
     * @return           Path of a valid file corresponding to the given
     *                   identifier in the source cache, or empty if none
     *                   exists.
     * @see SourceCache#getSourceImageFile(Identifier)
     */
    public Optional<Path> getSourceCacheFile(Identifier identifier)
            throws IOException {
        Optional<SourceCache> optSourceCache = getSourceCache();
        if (optSourceCache.isPresent()) {
            return optSourceCache.get().getSourceImageFile(identifier);
        }
        return Optional.empty();
    }

    public boolean isDerivativeCacheAvailable() {
        return getDerivativeCache().isPresent();
    }

    public boolean isInfoCacheAvailable() {
        return InfoService.getInstance().isObjectCacheEnabled();
    }

    /**
     * @see DerivativeCache#newDerivativeImageInputStream(OperationList)
     */
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws IOException {
        Optional<DerivativeCache> optCache = getDerivativeCache();
        if (optCache.isPresent()) {
            return optCache.get().newDerivativeImageInputStream(opList);
        }
        return null;
    }

    /**
     * @see DerivativeCache#newDerivativeImageOutputStream(OperationList)
     */
    public OutputStream newDerivativeImageOutputStream(OperationList opList)
            throws IOException {
        Optional<DerivativeCache> optCache = getDerivativeCache();
        if (optCache.isPresent()) {
            return optCache.get().newDerivativeImageOutputStream(opList);
        }
        return null;
    }

    /**
     * @see Cache#purge
     */
    public void purge() throws IOException {
        // Purge the info service.
        InfoService.getInstance().purgeObjectCache();

        // Purge the derivative cache.
        Optional<DerivativeCache> optDerivativeCache = getDerivativeCache();
        if (optDerivativeCache.isPresent()) {
            optDerivativeCache.get().purge();
        }

        // Purge the source cache.
        Optional<SourceCache> optSourceCache = getSourceCache();
        if (optSourceCache.isPresent()) {
            optSourceCache.get().purge();
        }
    }

    /**
     * @see Cache#purge(Identifier)
     */
    public void purge(Identifier identifier) throws IOException {
        // Purge it from the info service.
        InfoService.getInstance().purgeObjectCache(identifier);

        // Purge it from the derivative cache.
        Optional<DerivativeCache> optDerivativeCache = getDerivativeCache();
        if (optDerivativeCache.isPresent()) {
            optDerivativeCache.get().purge(identifier);
        }

        // Purge it from the source cache.
        Optional<SourceCache> optSourceCache = getSourceCache();
        if (optSourceCache.isPresent()) {
            optSourceCache.get().purge(identifier);
        }
    }

    /**
     * Invokes {@link #purge(Identifier)} asynchronously.
     */
    public void purgeAsync(Identifier identifier) {
        TaskQueue.getInstance().submit(() -> {
            try {
                purge(identifier);
            } catch (IOException e) {
                LOGGER.error("purgeAsync(): {}", e.getMessage());
            }
            return null;
        });
    }

    /**
     * @see DerivativeCache#purge(OperationList)
     */
    public void purge(OperationList opList) throws IOException {
        Optional<DerivativeCache> optCache = getDerivativeCache();
        if (optCache.isPresent()) {
            optCache.get().purge(opList);
        }
    }

    /**
     * @see Cache#purgeInvalid
     */
    public void purgeExpired() throws IOException {
        // Purge the derivative cache.
        Optional<DerivativeCache> optDerivativeCache = getDerivativeCache();
        if (optDerivativeCache.isPresent()) {
            optDerivativeCache.get().purgeInvalid();
        }

        // Purge the source cache.
        Optional<SourceCache> optSourceCache = getSourceCache();
        if (optSourceCache.isPresent()) {
            optSourceCache.get().purgeInvalid();
        }
    }

}
