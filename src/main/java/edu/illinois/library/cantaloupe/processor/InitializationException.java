package edu.illinois.library.cantaloupe.processor;

public class InitializationException extends ProcessorException {

    public InitializationException(String msg) {
        super(msg);
    }

    public InitializationException(Throwable t) {
        super(t.getMessage(), t);
    }

}
