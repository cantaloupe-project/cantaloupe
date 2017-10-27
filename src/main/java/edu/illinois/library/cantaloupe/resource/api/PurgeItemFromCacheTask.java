package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import edu.illinois.library.cantaloupe.async.Task;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.image.Identifier;

final class PurgeItemFromCacheTask extends APITask implements Task {

    private Identifier identifier;

    /**
     * @return Image identifier.
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    @JsonGetter("identifier")
    public String getIdentifierAsString() {
        return getUUID().toString();
    }

    /**
     * @param identifier Image identifier.
     */
    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = new Identifier(identifier);
    }

    @Override
    public void run() throws Exception {
        new CacheFacade().purge(getIdentifier());
    }

}
