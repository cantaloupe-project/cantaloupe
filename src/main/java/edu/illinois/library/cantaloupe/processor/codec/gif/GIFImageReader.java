package edu.illinois.library.cantaloupe.processor.codec.gif;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;

public final class GIFImageReader extends AbstractIIOImageReader
        implements ImageReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFImageReader.class);

    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.gif.reader";

    @Override
    protected String[] getApplicationPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.gif.GIFImageReader" };
    }

    @Override
    public Compression getCompression(int imageIndex) throws IOException {
        // Throw any contract-required exceptions.
        getSize(0);
        return Compression.LZW;
    }

    @Override
    protected Format getFormat() {
        return Format.GIF;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Metadata getMetadata(int imageIndex) throws IOException {
        // Throw any contract-required exceptions.
        getSize(0);

        // The GIFMetadata is going to read from the GIFMetadataReader which is
        // going to read from inputStream. But, this reader isn't done reading
        // from inputStream. So, call reset() to get a fresh stream...
        reset();
        GIFMetadataReader reader = new GIFMetadataReader();
        reader.setSource(inputStream);
        // Call one of GIFMetadataReader's reader methods to read from it...
        try {
            reader.getXMP();
        } catch (IOException e) {
            // If XMP-reading fails, we don't want the whole response to fail.
            LOGGER.info("getMetadata(): {}", e.getMessage());
        }
        // and then call reset() again so we can use it again.
        reset();
        // This is horrible of course, but it'll work for now.

        return new GIFMetadata(reader);
    }

    @Override
    protected String getUserPreferredIIOImplementation() {
        var config = Configuration.getInstance();
        return config.getString(IMAGEIO_PLUGIN_CONFIG_KEY);
    }

    /**
     * The default Image I/O GIF reader apparently has a bug that can cause
     * palette corruption when {@link
     * javax.imageio.ImageReadParam#setSourceRegion(Rectangle) reading a
     * region}. This override does not do that.
     *
     * {@inheritDoc}
     */
    @Override
    public BufferedImage read(final OperationList ops,
                              final ReductionFactor reductionFactor,
                              final Set<ReaderHint> hints) throws IOException {
        try {
            BufferedImage image = iioReader.read(0);

            if (image == null) {
                throw new SourceFormatException(iioReader.getFormatName());
            }

            hints.add(ReaderHint.ALREADY_ORIENTED);
            return image;
        } catch (IIOException e) {
            handle(e);
            return null;
        }
    }

}
