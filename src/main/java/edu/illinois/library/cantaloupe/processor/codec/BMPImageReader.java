package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BMPImageReader extends AbstractIIOImageReader
        implements ImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BMPImageReader.class);

    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.bmp.reader";

    @Override
    String[] getApplicationPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.bmp.BMPImageReader" };
    }

    @Override
    public Compression getCompression(int imageIndex) {
        return Compression.UNCOMPRESSED;
    }

    @Override
    Format getFormat() {
        return Format.BMP;
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Metadata getMetadata(int imageIndex) {
        return new BeanMetadata();
    }

    @Override
    String getUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        return config.getString(IMAGEIO_PLUGIN_CONFIG_KEY);
    }

}
