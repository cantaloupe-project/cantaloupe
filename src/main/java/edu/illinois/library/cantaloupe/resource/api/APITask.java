package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.illinois.library.cantaloupe.async.AbstractTask;

/**
 * Abstract RPC command superclass that adds some functionality beyond a
 * basic {@link edu.illinois.library.cantaloupe.async.Task}.
 * {@link com.fasterxml.jackson.databind.ObjectMapper} will deserialize JSON
 * objects into subclass instances based on their <code>verb</code> property.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "verb")
@JsonSubTypes({
        @JsonSubTypes.Type(
                name = "PurgeCache",
                value = PurgeCacheTask.class),
        @JsonSubTypes.Type(
                name = "PurgeDelegateMethodInvocationCache",
                value = PurgeDelegateMethodInvocationCacheTask.class),
        @JsonSubTypes.Type(
                name = "PurgeInvalidFromCache",
                value = PurgeInvalidFromCacheTask.class),
        @JsonSubTypes.Type(
                name = "PurgeItemFromCache",
                value = PurgeItemFromCacheTask.class)
})
abstract class APITask extends AbstractTask {

    private String verb;

    String getVerb() {
        return verb;
    }

    void setVerb(String verb) {
        this.verb = verb;
    }

    @JsonIgnore
    @Override
    public String toString() {
        return getVerb() + " " + getUUID();
    }

}
