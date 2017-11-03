package edu.illinois.library.cantaloupe.async;

import java.util.concurrent.atomic.AtomicBoolean;

class MockRunnable implements Runnable {

    private final AtomicBoolean ran = new AtomicBoolean(false);

    @Override
    public void run() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ran.set(true);
    }

    public boolean ran() {
        return ran.get();
    }

}
