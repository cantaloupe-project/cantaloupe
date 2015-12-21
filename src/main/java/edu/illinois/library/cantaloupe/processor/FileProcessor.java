package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.OperationList;
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
     * supplied File, and writing the result to the supplied OutputStream.</p>
     *
     * <p>Operations should be applied in the order they appear in the
     * OperationList iterator. For the sake of efficiency, implementations
     * should check whether each one is a no-op
     * ({@link edu.illinois.library.cantaloupe.image.Operation#isNoOp()})
     * before performing it.</p>
     *
     * <p>Implementations should use the sourceSize parameter and not their
     * own {#link #getScale} method to avoid reusing a potentially unreusable
     * InputStream.</p>
     *
     * @param ops OperationList of the output image
     * @param sourceFormat Format of the source image. Will never be
     * {@link SourceFormat#UNKNOWN}.
     * @param inputFile File from which to read the image. Implementations
     *                  should not close it.
     * @param outputStream Stream to which to write the image. Implementations
     *                     should not close it.
     * @throws UnsupportedOutputFormatException
     * @throws UnsupportedSourceFormatException
     * @throws ProcessorException
     */
    void process(OperationList ops, SourceFormat sourceFormat,
                 Dimension sourceSize, File inputFile,
                 OutputStream outputStream) throws ProcessorException;

}
