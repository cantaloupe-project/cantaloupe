package edu.illinois.library.cantaloupe.async;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Queue of serial tasks, backed internally by {@link ThreadPool}. Should be
 * preferred over {@link ThreadPool} for tasks that are not time-sensitive.
 */
public final class TaskQueue {

    private static TaskQueue instance = new TaskQueue();

    private final TaskRunner runner;

    /**
     * For testing only.
     */
    static synchronized void clearInstance() {
        instance = new TaskQueue();
    }

    /**
     * @return Singleton instance.
     */
    public static synchronized TaskQueue getInstance() {
        return instance;
    }

    private TaskQueue() {
        runner = new TaskRunner();
        ThreadPool.getInstance().submit(runner);
    }

    /**
     * @return Unmodifiable list of all queued tasks, including the one
     *         currently running, if any. Completed tasks are not included.
     *         Tasks may change from moment to moment, but the returned list
     *         is fixed and immutable.
     */
    List<Object> queuedTasks() {
        return runner.queuedTasks();
    }

    /**
     * Adds a task to the queue.
     */
    public void submit(Callable<?> callable) {
        runner.submit(callable);
    }

    /**
     * Adds a task to the queue.
     */
    public void submit(Runnable runnable) {
        runner.submit(runnable);
    }

}
