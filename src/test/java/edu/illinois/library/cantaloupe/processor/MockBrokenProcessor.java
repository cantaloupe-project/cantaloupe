package edu.illinois.library.cantaloupe.processor;

class MockBrokenProcessor extends Java2dProcessor {

    @Override
    public InitializationException getInitializationException() {
        return new InitializationException(new Exception("I'm broken"));
    }

}
