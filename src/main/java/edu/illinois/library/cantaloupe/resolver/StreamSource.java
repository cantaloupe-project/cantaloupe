package edu.illinois.library.cantaloupe.resolver;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides input streams to read from.
 */
public interface StreamSource {

    /**
     * <p>Provides a new ImageInputStream to read from.</p>
     *
     * <p>N.B. ImageInputStream is an ImageIO class that supports seeking,
     * among other benefits, making it potentially much more efficient than an
     * InputStream. Implementations are encouraged to return a full-fledged
     * ImageInputStream, but if they can't, they can return a wrapped
     * InputStream using {@link ImageIO#createImageInputStream(Object)}.</p>
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
