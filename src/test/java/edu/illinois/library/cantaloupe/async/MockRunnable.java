package edu.illinois.library.cantaloupe.async;

class MockRunnable extends MockTask implements Runnable {

    @Override
    public void run() {
        try {
            Thread.sleep(WAIT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ran.set(true);
    }

}
