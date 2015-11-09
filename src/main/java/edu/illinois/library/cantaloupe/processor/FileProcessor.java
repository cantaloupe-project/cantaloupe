package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.Parameters;

import java.awt.Dimension;
import java.io.File;
import java.io.OutputStream;

/**
 * Interface to be implemented by processors that support input via direct
 * file access.
 */
public interface FileProcessor extends Processor {

    /**
     * @param inputFile Source image
     * @param sourceFormat Format of the source image
     * @return Size of the source image in pixels.
     * @throws ProcessorException
     */
    Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException;

    /**
     * Uses the supplied parameters to process an image from the supplied File,
     * and writes the result to the given OutputStream.
     *
     * <p>Implementations should use the sourceSize parameter and not their
     * own <code>getSize()</code> method to avoid reusing a potentially
     * unreusable InputStream.</p>
     *
     * @param params Parameters of the output image
     * @param sourceFormat Format of the source image
     * @param inputFile File from which to read the image. Implementations
     *                  should not close it.
     * @param outputStream Stream to which to write the image. Implementations
     *                     should not close it.
     * @throws UnsupportedOutputFormatException
     * @throws UnsupportedSourceFormatException
     * @throws ProcessorException
     */
    void process(Parameters params, SourceFormat sourceFormat,
                 Dimension sourceSize, File inputFile,
                 OutputStream outputStream) throws ProcessorException;

}
