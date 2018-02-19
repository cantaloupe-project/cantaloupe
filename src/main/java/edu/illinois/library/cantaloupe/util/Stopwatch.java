package edu.illinois.library.cantaloupe.util;

/**
 * <p>Measures time elapsed. The timer starts at instantiation.</p>
 *
 * <p>Instances are thread-safe.</p>
 */
public final class Stopwatch {

    private final long startTime = System.currentTimeMillis();

    /**
     * @return Number of milliseconds since the stopwatch was started.
     */
    public long timeElapsed() {
        return System.currentTimeMillis() - startTime;
    }

    @Override
    public String toString() {
        return timeElapsed() + " msec";
    }

}
