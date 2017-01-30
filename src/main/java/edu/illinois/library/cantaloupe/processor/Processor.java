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

    String BACKGROUND_COLOR_CONFIG_KEY = "rrocessor.background_color";
    String DOWNSCALE_FILTER_CONFIG_KEY = "processor.downscale_filter";
    String PRESERVE_METADATA_CONFIG_KEY = "metadata.preserve";
    String RESPECT_ORIENTATION_CONFIG_KEY = "metadata.respect_orientation";
    String SHARPEN_CONFIG_KEY = "processor.sharpen";
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
     *         <li>If vice versa, implementations should read from a
     *         stream.</li>
     *         <li>If both have been called, implementations can read from
     *         either. If one or the other would be more efficient, they
     *         should lean toward that.</li>
     *     </ul>
     *     </li>
     *     <li>Operations should be applied in the order they occur in the
     *     OperationList's iterator. For the sake of efficiency, implementations
     *     should check whether each one is a no-op using
     *     {@link edu.illinois.library.cantaloupe.operation.Operation#hasEffect(Dimension, OperationList)}
     *     before performing it.</li>
     *     <li>The OperationList will be in a frozen (immutable) state.
     *     Implementations are discouraged from performing their own operations
     *     separate from the ones in the list, as this could interfere with the
     *     caching architecture.</li>
     *     <li>Implementations should get the full size of the source image from
     *     the {@link Info} argument and not their {#link #readImageInfo}
     *     method, for efficiency's sake.</li>
     * </ul>
     *
     * @param ops OperationList of the image to process.
     * @param sourceInfo Information about the source image.
     * @param outputStream Stream to write the image to.
     *                     Implementations should not close it.
     * @throws UnsupportedOutputFormatException
     * @throws ProcessorException
     */
    void process(OperationList ops, Info sourceInfo,
                 OutputStream outputStream) throws ProcessorException;

    /**
     * <p>Reads and returns information about the source image.</p>
     *
     * @return Information about the source image.
     * @throws ProcessorException
     */
    Info readImageInfo() throws ProcessorException;

    /**
     * @param format Format of the source image. Will never be
     *               {@link Format#UNKNOWN}.
     * @throws UnsupportedSourceFormatException
     */
    void setSourceFormat(Format format) throws UnsupportedSourceFormatException;

}
