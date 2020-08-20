package edu.illinois.library.cantaloupe.processor.codec.png;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.io.IOException;

public final class PNGImageReader extends AbstractIIOImageReader
        implements ImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PNGImageReader.class);

    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.png.reader";

    @Override
    public boolean canSeek() {
        return false;
    }

    @Override
    protected String[] getApplicationPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.png.PNGImageReader" };
    }

    @Override
    public Compression getCompression(int imageIndex) throws IOException {
        // Throw any contract-required exceptions.
        getSize(0);
        return Compression.DEFLATE;
    }

    @Override
    protected Format getFormat() {
        return Format.get("png");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Metadata getMetadata(int imageIndex) throws IOException {
        // Throw any contract-required exceptions.
        getSize(0);
        final IIOMetadata metadata = iioReader.getImageMetadata(imageIndex);
        final String metadataFormat = metadata.getNativeMetadataFormatName();
        return new PNGMetadata(metadata, metadataFormat);
    }

    @Override
    protected String getUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        return config.getString(IMAGEIO_PLUGIN_CONFIG_KEY);
    }

}
