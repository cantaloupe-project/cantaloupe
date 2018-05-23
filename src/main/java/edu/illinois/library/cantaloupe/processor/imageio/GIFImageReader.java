package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

final class GIFImageReader extends AbstractImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFImageReader.class);

    /**
     * @param sourceFile Source file to read.
     */
    GIFImageReader(Path sourceFile) throws IOException {
        super(sourceFile, Format.GIF);
    }

    /**
     * @param streamSource Source of streams to read.
     */
    GIFImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.GIF);
    }

    @Override
    Compression getCompression(int imageIndex) {
        return Compression.LZW;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    Metadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new GIFMetadata(metadata, metadataFormat);
    }

    @Override
    String[] preferredIIOImplementations() {
        // We don't want com.sun.media.imageioimpl.plugins.gif.GIFImageReader!
        return new String[] { "com.sun.imageio.plugins.gif.GIFImageReader" };
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
    BufferedImage read(final OperationList ops,
                       final Orientation orientation,
                       final ReductionFactor reductionFactor,
                       final Set<ImageReader.Hint> hints)
            throws IOException, ProcessorException {
        BufferedImage image = iioReader.read(0);

        if (image == null) {
            throw new UnsupportedSourceFormatException(iioReader.getFormatName());
        }

        return image;
    }

}
