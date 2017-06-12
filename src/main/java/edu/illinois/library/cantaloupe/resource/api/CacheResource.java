package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;

public class CacheResource extends APIResource {

    @Delete
    public Representation doPurge() throws Exception {
        final Cache cache = CacheFactory.getDerivativeCache();
        if (cache != null) {
            cache.purge(getIdentifier());
        }
        return new EmptyRepresentation();
    }

}
