package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * <p>Interface to be implemented by caches that cache source images.</p>
 *
 * <p>A source cache has two purposes:</p>
 *
 * <ol>
 *     <li>To cache source images to be read by processors that only implement
 *     {@link edu.illinois.library.cantaloupe.processor.FileProcessor} and not
 *     {@link edu.illinois.library.cantaloupe.processor.StreamProcessor};</li>
 *     <li>To cache source images from unsuitably slow sources to enable faster
 *     subsequent reading.</li>
 * </ol>
 */
public interface SourceCache extends Cache {

    /**
     * <p>Returns a source image corresponding to the given identifier, or
     * {@literal null} if a valid source image corresponding to the given
     * identifier does not exist in the cache..</p>
     *
     * <p>If an image corresponding to the given identifier exists in the
     * cache but is expired, implementations should delete it before
     * returning.</p>
     *
     * <p>If the desired image is being written in another thread, this method
     * should block while waiting for it to complete.</p>
     *
     * <p><strong>Clients must not arbitrarily write to the returned
     * path.</strong> They should use
     * {@link #newSourceImageOutputStream(Identifier)} instead.</p>
     *
     * @param identifier Identifier of an image to read from the cache.
     * @return File corresponding to the given identifier, or null if a
     *         non-expired image corresponding to the given identifier does not
     *         exist in the cache.
     * @throws IOException
     */
    Path getSourceImageFile(Identifier identifier) throws IOException;

    /**
     * @param identifier Identifier of an image to write to the cache.
     * @return Output stream to which an image corresponding to the given
     *         identifier can be written.
     * @throws IOException
     */
    OutputStream newSourceImageOutputStream(Identifier identifier)
            throws IOException;

}
