package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import edu.illinois.library.cantaloupe.cache.CacheFacade;
import edu.illinois.library.cantaloupe.image.Identifier;

import java.util.concurrent.Callable;

final class PurgeItemFromCacheCommand<T> extends Command
        implements Callable<T> {

    private Identifier identifier;

    @Override
    public T call() throws Exception {
        new CacheFacade().purge(getIdentifier());
        return null;
    }

    /**
     * @return Image identifier.
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    @JsonGetter("identifier")
    public String getIdentifierAsString() {
        return getIdentifier().toString();
    }

    @Override
    String getVerb() {
        return "PurgeItemFromCache";
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

}
