package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Operations;

import java.awt.Dimension;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface to be implemented by image processors that support input via
 * streams.
 */
public interface StreamProcessor extends Processor {

    /**
     * @param inputStream Source image. Implementations should not close it.
     * @param sourceFormat Format of the source image
     * @return Scale of the source image in pixels.
     * @throws ProcessorException
     */
    Dimension getSize(InputStream inputStream, SourceFormat sourceFormat)
            throws ProcessorException;

    /**
     * <p>Uses the supplied parameters to process an image from the supplied
     * InputStream, and writes the result to the given OutputStream.</p>
     *
     * <p>Implementations should use the sourceSize parameter and not their
     * own <code>getSize()</code> method to avoid reusing a potentially
     * unreusable InputStream.</p>
     *
     * @param params Operations of the output image
     * @param sourceFormat Format of the source image
     * @param sourceSize Scale of the source image
     * @param inputStream Stream from which to read the image. Implementations
     *                    should not close it.
     * @param outputStream Stream to which to write the image. Implementations
     *                     should not close it.
     * @throws UnsupportedOutputFormatException
     * @throws UnsupportedSourceFormatException
     * @throws ProcessorException
     */
    void process(Operations params, SourceFormat sourceFormat,
                 Dimension sourceSize, InputStream inputStream,
                 OutputStream outputStream) throws ProcessorException;

}
