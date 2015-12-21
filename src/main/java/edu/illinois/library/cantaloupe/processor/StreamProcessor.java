package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OperationList;

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
     * <p>Performs the supplied operations on an image, reading it from the
     * supplied InputStream, and writing the result to the supplied
     * OutputStream.</p>
     *
     * <p>Operations should be applied in the order they appear in the
     * OperationList iterator. For the sake of efficiency, implementations
     * should check whether each one is a no-op
     * ({@link edu.illinois.library.cantaloupe.image.Operation#isNoOp()})
     * before performing it.</p>
     *
     * <p>Implementations should use the sourceSize parameter and not their
     * own {#link #getSize} method to avoid reusing a potentially unreusable
     * InputStream.</p>
     *
     * @param ops OperationList of the image to process.
     * @param sourceFormat Format of the source image. Will never be
     * {@link SourceFormat#UNKNOWN}.
     * @param sourceSize Scale of the source image.
     * @param inputStream Stream from which to read the image. Implementations
     *                    should not close it.
     * @param outputStream Stream to which to write the image. Implementations
     *                     should not close it.
     * @throws UnsupportedOutputFormatException
     * @throws UnsupportedSourceFormatException
     * @throws ProcessorException
     */
    void process(OperationList ops, SourceFormat sourceFormat,
                 Dimension sourceSize, InputStream inputStream,
                 OutputStream outputStream) throws ProcessorException;

}
