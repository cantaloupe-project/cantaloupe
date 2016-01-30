package edu.illinois.library.cantaloupe.resolver;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides input streams to read from.
 */
public interface StreamSource {

    /**
     * Provides a new ImageInputStream to read from. The returned class may be
     * a subclass of ImageInputStream that can provide more efficient access.
     *
     * @return New input stream to read from.
     * @throws IOException
     */
    ImageInputStream newImageInputStream() throws IOException;

    /**
     * Provides a new input stream to read from. The returned class may be an
     * subclass of InputStream that can provide more efficient access.
     *
     * @return New input stream to read from.
     * @throws IOException
     */
    InputStream newInputStream() throws IOException;

}
