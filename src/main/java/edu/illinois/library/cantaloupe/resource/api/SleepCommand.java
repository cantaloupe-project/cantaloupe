package edu.illinois.library.cantaloupe.resource.api;

import java.util.concurrent.Callable;

/**
 * Sleeps for a specified duration.
 */
final class SleepCommand<T> extends Command implements Callable<T> {

    private int duration;

    @Override
    public T call() throws Exception {
        Thread.sleep(duration * 1000);
        return null;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    String getVerb() {
        return "Sleep";
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

}
