/**
 * <p>Provides asynchronous processing features:</p>
 *
 * <ul>
 *     <li>{@link edu.illinois.library.cantaloupe.async.ThreadPool} maintains a
 *     pool of threads that can run {@link java.lang.Runnable}s or
 *     {@link java.util.concurrent.Callable}s in parallel.</li>
 *     <li>{@link edu.illinois.library.cantaloupe.async.TaskQueue} can be used
 *     to submit {@link java.lang.Runnable}s to a threaded queue.</li>
 * </ul>
 */
package edu.illinois.library.cantaloupe.async;
