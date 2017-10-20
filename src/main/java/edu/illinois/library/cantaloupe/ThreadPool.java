package edu.illinois.library.cantaloupe;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Global application thread pool Singleton.
 */
public final class ThreadPool {

    private static ThreadPool instance;

    private boolean isShutdown = false;
    private ExecutorService pool = Executors.newCachedThreadPool();

    /**
     * @return Shared ThreadPool instance.
     */
    public static synchronized ThreadPool getInstance() {
        if (instance == null || instance.isShutdown()) {
            instance = new ThreadPool();
        }
        return instance;
    }

    private ThreadPool() {
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public void shutdown() {
        pool.shutdown();
        isShutdown = true;
    }

    public Future<?> submit(Callable<?> task) {
        return pool.submit(task);
    }

    public Future<?> submit(Runnable task) {
        return pool.submit(task);
    }

}
