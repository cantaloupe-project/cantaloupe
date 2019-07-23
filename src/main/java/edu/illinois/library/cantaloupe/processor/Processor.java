package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.source.StreamFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <p>Encapsulates an image codec and processing engine.</p>
 *
 * <p>This is an abstract interface. Either or both of {@link FileProcessor}
 * and/or {@link StreamProcessor} must be implemented&mdash;but ideally at
 * least {@link StreamProcessor}.</p>
 *
 * <p>Implementations can depend on their source being set (via {@link
 * FileProcessor#setSourceFile} or {@link StreamProcessor#setStreamFactory})
 * before any methods that would need to access it are called.</p>
 *
 * @since 1.0
 */
public interface Processor extends AutoCloseable {

    /**
     * Releases all resources used by the instance.
     */
    @Override
    void close();

    /**
     * @return Output formats available for the {@link #setSourceFormat(Format)
     *         set source format}, or an empty set if none.
     */
    Set<Format> getAvailableOutputFormats();

    /**
     * <p>Implementations may need to perform initialization (such as scanning
     * for supported formats etc.) that is more efficient to do only once, at
     * application startup. If this fails, this method should return a
     * non-{@literal null} value, signifying that the instance is in an
     * unusable state.</p>
     *
     * <p>This default implementation returns {@literal null}.</p>
     *
     * @see #getWarnings()
     * @since 3.4
     */
    default String getInitializationError() {
        return null;
    }

    /**
     * @return The same format passed to {@link #setSourceFormat(Format)}.
     */
    Format getSourceFormat();

    /**
     * @return All qualities supported for the {@link #setSourceFormat(Format)
     *         given source format}.
     */
    Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIIIF1Qualities();

    /**
     * @return All qualities supported for the {@link #setSourceFormat(Format)
     *         given source format}.
     */
    Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIIIF2Qualities();

    /**
     * <p>Returns a list of global (not request-specific) non-fatal warnings,
     * such as deprecation notices.</p>
     *
     * <p>An instance with warnings is still usable.</p>
     *
     * <p>The return value of {@link #getInitializationError()}, if not {@code
     * null}, should not be duplicated here.</p>
     *
     * <p>This default implementation returns an empty list.</p>
     *
     * @see #getInitializationError()
     * @since 3.4
     */
    default List<String> getWarnings() {
        return Collections.emptyList();
    }

    /**
     * <p>Performs the supplied operations on an image, writing the result to
     * the supplied output stream.</p>
     *
     * <p>Implementation notes:</p>
     *
     * <ul>
     *     <li>The source to read from will differ depending on whether
     *     implementations implement {@link FileProcessor} or
     *     {@link StreamProcessor}:
     *         <ul>
     *             <li>If {@link FileProcessor#setSourceFile(Path)} has been
     *             called, but not {@link
     *             StreamProcessor#setStreamFactory(StreamFactory)},
     *             implementations should read from a file.</li>
     *             <li>If vice versa, implementations should read from a
     *             stream.</li>
     *             <li>If both have been called, implementations can read from
     *             either one.</li>
     *         </ul>
     *     </li>
     *     <li>{@link edu.illinois.library.cantaloupe.operation.Operation}s
     *     should be applied in the order they are iterated.</li>
     *     <li>The {@link OperationList} will be {@link OperationList#freeze()
     *     frozen}. Implementations are discouraged from performing operations
     *     other than the ones in the list, as this could cause problems with
     *     caching.</li>
     *     <li>In addition to operations, the operation list may contain a
     *     number of {@link OperationList#getOptions() options}, which
     *     implementations should respect, where applicable. Option values may
     *     originate from the configuration, so implementations should not try
     *     to read the configuration themselves, as this could also cause
     *     problems with caching.</li>
     * </ul>
     *
     * @param opList       Operation list to process. As it will be equal to
     *                     the one passed to {@link #validate}, there is no
     *                     need to validate it again.
     * @param sourceInfo   Information about the source image. This will
     *                     probably be equal to the return value of {@link
     *                     #readInfo}, but it might not be the same
     *                     instance, as it may have come from a cache.
     * @param outputStream Stream to write the image to, which should not be
     *                     closed.
     * @throws SourceFormatException if the actual format of the
     *         source image is different from the one passed to {@link
     *         #setSourceFormat(Format)}.
     * @throws OutputFormatException if the {@link
     *         OperationList#getOutputFormat() output format} is not supported.
     * @throws ProcessorException if anything else goes wrong.
     */
    void process(OperationList opList,
                 Info sourceInfo,
                 OutputStream outputStream) throws FormatException, ProcessorException;

    /**
     * <p>Reads and returns information about the source image.</p>
     *
     * <p>If any of the {@link Info}'s properties cannot be set due to an
     * implementation's inability to read them, it is marked as {@link
     * Info#setComplete(boolean) incomplete}.</p>
     *
     * @return Information about the source image, without an {@link
     *         Info#getIdentifier() identifier} set.
     * @throws SourceFormatException if the actual format of the source image
     *         is different from the one passed to {@link
     *         #setSourceFormat(Format)}.
     * @throws IOException if anything else goes wrong.
     */
    Info readInfo() throws IOException;

    /**
     * @param format Expected format of the source image.
     * @throws SourceFormatException if the given format is not supported.
     */
    void setSourceFormat(Format format) throws SourceFormatException;

    /**
     * <p>Validates the given operation list, throwing an exception if
     * invalid.</p>
     *
     * <p>This default implementation does the following:</p>
     *
     * <ol>
     *     <li>Calls {@link OperationList#validate}</li>
     * </ol>
     *
     * <p>Notes:</p>
     *
     * <ul>
     *     <li>Overrides should call {@code super}.</li>
     *     <li>It is guaranteed that this method, if called, will always be
     *     called before {@link #process}.</li>
     * </ul>
     *
     * @param opList Operation list to process. Equal to the one passed to
     *               {@link #process}.
     * @throws ValidationException if validation fails.
     * @throws ProcessorException if there is an error in performing the
     *         validation.
     */
    default void validate(OperationList opList, Dimension fullSize)
            throws ValidationException, ProcessorException {
        opList.validate(fullSize, getSourceFormat());

        // TODO: bind Scale.Mode to ProcessorFeature and validate whether the
        // processor supports all of the requested operations.
        // This isn't needed as of 4.1 because all processors support all
        // operations.
    }

}
