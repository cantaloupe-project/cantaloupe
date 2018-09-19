package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.DelegateProxy;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

/**
 * <p>Locates and provides access to a source image. This is an abstract
 * interface; at least one of the sub-interfaces must be implemented.</p>
 *
 * <p>Methods are guaranteed to be called in the following order:</p>
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
     * <p>Checks the accessibility of the source image.</p>
     *
     * <p>This method is guaranteed to be called only once.</p>
     *
     * @throws NoSuchFileException if an image corresponding to the set
     *         identifier does not exist.
     * @throws AccessDeniedException if an image corresponding to the set
     *         identifier is not readable.
     * @throws IOException if there is some other issue accessing the image.
     */
    void checkAccess() throws IOException;

    /**
     * @return The expected format of the image corresponding to the
     *         identifier set by {@link #setIdentifier}, or
     *         {@link Format#UNKNOWN} if unknown; never {@literal null}.
     * @throws IOException if anything goes wrong.
     */
    Format getFormat() throws IOException;

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
     * @param proxy Delegate proxy for the current request.
     */
    void setDelegateProxy(DelegateProxy proxy);

    /**
     * <p>Shuts down the instance and any of its shared resource handles,
     * threads, etc.</p>
     *
     * <p>Will only be called at the end of the application lifecycle,</p>
     *
     * <p>Implementations must be thread-safe.</p>
     *
     * <p>The default implementation does nothing.</p>
     */
    default void shutdown() {}

}
