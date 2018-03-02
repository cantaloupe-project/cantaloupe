package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.io.IOException;

final class GIFImageReader extends AbstractIIOImageReader
        implements ImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFImageReader.class);

    @Override
    public Compression getCompression(int imageIndex) {
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
    public Metadata getMetadata(int imageIndex) throws IOException {
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new GIFMetadata(metadata, metadataFormat);
    }

    @Override
    String[] preferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.gif.GIFImageReader" };
    }

    @Override
    public BufferedImageSequence readSequence() throws IOException {
        BufferedImageSequence seq = new BufferedImageSequence();
        for (int i = 0, count = getNumImages(); i < count; i++) {
            seq.add(iioReader.read(i));
        }
        return seq;
    }

}
