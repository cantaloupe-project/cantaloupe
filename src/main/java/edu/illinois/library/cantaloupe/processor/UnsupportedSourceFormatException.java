package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

import java.io.IOException;

public class UnsupportedSourceFormatException extends IOException {

    private static String getMessage(Processor proc, Format format) {
        String msg;
        if (Format.UNKNOWN.equals(format)) {
            msg = proc.getClass().getSimpleName() + " only supports known " +
                    "source formats";
        } else {
            msg = proc.getClass().getSimpleName() + " does not support the " +
                    format.getName() + " source format";
        }
        return msg;
    }

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
        super(getMessage(proc, format));
    }

}
