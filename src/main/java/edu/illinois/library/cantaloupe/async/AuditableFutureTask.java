package edu.illinois.library.cantaloupe.async;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * {@link FutureTask} that provides additional functionality.
 */
public class AuditableFutureTask<T> extends FutureTask<T> {

    private volatile Instant dateQueued;
    private volatile Instant dateStarted;
    private volatile Instant dateStopped;
    private volatile Throwable failureException;
    private volatile TaskStatus status = TaskStatus.NEW;
    private UUID uuid = UUID.randomUUID();

    public AuditableFutureTask(Callable<T> callable) {
        super(callable);
    }

    public AuditableFutureTask(Runnable runnable, T type) {
        super(runnable, type);
    }

    @Override
    protected void done() {
        try {
            if (!isCancelled()) {
                get();
            }
            setStatus(TaskStatus.SUCCEEDED);
        } catch (ExecutionException e) {
            setStatus(TaskStatus.FAILED);
            setException(e);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        } finally {
            setInstantStopped(Instant.now());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final Throwable getException() {
        return failureException;
    }

    @JsonProperty("queued_at")
    public final Instant getInstantQueued() {
        return dateQueued;
    }

    @JsonProperty("started_at")
    public final Instant getInstantStarted() {
        return dateStarted;
    }

    @JsonProperty("stopped_at")
    public final Instant getInstantStopped() {
        return dateStopped;
    }

    public final TaskStatus getStatus() {
        return status;
    }

    public final UUID getUUID() {
        return uuid;
    }

    @JsonIgnore
    @Override
    public void run() {
        setInstantStarted(Instant.now());
        setStatus(TaskStatus.RUNNING);
        super.run();
    }

    @JsonIgnore
    @Override
    protected void setException(Throwable t) {
        super.setException(t);
        this.failureException = t;
    }

    @JsonIgnore
    void setInstantQueued(Instant queued) {
        this.dateQueued = queued;
    }

    @JsonIgnore
    private void setInstantStarted(Instant started) {
        this.dateStarted = started;
    }

    @JsonIgnore
    private void setInstantStopped(Instant stopped) {
        this.dateStopped = stopped;
    }

    @JsonIgnore
    void setStatus(TaskStatus status) {
        this.status = status;
    }

    @JsonIgnore
    private void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return getUUID().toString();
    }

}
