package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;

import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <p>Abstract image processor interface.</p>
 *
 * <p>Implementations must implement {@link FileProcessor} and/or
 * {@link StreamProcessor} and can assume that their source will be set (via
 * {@link FileProcessor#setSourceFile} or
 * {@link StreamProcessor#setStreamSource}) before any other methods are
 * called.</p>
 */
public interface Processor extends AutoCloseable {

    /**
     * Releases all resources used by the instance.
     */
    @Override
    default void close() {}

    /**
     * @return Output formats available for the {@link #setSourceFormat(Format)
     * set source format}, or an empty set if none.
     */
    Set<Format> getAvailableOutputFormats();

    /**
     * <p>Implementations may need to perform initialization (such as scanning
     * for supported formats etc.) that is more efficient to do only once, at
     * initialization time. If this fails, this method should return a
     * non-null value, which signifies that the instance is in an unusable
     * state.</p>
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
     * @return The source format of the image to be processed.
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
     *             StreamProcessor#setStreamSource(StreamSource)},
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
     *     <li>The {@link OperationList} will be in a frozen (immutable) state.
     *     Implementations are discouraged from performing their own operations
     *     separate from the ones in the list, as this could cause problems
     *     with caching.</li>
     *     <li>In addition to operations, the operation list may contain a
     *     number of {@link OperationList#getOptions() options}, which
     *     implementations should respect, where applicable. These typically
     *     come from the configuration, so implementations should not try to
     *     read the configuration themselves, except to get their own
     *     processor-specific info, as this could cause problems with
     *     caching.</li>
     * </ul>
     *
     * @param opList       Operation list to process. As it will be equal to
     *                     the one passed to {@link #validate}, there is no
     *                     need to validate it again.
     * @param sourceInfo   Information about the source image. This will be
     *                     equal to the return value of {@link #readImageInfo},
     *                     but it might not be the same instance, as it may
     *                     have come from a cache.
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
     * @return Information about the source image.
     * @throws IOException if anything goes wrong.
     */
    Info readImageInfo() throws IOException;

    /**
     * @param format Format of the source image. Will never be
     *               {@link Format#UNKNOWN}.
     * @throws UnsupportedSourceFormatException if the given format is not
     *                                          supported.
     */
    void setSourceFormat(Format format) throws UnsupportedSourceFormatException;

    /**
     * <p>Validates the given operation list, throwing an exception if
     * invalid.</p>
     *
     * <p>This default implementation simply validates the given operation
     * list.</p>
     *
     * <p>Notes:</p>
     *
     * <ul>
     *     <li>This method is only for validating processor-specific options
     *     in the list's {@link OperationList#getOptions() options map}.
     *     Implementations that don't use these will not need to override
     *     this.</li>
     *     <li>Implementations should call {@literal super}.</li>
     *     <li>It is guaranteed that this method, if called, will always be
     *     called before {@link #process}.</li>
     * </ul>
     *
     * @param opList Operation list to process. Will be equal to the one passed
     *               to {@link #process}.
     * @throws IllegalArgumentException if validation fails.
     * @throws ProcessorException       if there is an error in performing the
     *                                  validation.
     */
    default void validate(OperationList opList,
                          Dimension fullSize) throws ProcessorException {
        opList.validate(fullSize);
    }

}
