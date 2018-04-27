package edu.illinois.library.cantaloupe.source;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Source that supports access to source images via {@link Path}s.
 */
public interface FileSource extends Source {

    /**
     * @return File referencing the source image corresponding to the
     *         identifier set with {@link #setIdentifier}; never
     *         <code>null</code>.
     * @throws IOException If anything goes wrong.
     */
    Path getPath() throws IOException;

}
