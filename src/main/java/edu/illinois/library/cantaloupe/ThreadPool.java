package edu.illinois.library.cantaloupe;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Global application thread pool Singleton.
 */
public final class ThreadPool {

    private static ThreadPool instance;
    private ExecutorService pool = Executors.newCachedThreadPool();

    /**
     * @return Shared ThreadPool instance.
     */
    public static synchronized ThreadPool getInstance() {
        if (instance == null) {
            instance = new ThreadPool();
        }
        return instance;
    }

    private ThreadPool() {
    }

    public void shutdown() {
        pool.shutdown();
    }

    public Future<?> submit(Runnable task) {
        return pool.submit(task);
    }

}
