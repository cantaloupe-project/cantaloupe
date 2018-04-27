package edu.illinois.library.cantaloupe.source;

import java.io.IOException;

/**
 * Source that supports access to source images via streams.
 */
public interface StreamSource extends Source {

    /**
     * @return Source from which to read the source image identified by the
     *         identifier passed to {@link #setIdentifier}; never null.
     * @throws IOException If anything goes wrong.
     */
    StreamFactory newStreamFactory() throws IOException;

}
