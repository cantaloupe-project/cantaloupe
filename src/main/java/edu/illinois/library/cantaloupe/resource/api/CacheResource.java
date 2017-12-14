package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.RestletApplication;
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
        getLogger().warning("`DELETE " + RestletApplication.CACHE_PATH +
                "` is deprecated and will be removed in version 4. Please " +
                "upgrade to its successor. (See the user manual.)");

        new CacheFacade().purge(getIdentifier());
        Representation rep = new EmptyRepresentation();
        rep.setMediaType(MediaType.TEXT_PLAIN);
        return rep;
    }

}
