package edu.illinois.library.cantaloupe.resolver;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface to be implemented by all resolvers that support access to source
 * images via File objects.
 */
public interface FileResolver extends Resolver {

    /**
     * @return File referencing the source image corresponding to the
     *         identifier set with {@link #setIdentifier}; never
     *         <code>null</code>.
     * @throws IOException If anything goes wrong.
     */
    Path getPath() throws IOException;

}
