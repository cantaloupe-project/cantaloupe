package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.SourceFormat;

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
     * @return Scale of the source image in pixels.
     * @throws ProcessorException
     */
    Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException;

    /**
     * <p>Performs the supplied operations on an image, reading it from the
     * supplied file, and writing the result to the supplied OutputStream.</p>
     *
     * <p>Operations should be applied in the order they are set in the
     * Operations object. Implementations should check whether each option is
     * a no-op ({@link Operation#isNoOp()}) before performing it, for the sake
     * of efficiency.</p>
     *
     * <p>Implementations should use the sourceSize parameter and not their
     * own <code>getSize()</code> method to avoid reusing a potentially
     * unreusable InputStream.</p>
     *
     * @param ops Operations of the output image
     * @param sourceFormat Format of the source image
     * @param inputFile File from which to read the image. Implementations
     *                  should not close it.
     * @param outputStream Stream to which to write the image. Implementations
     *                     should not close it.
     * @throws UnsupportedOutputFormatException
     * @throws UnsupportedSourceFormatException
     * @throws ProcessorException
     */
    void process(Operations ops, SourceFormat sourceFormat,
                 Dimension sourceSize, File inputFile,
                 OutputStream outputStream) throws ProcessorException;

}
