package edu.illinois.library.cantaloupe.async;

import java.util.concurrent.atomic.AtomicBoolean;

abstract class MockTask {

    final static int WAIT = 200;

    final AtomicBoolean ran = new AtomicBoolean(false);

    public boolean ran() {
        return ran.get();
    }

}
