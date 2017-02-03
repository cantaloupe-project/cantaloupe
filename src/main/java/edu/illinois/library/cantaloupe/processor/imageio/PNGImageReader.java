package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.metadata.IIOMetadata;
import java.io.File;
import java.io.IOException;

class PNGImageReader extends AbstractImageReader {

    /**
     * @param sourceFile Source file to read.
     * @throws IOException
     */
    PNGImageReader(File sourceFile) throws IOException {
        super(sourceFile, Format.PNG);
    }

    /**
     * @param streamSource Source of streams to read.
     * @throws IOException
     */
    PNGImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.PNG);
    }

    @Override
    Compression getCompression(int imageIndex) throws IOException {
        return Compression.ZLIB;
    }

    Metadata getMetadata(int imageIndex) throws IOException {
        if (iioReader == null) {
            createReader();
        }
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new PNGMetadata(metadata, metadataFormat);
    }

}
