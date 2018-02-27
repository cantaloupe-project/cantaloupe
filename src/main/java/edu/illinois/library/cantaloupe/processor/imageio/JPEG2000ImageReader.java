package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Set;

final class JPEG2000ImageReader extends AbstractImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEG2000ImageReader.class);

    @Override
    Compression getCompression(int imageIndex) {
        return Compression.JPEG2000;
    }

    @Override
    Format getFormat() {
        return Format.JP2;
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
