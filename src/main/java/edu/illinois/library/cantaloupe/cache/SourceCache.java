package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.operation.Identifier;

import java.io.File;
import java.io.OutputStream;

/**
 * Interface to be implemented by caches that cache source images.
 */
public interface SourceCache extends Cache {

    /**
     * <p>Returns a File for a source image corresponding to the given
     * identifier, or null if a non-expired source image corresponding to the
     * given identifier does not exist in the cache..</p>
     *
     * <p>If an image corresponding to the given identifier exists in the
     * cache but is expired, implementations should delete it before returning
     * null.</p>
     *
     * <p>If the desired image is being written in another thread, this method
     * should block while waiting for it to complete.</p>
     *
     * <p><strong>Clients must not use the returned File for
     * writing.</strong> They should use
     * {@link #getImageOutputStream(Identifier)} instead.</p>
     *
     * @param identifier Identifier of an image to read from the cache.
     * @return File corresponding to the given identifier, or null if a
     *         non-expired image corresponding to the given identifier does not
     *         exist in the cache.
     * @throws CacheException
     */
    File getImageFile(Identifier identifier) throws CacheException;

    /**
     * @param identifier Identifier of an image to write to the cache.
     * @return Output stream to which an image corresponding to the given
     *         identifier can be written.
     * @throws CacheException
     */
    OutputStream getImageOutputStream(Identifier identifier)
            throws CacheException;

}
