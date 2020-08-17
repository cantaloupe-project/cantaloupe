package edu.illinois.library.cantaloupe.async;

import java.util.concurrent.Callable;

class MockCallable<T> extends MockTask implements Callable<T> {

    @Override
    public T call() throws Exception {
        Thread.sleep(WAIT);
        ran.set(true);
        return null;
    }

}
