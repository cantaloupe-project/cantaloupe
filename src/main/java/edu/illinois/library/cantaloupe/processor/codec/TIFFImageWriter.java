package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * TIFF image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * TIFFs.
 *
 * @see <a href="http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/imageio/plugins/tiff/package-summary.html">
 *     ImageIO TIFF Plugin Documentation</a>
 */
final class TIFFImageWriter extends AbstractIIOImageWriter
        implements ImageWriter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TIFFImageWriter.class);

    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.tif.writer";

    /**
     * No-op.
     *
     * @see #addMetadata(Metadata, IIOMetadata)
     */
    @Override
    void addMetadata(final IIOMetadataNode baseNode) {}

    /**
     * @param sourceMetadata
     * @param derivativeMetadata
     * @return New derivative metadata.
     */
    private IIOMetadata addMetadata(final Metadata sourceMetadata,
                                    IIOMetadata derivativeMetadata)
            throws IOException {
        // If sourceMetadata is native TIFF metadata, copy over its relevant
        // fields. Otherwise, if it contains at least an XMP packet, copy that
        // into a new field.
        if (sourceMetadata instanceof TIFFMetadata) {
            final TIFFDirectory destDir =
                    TIFFDirectory.createFromMetadata(derivativeMetadata);

            // Copy native fields.
            for (TIFFField field : ((TIFFMetadata) sourceMetadata).getNativeMetadata()) {
                destDir.addTIFFField(field);
            }

            // Copy the EXIF field.
            final TIFFField exifField = (TIFFField) sourceMetadata.getEXIF();
            if (exifField != null) {
                destDir.addTIFFField(exifField);
            }

            // Copy the IPTC field.
            final TIFFField iptcField = (TIFFField) sourceMetadata.getIPTC();
            if (iptcField != null) {
                destDir.addTIFFField(iptcField);
            }

            // Copy the XMP field.
            final TIFFField xmpField =
                    ((TIFFMetadata) sourceMetadata).getXMPField();
            if (xmpField != null) {
                destDir.addTIFFField(xmpField);
            }

            derivativeMetadata = destDir.getAsMetadata();
        } else {
            final String xmp = sourceMetadata.getXMP();
            if (xmp != null) {
                final TIFFDirectory destDir =
                        TIFFDirectory.createFromMetadata(derivativeMetadata);

                byte[] xmpBytes = xmp.getBytes(StandardCharsets.UTF_8);
                final TIFFTag xmpTag = new TIFFTag(
                        "unknown", TIFFMetadata.XMP_TAG_NUMBER, 0);
                final TIFFField xmpField = new TIFFField(
                        xmpTag, TIFFTag.TIFF_BYTE, xmpBytes.length, xmpBytes);
                destDir.addTIFFField(xmpField);

                derivativeMetadata = destDir.getAsMetadata();
            }
        }
        return derivativeMetadata;
    }

    @Override
    String[] getApplicationPreferredIIOImplementations() {
        // The GeoSolutions TIFF reader supports BigTIFF among other
        // enhancements. The Sun reader will do as a fallback, although there
        // shouldn't be any need to fall back.
        String[] impls = new String[2];
        impls[0] = it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter.class.getName();

        // Before Java 9, the Sun TIFF reader was part of the JAI ImageI/O
        // Tools. Then it was moved into the JDK.
        if (SystemUtils.getJavaMajorVersion() < 9) {
            impls[1] = "com.sun.media.imageioimpl.plugins.tiff.TIFFImageWriter";
        } else {
            impls[1] = "com.sun.imageio.plugins.tiff.TIFFImageWriter";
        }
        return impls;
    }

    /**
     * @return Compression type in the javax.codec vernacular. May return
     *         {@literal null} to indicate no equivalent or no compression.
     */
    private String getImageIOType(Compression compression) {
        switch (compression) {
            case DEFLATE:
                return "ZLib";
            case JPEG:
                return "JPEG";
            case JPEG2000:
                return "JPEG2000";
            case LZW:
                return "LZW";
            case RLE:
                return "PackBits";
            default:
                return null;
        }
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    /**
     * @param writeParam Write parameters on which to base the metadata.
     * @param image      Image to apply the metadata to.
     * @return           Image metadata with added metadata corresponding to any
     *                   writer-specific operations applied.
     */
    @Override
    IIOMetadata getMetadata(final ImageWriteParam writeParam,
                            final RenderedImage image) throws IOException {
        IIOMetadata derivativeMetadata = iioWriter.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromRenderedImage(image),
                writeParam);
        if (sourceMetadata != null) {
            derivativeMetadata = addMetadata(sourceMetadata, derivativeMetadata);
        }
        return derivativeMetadata;
    }

    @Override
    String getUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        return config.getString(IMAGEIO_PLUGIN_CONFIG_KEY);
    }

    /**
     * @return Write parameters respecting the operation list.
     */
    private ImageWriteParam getWriteParam() {
        final ImageWriteParam writeParam = iioWriter.getDefaultWriteParam();
        final Compression compression = encode.getCompression();
        if (compression != null) {
            final String type = getImageIOType(compression);
            if (type != null) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionType(type);
                LOGGER.debug("Compression type: {}", type);
            }
        }
        return writeParam;
    }

    /**
     * Writes the given image to the given output stream.
     *
     * @param image        Image to write.
     * @param outputStream Stream to write the image to.
     */
    @Override
    public void write(RenderedImage image,
                      OutputStream outputStream) throws IOException {
        final ImageWriteParam writeParam = getWriteParam();
        final IIOMetadata metadata = getMetadata(writeParam, image);
        final IIOImage iioImage = new IIOImage(image, null, metadata);

        try (ImageOutputStream os =
                     ImageIO.createImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);
            iioWriter.write(metadata, iioImage, writeParam);
            os.flush(); // http://stackoverflow.com/a/14489406
        } finally {
            iioWriter.dispose();
        }
    }

}
