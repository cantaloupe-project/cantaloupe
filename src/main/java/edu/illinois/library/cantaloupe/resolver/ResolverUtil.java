package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import org.apache.commons.lang3.StringUtils;

abstract class ResolverUtil {

    /**
     * Guesses the source format of a file based on the filename extension in
     * the given identifier.
     *
     * @param identifier
     * @return Inferred source format, or {@link SourceFormat#UNKNOWN} if
     * unknown.
     */
    public static SourceFormat inferSourceFormat(Identifier identifier) {
        String idStr = identifier.toString().toLowerCase();
        String extension = null;
        SourceFormat sourceFormat = SourceFormat.UNKNOWN;
        int i = idStr.lastIndexOf('.');
        if (i > 0) {
            extension = idStr.substring(i + 1);
        }
        if (extension != null) {
            for (SourceFormat enumValue : SourceFormat.values()) {
                if (enumValue.getExtensions().contains(extension)) {
                    sourceFormat = enumValue;
                    break;
                }
            }
        }
        return sourceFormat;
    }

    /**
     * Some web servers have issues dealing with encoded slashes (%2F) in URL
     * identifiers. This method enables the use of an alternate string as a
     * path separator.
     *
     * @param identifier
     * @param currentSeparator
     * @param newSeparator
     * @return
     */
    public static Identifier replacePathSeparators(Identifier identifier,
                                                   String currentSeparator,
                                                   String newSeparator) {
        final String idStr = StringUtils.replace(identifier.toString(),
                currentSeparator, newSeparator);
        return new Identifier(idStr);
    }

}
