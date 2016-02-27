package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

/**
 * Locates and provides access to a source image. This is an abstract interface;
 * implementations must implement at least one of the sub-interfaces.
 */
public interface Resolver {

    /**
     * @return The expected source format of the image corresponding to the
     * identifier set by {@link #setIdentifier}, or {@link Format#UNKNOWN} if
     * unknown; never null.
     * @throws FileNotFoundException if an image corresponding to the given
     * identifier does not exist
     * @throws AccessDeniedException if an image corresponding to the given
     * identifier is not readable
     * @throws IOException if there is some other issue accessing the image
     */
    Format getSourceFormat() throws IOException;

    /**
     * <dl>
     *     <dt>{@link FileResolver} &rarr; {@link StreamProcessor}</dt>
     *     <dd>OK, using {@link java.io.FileInputStream}</dd>
     *     <dt>{@link FileResolver} &rarr; {@link FileProcessor}</dt>
     *     <dd>OK, using {@link java.io.File}</dd>
     *     <dt>{@link StreamResolver} &rarr; {@link StreamProcessor}</dt>
     *     <dd>OK, using {@link java.io.InputStream}</dd>
     *     <dt>{@link StreamResolver} &rarr; {@link FileProcessor}</dt>
     *     <dd>Incompatible</dd>
     * </dl>
     *
     * @param processor
     * @return Whether the instance is compatible with the given processor.
     */
    boolean isCompatible(Processor processor);

    /**
     * @param identifier Identifier of a source image to resolve.
     */
    void setIdentifier(Identifier identifier);

}
