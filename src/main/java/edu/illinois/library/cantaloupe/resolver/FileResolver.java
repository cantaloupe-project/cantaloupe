package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.request.Identifier;

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
     * @param identifier IIIF identifier.
     * @return File referencing the source image; never null.
     * @throws FileNotFoundException if the image corresponding to the given
     * identifier does not exist
     * @throws AccessDeniedException if the image corresponding to the given
     * identifier is not readable
     * @throws IOException if there is some other issue accessing the image
     */
    File getFile(Identifier identifier) throws IOException;

}
