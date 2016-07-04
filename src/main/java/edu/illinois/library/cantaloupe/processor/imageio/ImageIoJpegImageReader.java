package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.metadata.IIOMetadata;
import java.io.File;
import java.io.IOException;

class ImageIoJpegImageReader extends AbstractImageIoImageReader {

    /**
     * @param sourceFile Source file to read.
     * @throws IOException
     */
    ImageIoJpegImageReader(File sourceFile) throws IOException {
        super(sourceFile, Format.JPG);
    }

    /**
     * @param streamSource Source of streams to read.
     * @throws IOException
     */
    ImageIoJpegImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.JPG);
    }

    ImageIoMetadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = reader.getImageMetadata(imageIndex);
        final String metadataFormat = reader.getImageMetadata(imageIndex).
                getNativeMetadataFormatName();
        return new ImageIoJpegMetadata(metadata, metadataFormat);
    }

}
