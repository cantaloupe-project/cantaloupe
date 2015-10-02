package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.Parameters;

import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.OutputStream;

/**
 * Interface to be implemented by image processors that support input via
 * streams.
 */
public interface StreamProcessor extends Processor {

    /**
     * @param inputStream Source image. Should not be closed.
     * @param sourceFormat Format of the source image
     * @return Size of the source image in pixels.
     * @throws ProcessorException
     */
    Dimension getSize(ImageInputStream inputStream, SourceFormat sourceFormat)
            throws ProcessorException;

    /**
     * Uses the supplied parameters to process an image from the supplied
     * ImageInputStream, and writes the result to the given OutputStream.
     *
     * @param params Parameters of the output image
     * @param sourceFormat Format of the source image
     * @param inputStream Stream from which to read the image. Implementations
     *                    should not close it.
     * @param outputStream Stream to which to write the image. Implementations
     *                     should not close it.
     * @throws UnsupportedOutputFormatException
     * @throws UnsupportedSourceFormatException
     * @throws ProcessorException
     */
    void process(Parameters params, SourceFormat sourceFormat,
                 ImageInputStream inputStream, OutputStream outputStream)
            throws ProcessorException;

}
