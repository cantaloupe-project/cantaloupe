package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * <p>Locates, provides access to, and infers the format of a source image.
 * This is an abstract interface; at least one of the sub-interfaces must be
 * implemented.</p>
 *
 * <p>Methods are called in the following order:</p>
 *
 * <ol>
 *     <li>{@link #setIdentifier(Identifier)} and
 *     {@link #setDelegateProxy(DelegateProxy)} (in either order)</li>
 *     <li>{@link #checkAccess()}</li>
 *     <li>Any other methods</li>
 *     <li>{@link #shutdown()}</li>
 * </ol>
 */
public interface Source {

    /**
     * @return Identifier of the source image to read.
     * @since 4.1
     */
    Identifier getIdentifier();

    /**
     * @param identifier Identifier of the source image to read.
     */
    void setIdentifier(Identifier identifier);

    /**
     * <p>Checks the accessibility of the source image.</p>
     *
     * <p>Will be called only once.</p>
     *
     * @throws NoSuchFileException if an image corresponding to the set
     *         identifier does not exist.
     * @throws AccessDeniedException if an image corresponding to the set
     *         identifier is not readable.
     * @throws IOException if there is some other issue accessing the image.
     */
    void checkAccess() throws IOException;

    /**
     * N.B.: This default implementation throws an {@link
     * UnsupportedOperationException}. It must be overridden if {@link
     * #supportsFileAccess()} returns {@code true}.
     *
     * @return File referencing the source image corresponding to the
     *         identifier set with {@link #setIdentifier}; never {@code null}.
     * @throws UnsupportedOperationException if access to files is not {@link
     *         #supportsFileAccess() supported}.
     * @throws IOException if anything goes wrong.
     */
    default Path getFile() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>Returns an iterator over the results of various techniques of
     * checking the format, in the order of least to most expensive. Any of the
     * calls to {@link Iterator#next()} or may return either an inaccurate
     * value, or {@link Format#UNKNOWN}. Clients should proceed using the first
     * non-unknown format they encounter and, if this turns out to be wrong,
     * iterate and try again.</p>
     *
     * @return Iterator over whatever format-inference strategies the instance
     *         supports. <strong>The instance is cached and the same one is
     *         returned every time.</strong>
     * @since 5.0
     */
    Iterator<Format> getFormatIterator();

    /**
     * @return Instance from which to read the source image identified by the
     *         identifier passed to {@link #setIdentifier}; never {@code null}.
     * @throws IOException if anything goes wrong.
     */
    StreamFactory newStreamFactory() throws IOException;

    /**
     * @param proxy Delegate proxy for the current request.
     */
    void setDelegateProxy(DelegateProxy proxy);

    /**
     * <p>Shuts down the instance and any of its shared resource handles,
     * threads, etc.</p>
     *
     * <p>Only called at the end of the application lifecycle.</p>
     *
     * <p>The default implementation does nothing.</p>
     */
    default void shutdown() {}

    /**
     * N.B. 1: This method's return value affects the behavior of {@link
     * #getFile()}. See the documentation of that method for more information.
     *
     * N.B. 2: The default implementation returns {@code false}.
     *
     * @return Whether the source image can be accessed via a {@link
     *         java.nio.file.Path}.
     */
    default boolean supportsFileAccess() {
        return false;
    }

}
