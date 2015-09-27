package edu.illinois.library.cantaloupe.processor;

public class UnsupportedOutputFormatException extends ProcessorException {

    public UnsupportedOutputFormatException() {
        super("Unsupported output format");
    }

    public UnsupportedOutputFormatException(String message) {
        super(message);
    }

}
