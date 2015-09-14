package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;

import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.OutputStream;
import java.util.Set;

/**
 * Interface to be implemented by all image processors.
 */
public interface Processor {

    /**
     * @param sourceFormat The source format for which to get a list of
     *                     available output formats.
     * @return Output formats available for the given source format.
     */
    Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat);

    /**
     * @param inputStream Source image
     * @param sourceFormat Format of the source image
     * @return Size of the source image in pixels.
     * @throws Exception
     */
    Dimension getSize(ImageInputStream inputStream, SourceFormat sourceFormat)
            throws Exception;

    /**
     * @param sourceFormat
     * @return All features supported by the processor for the given source
     * format.
     */
    Set<ProcessorFeature> getSupportedFeatures(SourceFormat sourceFormat);

    /**
     * @param sourceFormat
     * @return All qualities supported by the processor for the given source
     * format.
     */
    Set<Quality> getSupportedQualities(SourceFormat sourceFormat);

    /**
     * Uses the supplied parameters to process an image from the supplied
     * ImageInputStream, and writes the result to the given OutputStream.
     *
     * @param params Parameters of the output image
     * @param sourceFormat Format of the source image
     * @param inputStream Stream from which to read the image
     * @param outputStream Stream to which to write the image
     * @throws Exception
     */
    void process(Parameters params, SourceFormat sourceFormat,
                 ImageInputStream inputStream, OutputStream outputStream)
            throws Exception;

}
