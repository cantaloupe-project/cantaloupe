package edu.illinois.library.cantaloupe.async;

class MockFailingTask extends MockTask implements Task {

    @Override
    public void run() throws Exception {
        Thread.sleep(100);
        throw new Exception("Failed, as requested");
    }

}
