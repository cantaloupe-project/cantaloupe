package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Info;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>Interface to be implemented by cache that cache derivative images and
 * metadata.</p>
 *
 * <p>Implementations must be thread-safe.</p>
 */
public interface DerivativeCache extends Cache {

    /**
     * <p>Reads the cached image information corresponding to the given
     * identifier.</p>
     *
     * <p>If invalid image information exists in the cache, implementations
     * should delete it&mdash;ideally asynchronously.</p>
     *
     * @param identifier Image identifier for which to retrieve information.
     * @return Info corresponding to the given identifier, or
     *         <code>null</code> if no valid info exists in the cache.
     * @throws IOException
     */
    Info getImageInfo(Identifier identifier) throws IOException;

    /**
     * <p>Returns an input stream corresponding to the given operation list,
     * or <code>null</code> if a valid image corresponding to the given
     * operation list does not exist in the cache.</p>
     *
     * <p>If an invalid image corresponding to the given operation list exists
     * in the cache, implementations should delete it (ideally asynchronously)
     * and return <code>null</code>.</p>
     *
     * @param opList Operation list for which to retrieve an input stream for
     *               reading from the cache.
     * @return Input stream corresponding to the given operation list, or
     *         <code>null</code> if a valid image does not exist in the cache.
     * @throws IOException
     */
    InputStream newDerivativeImageInputStream(OperationList opList)
            throws IOException;

    /**
     * <p>Returns an output stream for writing an image to the cache.</p>
     *
     * <p>If an image corresponding to the given identifier already
     * exists, the stream should overwrite it. Implementations may choose to
     * allow multiple streams to write data to the same target concurrently
     * (assuming this wouldn't cause corruption), or else allow only one
     * stream to write to a particular target at a time, with other clients
     * writing to no-op streams.</p>
     *
     * <p>The {@link CompletableOutputStream#close()} method of the returned
     * instance must check the return value of {@link
     * CompletableOutputStream#isCompletelyWritten()} before committing data
     * to the cache. If it returns {@code false}, any written data should be
     * discarded.</p>
     *
     * @param opList Operation list describing the target image in the cache.
     * @return       Output stream to which an image corresponding to the given
     *               operation list can be written.
     * @throws IOException upon an I/O error. Any partially written data is
     *         automatically cleaned up.
     */
    CompletableOutputStream newDerivativeImageOutputStream(OperationList opList)
            throws IOException;

    /**
     * Deletes the cached image corresponding to the given operation list.
     *
     * @param opList
     * @throws IOException Upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purge(OperationList opList) throws IOException;

    /**
     * <p>Adds image information to the cache.</p>
     *
     * <p>If the information corresponding to the given identifier already
     * exists, it should be overwritten.</p>
     *
     * <p>This method is synchronous.</p>
     *
     * @param identifier Image identifier.
     * @param imageInfo Info containing information about the image with
     *                  the given identifier.
     * @throws IOException
     */
    void put(Identifier identifier, Info imageInfo) throws IOException;

}
