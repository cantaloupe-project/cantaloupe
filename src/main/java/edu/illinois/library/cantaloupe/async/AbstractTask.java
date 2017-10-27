package edu.illinois.library.cantaloupe.async;

import java.util.UUID;

/**
 * {@link Task} implementations can subclass this to get some free
 * functionality.
 */
public abstract class AbstractTask {

    private volatile Exception failureException;
    private volatile Task.Status status = Task.Status.QUEUED;
    private final UUID uuid = UUID.randomUUID();

    public final synchronized Exception getFailureException() {
        return failureException;
    }

    public final synchronized Task.Status getStatus() {
        return status;
    }

    public final UUID getUUID() {
        return uuid;
    }

    public final synchronized void setFailureException(Exception e) {
        this.failureException = e;
    }

    public final synchronized void setStatus(Task.Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return getUUID().toString();
    }

}
