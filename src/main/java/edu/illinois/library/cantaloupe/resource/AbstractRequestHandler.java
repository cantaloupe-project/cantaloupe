package edu.illinois.library.cantaloupe.resource;

import java.io.IOException;

import org.slf4j.Logger;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.processor.Processor;

abstract class AbstractRequestHandler {

    DelegateProxy delegateProxy;
    boolean isBypassingCache;
    boolean isBypassingCacheRead;
    RequestContext requestContext;
    protected Configuration configuration;

    abstract Logger getLogger();

    /**
     * <p>Returns the info for the source image corresponding to the
     * given identifier as efficiently as possible.</p>
     *
     * @param identifier Image identifier.
     * @param proc       Processor from which to read the info if it can't be
     *                   retrieved from a cache.
     * @return           Instance for the image with the given identifier.
     */
    Info getOrReadInfo(final Identifier identifier,
                       final Processor proc) throws IOException {
        Info info;
        if (!isBypassingCache) {
            if (!isBypassingCacheRead) {
                info = new CacheFacade(configuration).getOrReadInfo(identifier, proc).orElseThrow();
            } else {
                info = proc.readInfo();
                DerivativeCache cache = new CacheFactory(configuration).getDerivativeCache().orElse(null);
                if (cache != null) {
                    cache.put(identifier, info);
                }
            }
            info.setIdentifier(identifier);
        } else {
            getLogger().debug("getOrReadInfo(): bypassing the cache, as requested");
            info = proc.readInfo();
            info.setIdentifier(identifier);
        }
        return info;
    }

    /*
     * If true, we must confirm the source image exists before a cached copy
     * is returned. If false, the cached copy will be returned without checking.
     * Resolving first is safer but slower.
     */
    boolean verifyExistenceBeforeReturningCachedValue() {
        return configuration.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true);
    }

}
