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
     * @throws FileNotFoundException if the image does not exist
     * @throws AccessDeniedException if the image is not readable
     * @throws IOException if there is some other issue accessing the image
     */
    File getFile() throws IOException;

}
