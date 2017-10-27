/**
 * <p>Provides asynchronous processing features:</p>
 *
 * <ul>
 *     <li>{@link edu.illinois.library.cantaloupe.async.ThreadPool} maintains a
 *     pool of threads that can run {@link java.lang.Runnable}s or
 *     {@link java.util.concurrent.Callable}s in parallel.</li>
 *     <li>{@link edu.illinois.library.cantaloupe.async.TaskRunner} can be used
 *     to submit sequential {@link edu.illinois.library.cantaloupe.async.Task tasks}
 *     to a threaded queue.</li>
 * </ul>
 */
package edu.illinois.library.cantaloupe.async;
