package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;

/**
 * Infers a format based on some kind of name, such as a filename or object
 * key.
 */
final class NameFormatChecker implements FormatChecker {

    private String name;
    private transient Format format;

    NameFormatChecker(String name) {
        this.name = name;
    }

    @Override
    public Format check() {
        if (format == null) {
            format = Format.inferFormat(name);
        }
        return format;
    }

}
