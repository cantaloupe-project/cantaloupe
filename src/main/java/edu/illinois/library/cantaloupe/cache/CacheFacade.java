package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simplified interface to the caching architecture.
 */
public final class CacheFacade {

    /**
     * See {@link Cache#cleanUp}.
     */
    public void cleanUp() throws CacheException {
        // Clean up the derivative cache.
        DerivativeCache derivativeCache = getDerivativeCache();
        if (derivativeCache != null) {
            derivativeCache.cleanUp();
        }

        // Clean up the source cache.
        SourceCache sourceCache = getSourceCache();
        if (sourceCache != null) {
            sourceCache.cleanUp();
        }
    }

    /**
     * See {@link CacheFactory#getDerivativeCache}.
     */
    public DerivativeCache getDerivativeCache() {
        return CacheFactory.getDerivativeCache();
    }

    /**
     * See {@link InfoService#getOrReadInfo(Identifier, Processor)}.
     */
    public Info getOrReadInfo(Identifier identifier, Processor processor)
            throws CacheException, ProcessorException {
        return InfoService.getInstance().getOrReadInfo(identifier, processor);
    }

    /**
     * See {@link CacheFactory#getSourceCache}.
     */
    public SourceCache getSourceCache() {
        return CacheFactory.getSourceCache();
    }

    public boolean isDerivativeCacheAvailable() {
        return getDerivativeCache() != null;
    }

    public boolean isSourceCacheAvailable() {
        return getSourceCache() != null;
    }

    /**
     * See {@link DerivativeCache#newDerivativeImageInputStream(OperationList)}.
     */
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws CacheException {
        DerivativeCache derivativeCache = getDerivativeCache();
        if (derivativeCache != null) {
            return derivativeCache.newDerivativeImageInputStream(opList);
        }
        return null;
    }

    /**
     * See {@link DerivativeCache#newDerivativeImageOutputStream(OperationList)}.
     */
    public OutputStream newDerivativeImageOutputStream(OperationList opList)
            throws CacheException {
        DerivativeCache derivativeCache = getDerivativeCache();
        if (derivativeCache != null) {
            return derivativeCache.newDerivativeImageOutputStream(opList);
        }
        return null;
    }

    /**
     * See {@link Cache#purge}.
     */
    public void purge() throws CacheException {
        // Purge the info service.
        InfoService.getInstance().purge();

        // Purge the derivative cache.
        DerivativeCache derivativeCache = getDerivativeCache();
        if (derivativeCache != null) {
            derivativeCache.purge();
        }

        // Purge the source cache.
        SourceCache sourceCache = getSourceCache();
        if (sourceCache != null) {
            sourceCache.purge();
        }
    }

    /**
     * See {@link Cache#purge(Identifier)}.
     */
    public void purge(Identifier identifier) throws CacheException {
        // Purge it from the info service.
        InfoService.getInstance().purge(identifier);

        // Purge it from the derivative cache.
        DerivativeCache derivativeCache = getDerivativeCache();
        if (derivativeCache != null) {
            derivativeCache.purge(identifier);
        }

        // Purge it from the source cache.
        SourceCache sourceCache = getSourceCache();
        if (sourceCache != null) {
            sourceCache.purge(identifier);
        }
    }

    /**
     * Invokes {@link #purge(Identifier)} asynchronously.
     */
    public void purgeAsync(Identifier identifier) throws CacheException {
        TaskQueue.getInstance().submit(() -> {
            purge(identifier);
            return null;
        });
    }

    /**
     * See {@link DerivativeCache#purge(OperationList)}.
     */
    public void purge(OperationList opList) throws CacheException {
        DerivativeCache derivativeCache = getDerivativeCache();
        if (derivativeCache != null) {
            derivativeCache.purge(opList);
        }
    }

    /**
     * See {@link Cache#purgeExpired()}.
     */
    public void purgeExpired() throws CacheException {
        // Purge the derivative cache.
        DerivativeCache derivativeCache = getDerivativeCache();
        if (derivativeCache != null) {
            derivativeCache.purgeExpired();
        }

        // Purge the source cache.
        SourceCache sourceCache = getSourceCache();
        if (sourceCache != null) {
            sourceCache.purgeExpired();
        }
    }

}
