package edu.illinois.library.cantaloupe.processor;

public class InitializationException extends ProcessorException {

    public InitializationException(Exception e) {
        super(e.getMessage(), e);
    }

}
