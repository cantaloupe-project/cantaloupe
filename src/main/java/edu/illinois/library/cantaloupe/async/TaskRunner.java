package edu.illinois.library.cantaloupe.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

final class TaskRunner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(TaskRunner.class);

    private final BlockingQueue<Object> queue =
            new LinkedBlockingQueue<>();

    /**
     * @return Unmodifiable list of all queued tasks, including the one
     *         currently running, if any. Completed tasks are not included.
     *         Tasks may change from moment to moment, but the returned list
     *         is fixed.
     */
    List<Object> queuedTasks() {
        Object[] tasks = new Object[] {};
        return Collections.unmodifiableList(Arrays.asList(queue.toArray(tasks)));
    }

    @Override
    public void run() {
        while (true) {
            Object object;
            try {
                // This will block while the queue is empty.
                LOGGER.debug("run(): waiting for a task...");
                object = queue.take();

                LOGGER.debug("run(): running {}", object);
                if (object instanceof Runnable) {
                    ((Runnable) object).run();
                } else if (object instanceof Callable) {
                    ((Callable<?>) object).call();
                }
            } catch (Exception e) {
                LOGGER.error("run(): {}", e.getMessage());
            }
        }
    }

    /**
     * @param callable Object to submit to the queue.
     * @throws IllegalStateException If the queue is full.
     */
    boolean submit(Callable<?> callable) {
        LOGGER.debug("submit(): {} (queue size: {})", callable, queue.size());
        return queue.add(callable);
    }

    /**
     * @param runnable Object to submit to the queue.
     * @throws IllegalStateException If the queue is full.
     */
    boolean submit(Runnable runnable) {
        LOGGER.debug("submit(): {} (queue size: {})", runnable, queue.size());
        final boolean result = queue.add(runnable);

        if (runnable instanceof AuditableFutureTask) {
            AuditableFutureTask<?> aTask = (AuditableFutureTask<?>) runnable;
            aTask.setStatus(TaskStatus.QUEUED);
            aTask.setInstantQueued(Instant.now());
        }
        return result;
    }

}
