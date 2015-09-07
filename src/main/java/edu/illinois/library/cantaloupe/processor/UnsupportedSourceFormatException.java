package edu.illinois.library.cantaloupe.processor;

public class UnsupportedSourceFormatException extends RuntimeException {

    public UnsupportedSourceFormatException() {
        super("Unsupported source format");
    }

    public UnsupportedSourceFormatException(String message) {
        super(message);
    }

}
