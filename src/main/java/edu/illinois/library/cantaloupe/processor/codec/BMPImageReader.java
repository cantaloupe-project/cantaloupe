package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.io.IOException;

final class BMPImageReader extends AbstractIIOImageReader
        implements ImageReader {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(BMPImageReader.class);

    @Override
    public Compression getCompression(int imageIndex) {
        return Compression.UNCOMPRESSED;
    }

    @Override
    Format getFormat() {
        return Format.BMP;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Metadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new NullMetadata(metadata, metadataFormat);
    }

    @Override
    String[] preferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.bmp.BMPImageReader" };
    }

}
