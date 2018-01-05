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
     * <p>Provides a new {@link ImageInputStream} to read from. May be called
     * multiple times.</p>
     *
     * <p>N.B.: {@link ImageInputStream} is an ImageIO class that supports
     * seeking, among other benefits, making it potentially much more efficient
     * than an {@link InputStream}. If a first-class implementation can't be
     * returned, then {@link ImageIO#createImageInputStream} can be used to
     * return a wrapped {@link InputStream}.</p>
     *
     * @return New input stream to read from.
     * @throws IOException If there is any issue creating the stream.
     */
    ImageInputStream newImageInputStream() throws IOException;

    /**
     * Provides a new input stream to read from. May be called multiple times.
     *
     * @return New input stream to read from.
     * @throws IOException If there is any issue creating the stream.
     */
    InputStream newInputStream() throws IOException;

}
