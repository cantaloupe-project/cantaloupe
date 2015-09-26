package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;

public class UnsupportedSourceFormatException extends RuntimeException {

    public UnsupportedSourceFormatException() {
        super("Unsupported source format");
    }

    public UnsupportedSourceFormatException(String message) {
        super(message);
    }

    public UnsupportedSourceFormatException(SourceFormat format) {
        super("Unsupported source format: " + format);
    }

}
