package edu.illinois.library.cantaloupe.processor;

public class InitializationException extends ProcessorException {

    public InitializationException(Throwable t) {
        super(t.getMessage(), t);
    }

}
