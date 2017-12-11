package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.cache.CacheFacade;
import org.restlet.data.MediaType;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;

/**
 * @since 3.3
 * @deprecated To be replaced by {@link PurgeItemFromCacheCommand} in conjunction
 *             with {@link TasksResource} in version 4.0.
 */
@Deprecated
public class CacheResource extends AbstractAPIResource {

    @Delete
    public Representation doPurge() throws Exception {
        new CacheFacade().purge(getIdentifier());
        Representation rep = new EmptyRepresentation();
        rep.setMediaType(MediaType.TEXT_PLAIN);
        return rep;
    }

}
