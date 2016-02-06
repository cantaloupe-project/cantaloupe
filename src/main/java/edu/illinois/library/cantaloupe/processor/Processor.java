package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;

import java.awt.Dimension;
import java.io.OutputStream;
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
public interface Processor {

    /**
     * @return Output formats available for the set source format, or an
     * empty set if none.
     */
    Set<OutputFormat> getAvailableOutputFormats();

    /**
     * @return Pixel dimensions of the source image.
     * @throws ProcessorException
     */
    Dimension getSize() throws ProcessorException;

    /**
     * @return The source format of the image to be processed.
     */
    SourceFormat getSourceFormat();

    /**
     * @return All features supported by the processor for the set source
     * format.
     */
    Set<ProcessorFeature> getSupportedFeatures();

    /**
     * @return All qualities supported by the processor for the set source
     * format.
     */
    Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIiif1_1Qualities();

    /**
     * @return All qualities supported by the processor for the set source
     * format.
     */
    Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities();

    /**
     * @return List of tile sizes of each resolution of a multiresolution
     * image, in order of image index. For monoresolution images, this
     * should be a one-element list. If the image is not tiled, the size
     * should be the full image size.
     * @throws ProcessorException
     */
    List<Dimension> getTileSizes() throws ProcessorException;

    /**
     * <p>Performs the supplied operations on an image, reading it from the
     * supplied stream, and writing the result to the supplied stream.</p>
     *
     * <p>Operations should be applied in the order they appear in the
     * OperationList iterator. For the sake of efficiency, implementations
     * should check whether each one is a no-op
     * ({@link edu.illinois.library.cantaloupe.image.Operation#isNoOp()})
     * before performing it.</p>
     *
     * <p>Implementations should get the full size of the source image from
     * the sourceSize parameter instead of their {#link #getSize} method,
     * for efficiency's sake.</p>
     *
     * @param ops OperationList of the image to process.
     * @param sourceSize Size of the source image.
     * @param outputStream Stream to write the image to.
     *                     Implementations should not close it.
     * @throws UnsupportedOutputFormatException
     * @throws ProcessorException
     */
    void process(OperationList ops, Dimension sourceSize,
                 OutputStream outputStream) throws ProcessorException;

    /**
     * @param sourceFormat Format of the source image. Will never be
     *                     {@link SourceFormat#UNKNOWN}.
     * @throws UnsupportedSourceFormatException
     */
    void setSourceFormat(SourceFormat sourceFormat)
            throws UnsupportedSourceFormatException;

}
