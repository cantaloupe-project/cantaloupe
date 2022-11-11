package edu.illinois.library.cantaloupe.processor.codec.jpeg2000;

import de.digitalcollections.openjpeg.imageio.OpenJp2ImageReader;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.*;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.source.stream.BufferedImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import java.io.IOException;

public final class JPEG2000OpenJpegImageReader extends AbstractIIOImageReader
        implements ImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEG2000OpenJpegImageReader.class);

    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.jp2.reader";

    @Override
    public boolean canSeek() {
        return true;
    }

    @Override
    protected String[] getApplicationPreferredIIOImplementations() {
        return new String[] { "de.digitalcollections.openjpeg.imageio.OpenJp2ImageReader" };
    }

    @Override
    public Compression getCompression(int imageIndex) throws IOException {
        // Throw any contract-required exceptions.
        return Compression.UNCOMPRESSED;
    }

    @Override
    protected Format getFormat() {
        return Format.get("jp2");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Metadata getMetadata(int imageIndex) throws IOException {
        return new JPEG2000Metadata(iioReader.getImageMetadata(imageIndex));
    }

    @Override
    protected String getUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        return config.getString(IMAGEIO_PLUGIN_CONFIG_KEY);
    }

}
