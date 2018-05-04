package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.processor.codec.ClosingMemoryCacheImageInputStream;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides input streams to read from.
 */
public interface StreamFactory {

    /**
     * <p>Provides a new {@link ImageInputStream} to read from.</p>
     *
     * <p>This method may be called multiple times.</p>
     *
     * <p>N.B.: {@link ImageInputStream} is an ImageIO class that supports
     * seeking, among other benefits, making it potentially much more efficient
     * than an {@link InputStream}. If a first-class implementation can't be
     * returned, then a {@link ClosingMemoryCacheImageInputStream} (or
     * something similar whose {@link ImageInputStream#close} method actually
     * closes the wrapped stream) that wraps the result of {@link
     * #newInputStream()} can be returned instead. (That is what this default
     * implementation does.)</p>
     *
     * @return New stream to read from.
     * @throws IOException if there is any issue creating the stream.
     */
    default ImageInputStream newImageInputStream() throws IOException {
        return new ClosingMemoryCacheImageInputStream(newInputStream());
    }

    /**
     * Provides a new input stream to read from. May be called multiple times.
     *
     * @return New input stream to read from.
     * @throws IOException If there is any issue creating the stream.
     */
    InputStream newInputStream() throws IOException;

}
