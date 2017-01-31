package edu.illinois.library.cantaloupe;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public void submit(Runnable task) {
        pool.submit(task);
    }

}
