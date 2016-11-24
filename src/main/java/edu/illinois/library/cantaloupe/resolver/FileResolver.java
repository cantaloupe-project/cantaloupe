package edu.illinois.library.cantaloupe.resolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

/**
 * Interface to be implemented by all resolvers that support access to source
 * images via File objects.
 */
public interface FileResolver extends Resolver {

    /**
     * @return File referencing the source image corresponding to the
     *         identifier set with {@link #setIdentifier}; never null.
     * @throws FileNotFoundException If the image corresponding to the given
     *                               identifier does not exist.
     * @throws AccessDeniedException If the image corresponding to the given
     *                               identifier is not readable.
     * @throws IOException If there is some other issue accessing the image.
     */
    File getFile() throws IOException;

}
