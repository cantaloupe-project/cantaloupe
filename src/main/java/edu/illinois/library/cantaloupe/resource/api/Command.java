package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstract RPC command superclass.
 * {@link com.fasterxml.jackson.databind.ObjectMapper} will deserialize JSON
 * objects into subclass instances based on their {@literal verb} property.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "verb")
@JsonSubTypes({
        @JsonSubTypes.Type(
                name = "Sleep",
                value = SleepCommand.class),
        @JsonSubTypes.Type(
                name = "PurgeCache",
                value = PurgeCacheCommand.class),
        @JsonSubTypes.Type(
                name = "PurgeInfoCache",
                value = PurgeInfoCacheCommand.class),
        @JsonSubTypes.Type(
                name = "PurgeInfosFromCache",
                value = PurgeInfosFromCacheCommand.class),
        @JsonSubTypes.Type(
                name = "PurgeInvalidFromCache",
                value = PurgeInvalidFromCacheCommand.class),
        @JsonSubTypes.Type(
                name = "PurgeItemFromCache",
                value = PurgeItemFromCacheCommand.class)
})
abstract class Command {

    abstract String getVerb();

}
