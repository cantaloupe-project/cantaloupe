package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.io.IOException;

final class GIFImageReader extends AbstractImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFImageReader.class);

    @Override
    Compression getCompression(int imageIndex) {
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

}
