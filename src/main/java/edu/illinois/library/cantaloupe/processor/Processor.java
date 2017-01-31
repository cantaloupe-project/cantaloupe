package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;

import java.awt.Dimension;
import java.io.File;
import java.io.OutputStream;
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
public interface Processor {

    String BACKGROUND_COLOR_CONFIG_KEY = "processor.background_color";
    String DOWNSCALE_FILTER_CONFIG_KEY = "processor.downscale_filter";
    String JPG_QUALITY_CONFIG_KEY = "processor.jpg.quality";
    String NORMALIZE_CONFIG_KEY = "processor.normalize";
    String PRESERVE_METADATA_CONFIG_KEY = "metadata.preserve";
    String RESPECT_ORIENTATION_CONFIG_KEY = "metadata.respect_orientation";
    String SHARPEN_CONFIG_KEY = "processor.sharpen";
    String TIF_COMPRESSION_CONFIG_KEY = "processor.tif.compression";
    String UPSCALE_FILTER_CONFIG_KEY = "processor.upscale_filter";

    /**
     * @return Output formats available for the set source format, or an
     *         empty set if none.
     */
    Set<Format> getAvailableOutputFormats();

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
    getSupportedIiif1_1Qualities();

    /**
     * @return All qualities supported by the processor for the set source
     *         format.
     */
    Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities();

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
     *     <ul>
     *         <li>If {@link FileProcessor#setSourceFile(File)} has been
     *         called, but not
     *         {@link StreamProcessor#setStreamSource(StreamSource)},
     *         implementations should read from a file.</li>
     *         <li>If vice versa, or if both have been called, implementations
     *         should read from a stream.</li>
     *     </ul>
     *     </li>
     *     <li>Operations should be applied in the order they are iterated.
     *     For the sake of efficiency, implementations should check whether
     *     each one is a no-op using
     *     {@link edu.illinois.library.cantaloupe.operation.Operation#hasEffect(Dimension, OperationList)}
     *     before performing it.</li>
     *     <li>The OperationList will be in a frozen (immutable) state.
     *     Implementations are discouraged from performing their own operations
     *     separate from the ones in the list, as this could interfere with the
     *     caching architecture.</li>
     *     <li>In addition to operations, the operation list may contain a
     *     number of options accessible via its
     *     {@link OperationList#getOptions()} method, which implementations
     *     should respect, where applicable. These typically come from the
     *     configuration, so implementations should not try to read the
     *     configuration themselves, except to get their own processor-specific
     *     info.</li>
     * </ul>
     *
     * @param opList Operation list to process. As it will be equal to the one
     *               passed to {@link #validate}, there is no need to validate
     *               it again.
     * @param sourceInfo Information about the source image. This will be equal
     *                   to the return value of {@link #readImageInfo}, but it
     *                   might not be the same instance, as it may have come
     *                   from a cache.
     * @param outputStream Stream to write the image to, which should not be
     *                     closed.
     * @throws UnsupportedOutputFormatException Implementations can extend
     *                                          {@link AbstractProcessor} and
     *                                          call super to get this check for
     *                                          free.
     * @throws ProcessorException If anything goes wrong.
     */
    void process(OperationList opList, Info sourceInfo,
                 OutputStream outputStream) throws ProcessorException;

    /**
     * <p>Reads and returns information about the source image.</p>
     *
     * @return Information about the source image.
     * @throws ProcessorException If anything goes wrong.
     */
    Info readImageInfo() throws ProcessorException;

    /**
     * @param format Format of the source image. Will never be
     *               {@link Format#UNKNOWN}.
     * @throws UnsupportedSourceFormatException
     */
    void setSourceFormat(Format format) throws UnsupportedSourceFormatException;

    /**
     * <p>Validates the given operation list, throwing an
     * {@link IllegalArgumentException} if invalid.</p>
     *
     * <p>This default implementation does nothing.</p>
     *
     * <p>Notes:</p>
     *
     * <ul>
     *     <li>This method is mainly for validating processor-specific options
     *     in the list's options map. There is typically no need to validate
     *     the operations themselves, as this will have already been done by
     *     the endpoints. Most implementations will therefore not need to do
     *     much, if anything.</li>
     *     <li>It is guaranteed that this method, if called, will always be
     *     called before {@link #process}.</li>
     * </ul>
     *
     * @param opList OperationList to process. Will be equal to the one passed
     *               to {@link #process}.
     * @throws IllegalArgumentException If validation fails.
     * @throws ProcessorException       If there is some issue performing the
     *                                  validation.
     */
    default void validate(OperationList opList)
            throws IllegalArgumentException, ProcessorException {}

}
