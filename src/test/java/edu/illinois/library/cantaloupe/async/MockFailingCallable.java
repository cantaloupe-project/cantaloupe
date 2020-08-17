package edu.illinois.library.cantaloupe.async;

import java.util.concurrent.Callable;

class MockFailingCallable<T> extends MockTask  implements Callable<T> {

    @Override
    public T call() throws Exception {
        Thread.sleep(WAIT);
        throw new Exception("Failed, as requested");
    }

}
