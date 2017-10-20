package edu.illinois.library.cantaloupe;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Global application thread pool Singleton.
 */
public final class ThreadPool {

    public enum Priority {
        LOW, NORMAL, HIGH
    }

    private static abstract class AbstractThreadFactory {

        private final AtomicInteger counter = new AtomicInteger();

        abstract String getThreadNamePrefix();

        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName(getThreadNamePrefix() + "-" +
                    counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    private static class LowPriorityThreadFactory
            extends AbstractThreadFactory implements ThreadFactory {
        @Override
        String getThreadNamePrefix() {
            return "cid-lp";
        }
    }

    private static class NormalPriorityThreadFactory
            extends AbstractThreadFactory implements ThreadFactory {
        @Override
        String getThreadNamePrefix() {
            return "cid-np";
        }
    }

    private static class HighPriorityThreadFactory
            extends AbstractThreadFactory implements ThreadFactory {
        @Override
        String getThreadNamePrefix() {
            return "cid-hp";
        }
    }

    private static ThreadPool instance;

    private boolean isShutdown = false;
    private final ExecutorService lowPriorityPool =
            Executors.newCachedThreadPool(new LowPriorityThreadFactory());
    private final ExecutorService normalPriorityPool =
            Executors.newCachedThreadPool(new NormalPriorityThreadFactory());
    private final ExecutorService highPriorityPool =
            Executors.newCachedThreadPool(new HighPriorityThreadFactory());

    /**
     * @return Shared {@link ThreadPool} instance.
     */
    public static synchronized ThreadPool getInstance() {
        if (instance == null || instance.isShutdown()) {
            instance = new ThreadPool();
        }
        return instance;
    }

    /**
     * For testing.
     */
    static synchronized void clearInstance() {
        instance.shutdown();
        instance = null;
    }

    private ThreadPool() {
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public void shutdown() {
        lowPriorityPool.shutdown();
        normalPriorityPool.shutdown();
        highPriorityPool.shutdown();
        isShutdown = true;
    }

    public Future<?> submit(Callable<?> task) {
        return submit(task, Priority.NORMAL);
    }

    public Future<?> submit(Callable<?> task, Priority priority) {
        switch (priority) {
            case LOW:
                return lowPriorityPool.submit(task);
            case HIGH:
                return highPriorityPool.submit(task);
            default:
                return normalPriorityPool.submit(task);
        }
    }

    public Future<?> submit(Runnable task) {
        return submit(task, Priority.NORMAL);
    }

    public Future<?> submit(Runnable task, Priority priority) {
        switch (priority) {
            case LOW:
                return lowPriorityPool.submit(task);
            case HIGH:
                return highPriorityPool.submit(task);
            default:
                return normalPriorityPool.submit(task);
        }
    }

}
