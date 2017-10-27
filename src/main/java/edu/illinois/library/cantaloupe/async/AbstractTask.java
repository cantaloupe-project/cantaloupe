package edu.illinois.library.cantaloupe.async;

import java.time.Instant;
import java.util.UUID;

/**
 * {@link Task} implementations can subclass this to get some free
 * functionality.
 */
public abstract class AbstractTask {

    private volatile Instant dateQueued;
    private volatile Instant dateStarted;
    private volatile Instant dateStopped;
    private volatile Exception failureException;
    private volatile Task.Status status = Task.Status.QUEUED;
    private final UUID uuid = UUID.randomUUID();

    public final synchronized Instant getInstantQueued() {
        return dateQueued;
    }

    public final synchronized Instant getInstantStarted() {
        return dateStarted;
    }

    public final synchronized Instant getInstantStopped() {
        return dateStopped;
    }

    public final synchronized Exception getFailureException() {
        return failureException;
    }

    public final synchronized Task.Status getStatus() {
        return status;
    }

    public final UUID getUUID() {
        return uuid;
    }

    public final synchronized void setInstantQueued(Instant queued) {
        this.dateQueued = queued;
    }

    public final synchronized void setInstantStarted(Instant started) {
        this.dateStarted = started;
    }

    public final synchronized void setInstantStopped(Instant stopped) {
        this.dateStopped = stopped;
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
