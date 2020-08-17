package edu.illinois.library.cantaloupe.async;

import java.util.concurrent.atomic.AtomicBoolean;

abstract class MockTask {

    final static int WAIT = 400;

    final AtomicBoolean ran = new AtomicBoolean(false);

    public boolean ran() {
        return ran.get();
    }

}
