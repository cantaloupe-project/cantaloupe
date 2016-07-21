package edu.illinois.library.cantaloupe.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Simple class used for measuring time elapsed.</p>
 *
 * <p>Instances are thread-safe.</p>
 */
public class Stopwatch {

    private AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

    /**
     * @return Number of milliseconds since the stopwatch was started.
     */
    public long timeElapsed() {
        return System.currentTimeMillis() - startTime.get();
    }

}
