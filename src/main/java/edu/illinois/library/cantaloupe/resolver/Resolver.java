package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;

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
 *     {@link #setContext(RequestContext)} (in either order)</li>
 *     <li>{@link #checkAccess()}</li>
 *     <li>Other methods</li>
 * </ol>
 */
public interface Resolver {

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
     * @return The expected source format of the image corresponding to the
     *         identifier set by {@link #setIdentifier}, or
     *         {@link Format#UNKNOWN} if unknown; never <code>null</code>.
     * @throws IOException If anything goes wrong.
     */
    Format getSourceFormat() throws IOException;

    /**
     * @param identifier Identifier of a source image to resolve.
     */
    void setIdentifier(Identifier identifier);

    /**
     * @param context The context for the resolver. Passed to delegate method.
     */
    void setContext(RequestContext context);

    /**
     * <p>Shuts the instance and any of its shared resource handles, threads,
     * etc.</p>
     *
     * <p>Will only be called at the end of the application lifecycle,</p>
     *
     * <p>Implementations must be thread-safe.</p>
     *
     * <p>The default implementation does nothing.</p>
     */
    default void shutdown() {}

}
