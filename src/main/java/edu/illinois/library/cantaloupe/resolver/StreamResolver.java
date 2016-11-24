package edu.illinois.library.cantaloupe.resolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

/**
 * Interface to be implemented by all resolvers that support access to source
 * images via input streams.
 */
public interface StreamResolver extends Resolver {

    /**
     * @return StreamSource for reading the source image identified by the
     *         identifier passed to {@link #setIdentifier}; never null.
     * @throws FileNotFoundException If the image corresponding to the given
     *                               identifier does not exist.
     * @throws AccessDeniedException If the image corresponding to the given
     *                               identifier is not readable.
     * @throws IOException If there is some other issue accessing the image.
     */
    StreamSource getStreamSource() throws IOException;

}
