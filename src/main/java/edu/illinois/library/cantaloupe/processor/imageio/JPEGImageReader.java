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

final class JPEGImageReader extends AbstractImageReader {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(JPEGImageReader.class);

    /**
     * @param sourceFile Source file to read.
     */
    JPEGImageReader(Path sourceFile) throws IOException {
        super(sourceFile, Format.JPG);
    }

    /**
     * @param inputStream Stream to read.
     */
    JPEGImageReader(ImageInputStream inputStream) throws IOException {
        super(inputStream, Format.JPG);
    }

    /**
     * @param streamSource Source of streams to read.
     */
    JPEGImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.JPG);
    }

    @Override
    Compression getCompression(int imageIndex) {
        return Compression.JPEG;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    Metadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new JPEGMetadata(metadata, metadataFormat);
    }

    @Override
    String[] preferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.jpeg.JPEGImageReader" };
    }

}
