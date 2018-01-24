package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Path;

final class BMPImageReader extends AbstractImageReader {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(BMPImageReader.class);

    /**
     * @param sourceFile Source file to read.
     */
    BMPImageReader(Path sourceFile) throws IOException {
        super(sourceFile, Format.BMP);
    }

    /**
     * @param inputStream Stream to read.
     */
    BMPImageReader(ImageInputStream inputStream) throws IOException {
        super(inputStream, Format.BMP);
    }

    /**
     * @param streamSource Source of streams to read.
     */
    BMPImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.BMP);
    }

    @Override
    Compression getCompression(int imageIndex) {
        return Compression.UNCOMPRESSED;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    Metadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new NullMetadata(metadata, metadataFormat);
    }

}
