package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Info;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

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
     * @return           Info corresponding to the given identifier.
     */
    Optional<Info> getInfo(Identifier identifier) throws IOException;

    /**
     * <p>Returns an input stream corresponding to the given operation list,
     * or {@code null} if a valid image corresponding to the given operation
     * list does not exist in the cache.</p>
     *
     * <p>If an invalid image corresponding to the given operation list exists
     * in the cache, implementations should delete it (ideally asynchronously)
     * and return {@code null}.</p>
     *
     * @param opList Operation list for which to retrieve an input stream for
     *               reading from the cache.
     * @return       Input stream corresponding to the given operation list, or
     *               {@code null} if a valid image does not exist in the cache.
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
     * CompletableOutputStream#isComplete()} before committing data
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
     * @throws IOException upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     */
    void purge(OperationList opList) throws IOException;

    /**
     * Deletes all cached infos.
     *
     * @throws IOException upon fatal error. Implementations should do the
     *         best they can to complete the operation and swallow and log
     *         non-fatal errors.
     * @since 5.0
     */
    void purgeInfos() throws IOException;

    /**
     * <p>Synchronously adds image information to the cache.</p>
     *
     * <p>If the information corresponding to the given identifier already
     * exists, it will be overwritten.</p>
     *
     * <p>Non-{@link Info#isPersistable() persistable} instances are silently
     * ignored.</p>
     *
     * @param identifier Image identifier.
     * @param info       Information about the image corresponding with the
     *                   given identifier.
     */
    void put(Identifier identifier, Info info) throws IOException;

    /**
     * <p>Alternative to {@link #put(Identifier, Info)} that adds a raw UTF-8
     * string to the cache, trusting that it is a JSON-serialized {@link Info}
     * instance.</p>
     *
     * <p>This method is used mainly for testing. {@link
     * #put(Identifier, Info)} should normally be used instead.</p>
     *
     * @param identifier Image identifier.
     * @param info       JSON-encoded information about the image corresponding
     *                   with the given identifier, obtained (for example) from
     *                   {@link Info#toJSON()}.
     */
    void put(Identifier identifier, String info) throws IOException;

}
