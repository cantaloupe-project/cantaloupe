package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;

final class GIFImageReader extends AbstractIIOImageReader
        implements ImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFImageReader.class);

    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.gif.reader";

    @Override
    String[] getApplicationPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.gif.GIFImageReader" };
    }

    @Override
    public Compression getCompression(int imageIndex) {
        return Compression.LZW;
    }

    @Override
    Format getFormat() {
        return Format.GIF;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Metadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new GIFMetadata(metadata, metadataFormat);
    }

    @Override
    String getUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        return config.getString(IMAGEIO_PLUGIN_CONFIG_KEY);
    }

    /**
     * The default Image I/O GIF reader apparently has a bug that can cause
     * palette corruption when {@link
     * javax.imageio.ImageReadParam#setSourceRegion(Rectangle) reading a
     * region}. This override does not do that.
     *
     * {@inheritDoc}
     */
    @Override
    public BufferedImage read(final OperationList ops,
                              final Orientation orientation,
                              final ReductionFactor reductionFactor,
                              final Set<ReaderHint> hints) throws IOException {
        BufferedImage image = iioReader.read(0);

        if (image == null) {
            throw new UnsupportedSourceFormatException(iioReader.getFormatName());
        }

        return image;
    }

    @Override
    public BufferedImageSequence readSequence() throws IOException {
        BufferedImageSequence seq = new BufferedImageSequence();
        for (int i = 0, count = getNumImages(); i < count; i++) {
            seq.add(iioReader.read(i));
        }
        return seq;
    }

}
