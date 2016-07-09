package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.metadata.IIOMetadata;
import java.io.File;
import java.io.IOException;

class BmpImageReader extends AbstractImageReader {

    /**
     * @param sourceFile Source file to read.
     * @throws IOException
     */
    BmpImageReader(File sourceFile) throws IOException {
        super(sourceFile, Format.BMP);
    }

    /**
     * @param streamSource Source of streams to read.
     * @throws IOException
     */
    BmpImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.BMP);
    }

    Metadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = reader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new BmpMetadata(metadata, metadataFormat);
    }

}
