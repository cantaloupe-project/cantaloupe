package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;

public class CacheResource extends APIResource {

    @Delete
    public Representation doPurge() throws Exception {
        new CacheFacade().purge(getIdentifier());
        return new EmptyRepresentation();
    }

}
