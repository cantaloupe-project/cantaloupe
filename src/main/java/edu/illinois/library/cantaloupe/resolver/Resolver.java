package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

/**
 * Locates and provides access to a source image. This is an abstract interface;
 * implementations must implement at least one of the sub-interfaces.
 */
public interface Resolver {

    /**
     * @return The expected source format of the image corresponding to the
     *         identifier set by {@link #setIdentifier}, or
     *         {@link Format#UNKNOWN} if unknown; never <code>null</code>.
     * @throws FileNotFoundException if an image corresponding to the given
     *         identifier does not exist.
     * @throws AccessDeniedException if an image corresponding to the given
     *         identifier is not readable.
     * @throws IOException if there is some other issue accessing the image.
     */
    Format getSourceFormat() throws IOException;

    /**
     * @param identifier Identifier of a source image to resolve.
     */
    void setIdentifier(Identifier identifier);

}
