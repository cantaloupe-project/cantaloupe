package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.Identifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

/**
 * Locates and provides access to a source image. This is an abstract interface;
 * implementations must implement at least one of the sub-interfaces.
 */
public interface Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return The expected source format of the image corresponding with the
     * given identifier, or <code>SourceFormat.UNKNOWN</code> if unknown;
     * never null.
     * @throws FileNotFoundException if an image corresponding to the given
     * identifier does not exist
     * @throws AccessDeniedException if an image corresponding to the given
     * identifier is not readable
     * @throws IOException if there is some other issue accessing the image
     */
    SourceFormat getSourceFormat(Identifier identifier) throws IOException;

}
