package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

public class UnsupportedOutputFormatException extends ProcessorException {

    public UnsupportedOutputFormatException() {
        super("Unsupported output format");
    }

    public UnsupportedOutputFormatException(String message) {
        super(message);
    }

    public UnsupportedOutputFormatException(Format format) {
        super("Unsupported output format: " + format.getName());
    }

}
