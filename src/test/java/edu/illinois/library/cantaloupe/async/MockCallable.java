package edu.illinois.library.cantaloupe.async;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class MockCallable<T> implements Callable<T> {

    private final AtomicBoolean ran = new AtomicBoolean(false);

    @Override
    public T call() throws Exception {
        Thread.sleep(100);
        ran.set(true);
        return null;
    }

    public boolean ran() {
        return ran.get();
    }

}
