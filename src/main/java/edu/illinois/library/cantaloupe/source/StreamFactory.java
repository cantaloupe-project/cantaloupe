package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.source.stream.ClosingMemoryCacheImageInputStream;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides streams for reading.
 */
public interface StreamFactory {

    /**
     * <p>Provides a new {@link InputStream} to read from.</p>
     *
     * <p>This method be called multiple times. Each call returns a new
     * instance.</p>
     *
     * @return New input stream to read from.
     * @throws IOException if there is any issue creating the stream.
     */
    InputStream newInputStream() throws IOException;

    /**
     * <p>Provides a new {@link ImageInputStream} to read from.</p>
     *
     * <p>This method be called multiple times. Each call returns a new
     * instance.</p>
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
     * <p>When overriding this method, be sure to also override {@link
     * #isSeekingDirect()}.</p>
     *
     * @return New stream to read from.
     * @throws IOException if there is any issue creating the stream.
     */
    default ImageInputStream newSeekableStream() throws IOException {
        return new ClosingMemoryCacheImageInputStream(newInputStream());
    }

    /**
     * <p>This method is used in the context of {@link #newSeekableStream()}.
     * The stream returned from that method does support seeking from the
     * client's perspective, but it may use any technique to achieve this,
     * including inefficient techniques that would, for example, read fully up
     * to a given seek position. In that case the seeking would not be
     * &quot;direct&quot; in that there is not a direct correspondence between
     * the seek the client has requested and a (however roughly) equivalent
     * seek on the source end.</p>
     *
     * <p>In contrast, a {@link javax.imageio.stream.FileImageInputStream},
     * for example, would support direct seeking because client seeks would
     * correspond clearly to source seeks.</p>
     *
     * <p>This default implementation returns {@literal false} in order to work
     * with the default implementation of {@link #newSeekableStream()}. It
     * should be overridden if that method is overridden.</p>
     *
     * @return {@literal true} if the stream returned from {@link
     *         #newSeekableStream()} supports direct seeking.
     */
    default boolean isSeekingDirect() {
        return false;
    }

}
