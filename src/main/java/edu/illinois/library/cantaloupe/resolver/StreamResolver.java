package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.request.Identifier;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface to be implemented by all resolvers that support access to source
 * images via streams.
 */
public interface StreamResolver extends Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return Stream for reading the source image; never null.
     * @throws FileNotFoundException if the image corresponding to the given
     * identifier does not exist
     * @throws AccessDeniedException if the image corresponding to the given
     * identifier is not readable
     * @throws IOException if there is some other issue accessing the image
     */
    InputStream getInputStream(Identifier identifier) throws IOException;

}
