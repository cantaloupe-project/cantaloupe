package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;

/**
 * Infers a format based on an {@link
 * edu.illinois.library.cantaloupe.image.Identifier}.
 */
final class IdentifierFormatChecker implements FormatChecker {

    private Identifier identifier;
    private transient Format format;

    IdentifierFormatChecker(Identifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public Format check() {
        if (format == null) {
            format = Format.inferFormat(identifier);
        }
        return format;
    }

}
