package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.metadata.IIOMetadata;
import java.io.File;
import java.io.IOException;

class GifImageReader extends AbstractImageReader {

    /**
     * @param sourceFile Source file to read.
     * @throws IOException
     */
    GifImageReader(File sourceFile) throws IOException {
        super(sourceFile, Format.GIF);
    }

    /**
     * @param streamSource Source of streams to read.
     * @throws IOException
     */
    GifImageReader(StreamSource streamSource) throws IOException {
        super(streamSource, Format.GIF);
    }

    Metadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = reader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new GifMetadata(metadata, metadataFormat);
    }

}
