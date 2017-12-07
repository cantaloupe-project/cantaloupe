package edu.illinois.library.cantaloupe.resolver;

import java.io.IOException;

/**
 * Interface to be implemented by all resolvers that support access to source
 * images via input streams.
 */
public interface StreamResolver extends Resolver {

    /**
     * @return Source from which to read the source image identified by the
     *         identifier passed to {@link #setIdentifier}; never null.
     * @throws IOException If anything goes wrong.
     */
    StreamSource newStreamSource() throws IOException;

}
