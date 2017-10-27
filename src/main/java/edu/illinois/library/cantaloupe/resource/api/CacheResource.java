package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;

/**
 * @since 3.3
 * @deprecated To be replaced by {@link PurgeItemFromCacheTask} in conjunction
 *             with {@link TasksResource} in version 4.0.
 */
@Deprecated
public class CacheResource extends AbstractAPIResource {

    @Delete
    public Representation doPurge() throws Exception {
        new CacheFacade().purge(getIdentifier());
        return new EmptyRepresentation();
    }

}
