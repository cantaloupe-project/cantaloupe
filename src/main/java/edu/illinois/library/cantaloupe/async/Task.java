package edu.illinois.library.cantaloupe.async;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.time.Instant;
import java.util.UUID;

/**
 * Queueable task.
 */
public interface Task {

    enum Status {
        QUEUED, RUNNING, SUCCEEDED, FAILED
    }

    @JsonGetter("exception")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Exception getFailureException();

    @JsonProperty("queued")
    Instant getInstantQueued();

    @JsonProperty("started")
    Instant getInstantStarted();

    @JsonProperty("stopped")
    Instant getInstantStopped();

    /**
     * Instances don't need to manage this; it will be set for them.
     */
    Status getStatus();

    UUID getUUID();

    void run() throws Exception;

    @JsonSetter("exception")
    void setFailureException(Exception e);

    void setInstantQueued(Instant queued);

    void setInstantStarted(Instant started);

    void setInstantStopped(Instant stopped);

    /**
     * Instances don't need to manage this; it will be set for them.
     */
    void setStatus(Task.Status status);

}
