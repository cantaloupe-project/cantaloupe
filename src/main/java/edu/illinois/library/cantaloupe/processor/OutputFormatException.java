package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

public class OutputFormatException extends FormatException {

    public OutputFormatException() {
        super("Unsupported output format");
    }

    public OutputFormatException(Format format) {
        super("Unsupported output format: " + format.getName());
    }

    public OutputFormatException(Processor processor, Format format) {
        super(String.format("%s does not support the \"%s\" output format",
                processor.getClass().getSimpleName(),
                format.getName()));
    }

}
