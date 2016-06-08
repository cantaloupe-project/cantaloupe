package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.metadata.IIOMetadata;
import java.io.File;
import java.io.IOException;

class ImageIoPngImageReader extends AbstractImageIoImageReader {

    /**
     * @param sourceFile Source file to read.
     * @throws IOException
     */
    ImageIoPngImageReader(File sourceFile) throws IOException {
        super(sourceFile, Format.PNG);
    }

    /**
     * @param streamSource Source of streams to read.
     * @throws IOException
     */
    ImageIoPngImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.PNG);
    }

    ImageIoMetadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = reader.getImageMetadata(imageIndex);
        final String metadataFormat = reader.getImageMetadata(imageIndex).
                getNativeMetadataFormatName();
        return new ImageIoPngMetadata(metadata, metadataFormat);
    }

}
