package edu.illinois.library.cantaloupe.async;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

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

    /**
     * Instances don't need to manage this; it will be set for them.
     */
    Status getStatus();

    UUID getUUID();

    void run() throws Exception;

    @JsonSetter("exception")
    void setFailureException(Exception e);

    /**
     * Instances don't need to manage this; it will be set for them.
     */
    void setStatus(Task.Status status);

}
