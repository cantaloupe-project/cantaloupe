package edu.illinois.library.cantaloupe.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

final class TaskRunner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(TaskRunner.class);

    private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

    /**
     * @return Unmodifiable list of all queued tasks, including the one
     *         currently running, if any. Completed tasks are not included.
     *         Tasks may change from moment to moment, but the returned list
     *         is fixed.
     */
    List<Object> queuedTasks() {
        Object[] tasks = new Object[] {};
        return List.of(queue.toArray(tasks));
    }

    @Override
    public void run() {
        while (true) {
            Object object;
            try {
                object = queue.take();

                LOGGER.trace("run(): running {}", object);
                if (object instanceof Runnable) {
                    ((Runnable) object).run();
                } else if (object instanceof Callable) {
                    ((Callable<?>) object).call();
                }
            } catch (Exception e) {
                LOGGER.error("run(): {}", e.getMessage(), e);
            }
        }
    }

    /**
     * @param callable Object to submit to the queue.
     * @throws IllegalStateException If the queue is full.
     */
    boolean submit(Callable<?> callable) {
        LOGGER.trace("submit(): {} (queue size: {})", callable, queue.size());
        return queue.add(callable);
    }

    /**
     * @param runnable Object to submit to the queue.
     * @throws IllegalStateException If the queue is full.
     */
    boolean submit(Runnable runnable) {
        LOGGER.trace("submit(): {} (queue size: {})", runnable, queue.size());
        return queue.add(runnable);
    }

    /**
     * Marks the task as queued and submits it to the queue.
     *
     * @param task Task to submit to the queue.
     * @throws IllegalStateException If the queue is full.
     */
    boolean submit(AuditableFutureTask<?> task) {
        // This must happen before the task is added to the queue; otherwise
        // the runner thread may start it first, and its RUNNING status would
        // then be overwritten with QUEUED.
        task.setStatus(TaskStatus.QUEUED);
        task.setInstantQueued(Instant.now());
        return submit((Runnable) task);
    }

}
