package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

public class SourceFormatException extends FormatException {

    private static String getMessage(Processor proc, Format format) {
        String msg;
        if (Format.UNKNOWN.equals(format)) {
            msg = "Unknown source format";
        } else {
            msg = proc.getClass().getSimpleName() + " does not support the " +
                    format.getName() + " source format";
        }
        return msg;
    }

    public SourceFormatException() {
        super("Unsupported source format");
    }

    public SourceFormatException(String message) {
        super(message);
    }

    public SourceFormatException(Format format) {
        super("Unsupported source format: " + format.getName());
    }

    public SourceFormatException(Processor proc, Format format) {
        super(getMessage(proc, format));
    }

}
