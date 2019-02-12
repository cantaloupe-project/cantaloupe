package edu.illinois.library.cantaloupe.processor;

class MockBrokenProcessor extends Java2dProcessor {

    @Override
    public String getInitializationError() {
        return "I'm broken";
    }

}
