package edu.illinois.library.cantaloupe.processor;

public class ProcessorException extends Exception {

    public ProcessorException(String message) {
        super(message);
    }

    public ProcessorException(String message, Throwable t) {
        super(message, t);
    }

    public ProcessorException(Throwable t) {
        super(t);
    }

}
