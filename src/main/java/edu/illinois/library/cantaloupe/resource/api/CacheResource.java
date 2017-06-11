package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import org.restlet.data.Reference;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;

public class CacheResource extends APIResource {

    /**
     * @throws Exception
     */
    @Delete
    public Representation doPurge() throws Exception {
        final Cache cache = CacheFactory.getDerivativeCache();
        if (cache != null) {
            final String idStr = (String) this.getRequest().getAttributes().
                    get("identifier");
            final Identifier identifier =
                    new Identifier(decodeSlashes(Reference.decode(idStr)));

            cache.purge(identifier);
        }
        return new EmptyRepresentation();
    }

}
