package edu.illinois.library.cantaloupe.resolver;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides input streams to read from.
 */
public interface StreamSource {

    /**
     * Provides a new ImageInputStream to read from.
     *
     * @return New input stream to read from.
     * @throws IOException If there is any issue creating the stream.
     */
    ImageInputStream newImageInputStream() throws IOException;

    /**
     * Provides a new input stream to read from.
     *
     * @return New input stream to read from.
     * @throws IOException If there is any issue creating the stream.
     */
    InputStream newInputStream() throws IOException;

}
