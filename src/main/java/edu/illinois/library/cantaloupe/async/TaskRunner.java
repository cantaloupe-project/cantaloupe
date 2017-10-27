package edu.illinois.library.cantaloupe.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

final class TaskRunner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(TaskRunner.class);

    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

    /**
     * @return Unmodifiable list of all queued tasks, including the one
     *         currently running, if any. Completed tasks are not included.
     *         Tasks may change from moment to moment, but the returned list
     *         is fixed.
     */
    List<Task> queuedTasks() {
        Task[] tasks = new Task[] {};
        return Collections.unmodifiableList(Arrays.asList(queue.toArray(tasks)));
    }

    @Override
    public void run() {
        while (true) {
            Task task = null;
            try {
                // This will block while the queue is empty.
                LOGGER.debug("run(): blocking for a task...");
                task = queue.take();

                LOGGER.debug("run(): running {}", task);
                task.setStatus(Task.Status.RUNNING);
                task.run();
                task.setStatus(Task.Status.SUCCEEDED);
            } catch (Exception e) {
                if (task != null) {
                    task.setStatus(Task.Status.FAILED);
                    task.setFailureException(e);
                }
            }
        }
    }

    /**
     * @param task Task to submit to the queue.
     * @throws IllegalStateException If the queue is full.
     */
    void submit(Task task) {
        LOGGER.debug("submit(): {} (queue size: {})", task, queue.size());
        queue.add(task);
        task.setStatus(Task.Status.QUEUED);
    }

}
