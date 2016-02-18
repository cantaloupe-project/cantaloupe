package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;

abstract class ResolverUtil {

    /**
     * Guesses the source format of a file based on the filename extension in
     * the given identifier.
     *
     * @param identifier
     * @return Inferred source format, or {@link Format#UNKNOWN} if
     * unknown.
     */
    public static Format inferSourceFormat(Identifier identifier) {
        String idStr = identifier.toString().toLowerCase();
        String extension = null;
        Format format = Format.UNKNOWN;
        int i = idStr.lastIndexOf('.');
        if (i > 0) {
            extension = idStr.substring(i + 1);
        }
        if (extension != null) {
            for (Format enumValue : Format.values()) {
                if (enumValue.getExtensions().contains(extension)) {
                    format = enumValue;
                    break;
                }
            }
        }
        return format;
    }

}
