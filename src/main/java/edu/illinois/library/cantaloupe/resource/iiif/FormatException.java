package edu.illinois.library.cantaloupe.resource.iiif;

public class FormatException extends IllegalArgumentException {

    public FormatException(String format) {
        super("Unsupported format: " + format.replaceAll("[^A-Za-z0-9]", ""));
    }

}
