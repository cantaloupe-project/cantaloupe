package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

public class UnsupportedSourceFormatException extends ProcessorException {

    public UnsupportedSourceFormatException() {
        super("Unsupported source format");
    }

    public UnsupportedSourceFormatException(String message) {
        super(message);
    }

    public UnsupportedSourceFormatException(Format format) {
        super("Unsupported source format: " + format.getName());
    }

}
