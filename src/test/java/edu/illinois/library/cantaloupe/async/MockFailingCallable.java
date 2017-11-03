package edu.illinois.library.cantaloupe.async;

import java.util.concurrent.Callable;

class MockFailingCallable<T> implements Callable<T> {

    @Override
    public T call() throws Exception {
        Thread.sleep(100);
        throw new Exception("Failed, as requested");
    }

}
