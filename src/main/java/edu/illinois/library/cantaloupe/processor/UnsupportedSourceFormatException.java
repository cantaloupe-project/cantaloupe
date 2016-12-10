package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.operation.Format;

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

    public UnsupportedSourceFormatException(Processor proc, Format format) {
        super(proc.getClass().getSimpleName() + " does not support " +
                (format.equals(Format.UNKNOWN) ? "this" :
                        "the " + format.getName()) + " source format");
    }

}
