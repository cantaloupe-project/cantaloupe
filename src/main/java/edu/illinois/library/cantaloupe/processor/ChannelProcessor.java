package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.resolver.ChannelSource;

import java.awt.Dimension;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Interface to be implemented by image processors that support input via
 * channels.
 */
public interface ChannelProcessor extends Processor {

    /**
     * @param readableChannel Channel for reading the source image.
     *                        Implementations should close it.
     * @param sourceFormat Format of the source image
     * @return Scale of the source image in pixels.
     * @throws ProcessorException
     */
    Dimension getSize(ReadableByteChannel readableChannel,
                      SourceFormat sourceFormat) throws ProcessorException;

    /**
     * <p>Performs the supplied operations on an image, reading it from the
     * supplied channel, and writing the result to the supplied channel.</p>
     *
     * <p>Operations should be applied in the order they appear in the
     * OperationList iterator. For the sake of efficiency, implementations
     * should check whether each one is a no-op
     * ({@link edu.illinois.library.cantaloupe.image.Operation#isNoOp()})
     * before performing it.</p>
     *
     * <p>Implementations should get the full size of the source image from
     * the sourceSize parameter instead of their {#link #getSize} method,
     * for efficiency.</p>
     *
     * @param ops OperationList of the image to process.
     * @param sourceFormat Format of the source image. Will never be
     * {@link SourceFormat#UNKNOWN}.
     * @param sourceSize Scale of the source image.
     * @param channelSource Source for acquiring channels from which to read
     *                      the imagee.
     * @param writableChannel Writable channel to write the image to.
     *                        Implementations should not close it.
     * @throws UnsupportedOutputFormatException
     * @throws UnsupportedSourceFormatException
     * @throws ProcessorException
     */
    void process(OperationList ops, SourceFormat sourceFormat,
                 Dimension sourceSize, ChannelSource channelSource,
                 WritableByteChannel writableChannel) throws ProcessorException;

}
