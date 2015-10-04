package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.request.Identifier;

import java.io.File;
import java.io.IOException;

/**
 * Interface to be implemented by all resolvers that support access to source
 * images via File objects.
 */
public interface FileResolver extends Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return File referencing the source image; never null.
     * @throws IOException if the image corresponding to the given identifier
     * does not exist.
     */
    File getFile(Identifier identifier) throws IOException;

}
