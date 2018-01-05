package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
     * @see CacheFactory#getDerivativeCache
     */
    public DerivativeCache getDerivativeCache() {
        return CacheFactory.getDerivativeCache();
    }

    /**
     * Retrieves an info corresponding to the given identifier from the info
     * or derivative cache.
     *
     * @see #getOrReadInfo(Identifier, Processor)
     */
    public Info getInfo(Identifier identifier) throws IOException {
        return InfoService.getInstance().getInfo(identifier);
    }

    /**
     * Retrieves an info corresponding to the given identifier from the info
     * or derivative cache, falling back to reading it from a processor, if
     * necessary.
     *
     * @see #getInfo(Identifier)
     */
    public Info getOrReadInfo(Identifier identifier, Processor processor)
            throws IOException {
        return InfoService.getInstance().getOrReadInfo(identifier, processor);
    }

    /**
     * @see CacheFactory#getSourceCache
     */
    public SourceCache getSourceCache() {
        return CacheFactory.getSourceCache();
    }

    public boolean isDerivativeCacheAvailable() {
        return getDerivativeCache() != null;
    }

    public boolean isInfoCacheAvailable() {
        return InfoService.getInstance().isObjectCacheEnabled();
    }

    public boolean isSourceCacheAvailable() {
        return getSourceCache() != null;
    }

    /**
     * @see DerivativeCache#newDerivativeImageInputStream(OperationList)
     */
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws IOException {
        DerivativeCache derivativeCache = getDerivativeCache();
        if (derivativeCache != null) {
            return derivativeCache.newDerivativeImageInputStream(opList);
        }
        return null;
    }

    /**
     * @see DerivativeCache#newDerivativeImageOutputStream(OperationList)
     */
    public OutputStream newDerivativeImageOutputStream(OperationList opList)
            throws IOException {
        DerivativeCache derivativeCache = getDerivativeCache();
        if (derivativeCache != null) {
            return derivativeCache.newDerivativeImageOutputStream(opList);
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
     * @see Cache#purge(Identifier)
     */
    public void purge(Identifier identifier) throws IOException {
        // Purge it from the info service.
        InfoService.getInstance().purgeObjectCache(identifier);

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
        DerivativeCache derivativeCache = getDerivativeCache();
        if (derivativeCache != null) {
            derivativeCache.purge(opList);
        }
    }

    /**
     * @see Cache#purgeExpired
     */
    public void purgeExpired() throws IOException {
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
