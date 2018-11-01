package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <p>Encapsulates an image codec (encoder + decoder) and processing engine.</p>
 *
 * <p>This is an abstract interface. Either or both of {@link FileProcessor}
 * and/or {@link StreamProcessor} must be implemented. Implementations can
 * depend on their source being set (via {@link FileProcessor#setSourceFile} or
 * {@link StreamProcessor#setStreamFactory}) before any methods that would need
 * to access it are called.</p>
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
    default InitializationException getInitializationException() {
        return null;
    }

    /**
     * @return Format of the source image.
     */
    Format getSourceFormat();

    /**
     * @return All features supported by the processor for the set source
     *         format.
     */
    Set<ProcessorFeature> getSupportedFeatures();

    /**
     * @return All qualities supported by the processor for the set source
     *         format.
     */
    Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIIIF1Qualities();

    /**
     * @return All qualities supported by the processor for the set source
     *         format.
     */
    Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIIIF2Qualities();

    /**
     * <p>Returns a list of global (not request-specific) non-fatal warnings,
     * such as deprecation notices.</p>
     *
     * <p>An instance with warnings is still usable.</p>
     *
     * <p>The return value of {@link #getInitializationException()}, if not
     * {@literal null}, should not be duplicated here.</p>
     *
     * <p>This default implementation returns an empty list.</p>
     *
     * @see #getInitializationException()
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
     *             <li>If vice versa, or if both have been called,
     *             implementations should read from a stream.</li>
     *         </ul>
     *     </li>
     *     <li>{@link edu.illinois.library.cantaloupe.operation.Operation}s
     *     should be applied in the order they are iterated. For efficiency's
     *     sake, implementations should check whether each one is a no-op using
     *     {@link edu.illinois.library.cantaloupe.operation.Operation#hasEffect(Dimension, OperationList)}
     *     before performing it.</li>
     *     <li>The {@link OperationList} will be in an immutable state.
     *     Implementations are discouraged from performing their own operations
     *     separate from the ones in the list, as this could cause problems
     *     with caching.</li>
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
     *                     #readImageInfo}, but it might not be the same
     *                     instance, as it may have come from a cache.
     * @param outputStream Stream to write the image to, which should not be
     *                     closed.
     * @throws UnsupportedOutputFormatException Implementations can extend
     *                                          {@link AbstractProcessor} and
     *                                          call super to get this check
     *                                          for free.
     * @throws ProcessorException If anything goes wrong.
     */
    void process(OperationList opList,
                 Info sourceInfo,
                 OutputStream outputStream) throws ProcessorException;

    /**
     * <p>Reads and returns information about the source image.</p>
     *
     * <p>N.B.: The returned instance will not have its {@link
     * Info#setIdentifier(Identifier) identifier set}. Clients should set it
     * manually.</p>
     *
     * @return Information about the source image.
     * @throws IOException if anything goes wrong.
     */
    Info readImageInfo() throws IOException;

    /**
     * @param format Format of the source image. Never {@link Format#UNKNOWN}.
     * @throws UnsupportedSourceFormatException if the given format is not
     *                                          supported.
     */
    void setSourceFormat(Format format) throws UnsupportedSourceFormatException;

    /**
     * <p>Validates the given operation list, throwing an exception if
     * invalid.</p>
     *
     * <p>This default implementation does the following:</p>
     *
     * <ol>
     *     <li>Calls {@link OperationList#validate}</li>
     *     <li>Ensures that the scale mode is not {@link
     *     Scale.Mode#NON_ASPECT_FILL} if {@link
     *     ProcessorFeature#SIZE_BY_DISTORTED_WIDTH_HEIGHT} is not {@link
     *     #getSupportedFeatures() supported}</li>
     * </ol>
     *
     * <p>Notes:</p>
     *
     * <ul>
     *     <li>Overrides should call super.</li>
     *     <li>It is guaranteed that this method, if called, will always be
     *     called before {@link #process}.</li>
     * </ul>
     *
     * @param opList Operation list to process. Will be equal to the one passed
     *               to {@link #process}.
     * @throws ValidationException if validation fails.
     * @throws ProcessorException  if there is an error in performing the
     *                             validation.
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
