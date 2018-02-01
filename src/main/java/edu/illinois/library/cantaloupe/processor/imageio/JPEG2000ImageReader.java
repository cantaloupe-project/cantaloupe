package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

final class JPEG2000ImageReader extends AbstractImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEG2000ImageReader.class);

    /**
     * @param sourceFile Source file to read.
     */
    JPEG2000ImageReader(Path sourceFile) throws IOException {
        super(sourceFile, Format.JP2);
    }

    /**
     * @param inputStream Stream to read.
     */
    JPEG2000ImageReader(ImageInputStream inputStream) throws IOException {
        super(inputStream, Format.JP2);
    }

    /**
     * @param streamSource Source of streams to read.
     */
    JPEG2000ImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.JP2);
    }

    @Override
    Compression getCompression(int imageIndex) {
        return Compression.JPEG2000;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    Metadata getMetadata(int imageIndex) {
        return new NullMetadata();
    }

    /**
     * @throws UnsupportedOperationException Always.
     */
    @Override
    BufferedImage read() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException Always.
     */
    @Override
    BufferedImage read(OperationList ops,
                       Orientation orientation,
                       ReductionFactor reductionFactor,
                       Set<ImageReader.Hint> hints) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException Always.
     */
    @Override
    public RenderedImage readRendered(OperationList ops,
                                      Orientation orientation,
                                      ReductionFactor reductionFactor,
                                      Set<ImageReader.Hint> hints) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException Always.
     */
    @Override
    BufferedImageSequence readSequence() {
        throw new UnsupportedOperationException();
    }

}
