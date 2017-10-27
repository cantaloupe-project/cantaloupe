package edu.illinois.library.cantaloupe.async;

import java.util.concurrent.atomic.AtomicBoolean;

public class MockTask extends AbstractTask implements Task {

    private final AtomicBoolean ran = new AtomicBoolean(false);

    @Override
    public void run() throws Exception {
        Thread.sleep(100);
        ran.set(true);
    }

    public boolean ran() {
        return ran.get();
    }

}
