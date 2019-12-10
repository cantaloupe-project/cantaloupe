package edu.illinois.library.cantaloupe.resource.generic;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import org.slf4j.Logger;

import java.io.IOException;

abstract class AbstractRequestHandler {

    DelegateProxy delegateProxy;
    boolean isBypassingCache;
    boolean isBypassingCacheRead;
    RequestContext requestContext;

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
                info = new CacheFacade().getOrReadInfo(identifier, proc).orElseThrow();
            } else {
                info = proc.readInfo();
                DerivativeCache cache = CacheFactory.getDerivativeCache().orElse(null);
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

    boolean isResolvingFirst() {
        return Configuration.getInstance().
                getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true);
    }

}
