package edu.illinois.library.cantaloupe.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Runs submitted {@link Runnable}s in a queue.
 */
class QueuedRunnableRunner extends Thread {

    private static final Logger logger = LoggerFactory.
            getLogger(QueuedRunnableRunner.class);

    private static final String THREAD_NAME = "upload-worker";

    private ArrayBlockingQueue<Runnable> queue;

    QueuedRunnableRunner(int maxQueueSize) {
        super(THREAD_NAME + "-" + new Random().nextInt(1000000));
        queue = new ArrayBlockingQueue<>(maxQueueSize);
    }

    @Override
    public void run() {
        try {
            while (true) {
                logger.debug("Taking an item from the queue (size: {})",
                        queue.size());
                // This will block while the queue is empty.
                Runnable runnable = queue.take();
                runnable.run();
            }
        } catch (InterruptedException e) {
            logger.info(e.getMessage());
        }
    }

    /**
     * @param runnable Runnable to submit to the queue.
     * @throws IllegalStateException If the queue is full.
     */
    public void submit(Runnable runnable) {
        logger.debug("Runnable submitted to the queue (size: {})",
                queue.size());
        queue.add(runnable);
    }

}