package edu.illinois.library.cantaloupe.async;

import java.util.List;

public final class TaskQueue {

    private static volatile TaskQueue instance;

    private final TaskRunner runner;

    /**
     * For testing only.
     */
    static synchronized void clearInstance() {
        instance = null;
    }

    /**
     * @return Singleton instance.
     */
    public static TaskQueue getInstance() {
        TaskQueue queue = instance;
        if (queue == null) {
            synchronized (TaskQueue.class) {
                queue = instance;
                if (queue == null) {
                    instance = new TaskQueue();
                    queue = instance;
                }
            }
        }
        return queue;
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
    public List<Task> queuedTasks() {
        return runner.queuedTasks();
    }

    public void submit(Task task) {
        runner.submit(task);
    }

}
