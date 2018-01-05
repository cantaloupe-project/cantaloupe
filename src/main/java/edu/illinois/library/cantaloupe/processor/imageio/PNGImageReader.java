package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.io.IOException;
import java.nio.file.Path;

final class PNGImageReader extends AbstractImageReader {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(PNGImageReader.class);

    /**
     * @param sourceFile Source file to read.
     */
    PNGImageReader(Path sourceFile) throws IOException {
        super(sourceFile, Format.PNG);
    }

    /**
     * @param streamSource Source of streams to read.
     */
    PNGImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.PNG);
    }

    @Override
    Compression getCompression(int imageIndex) {
        return Compression.DEFLATE;
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

}
