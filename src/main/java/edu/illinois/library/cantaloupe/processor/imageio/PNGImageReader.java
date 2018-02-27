package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.io.IOException;

final class PNGImageReader extends AbstractImageReader {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(PNGImageReader.class);

    static String[] getPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.png.PNGImageReader" };
    }

    @Override
    Compression getCompression(int imageIndex) {
        return Compression.DEFLATE;
    }

    @Override
    Format getFormat() {
        return Format.PNG;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    Metadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new PNGMetadata(metadata, metadataFormat);
    }

    @Override
    String[] preferredIIOImplementations() {
        return getPreferredIIOImplementations();
    }

}
