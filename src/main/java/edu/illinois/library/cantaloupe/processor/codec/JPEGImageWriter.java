package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.processor.Java2DUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;

/**
 * JPEG image writer using ImageIO, capable of writing both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s as JPEGs.
 */
final class JPEGImageWriter extends AbstractIIOImageWriter
        implements ImageWriter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEGImageWriter.class);

    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.jpg.writer";

    /**
     * @param baseTree Tree to embed the metadata into.
     */
    @Override
    protected void addMetadata(final IIOMetadataNode baseTree) {
        if (sourceMetadata instanceof JPEGMetadata) {
            final Object exif = sourceMetadata.getEXIF();
            if (exif != null) {
                // Create the EXIF node.
                final IIOMetadataNode node = new IIOMetadataNode("unknown");
                node.setAttribute("MarkerTag", "225");
                node.setUserObject(exif);
                // Append it to /markerSequence/unknown[@MarkerTag=225]
                final Node markerSequence =
                        baseTree.getElementsByTagName("markerSequence").item(0);
                markerSequence.insertBefore(node, markerSequence.getFirstChild());
            }

            final Object iptc = sourceMetadata.getIPTC();
            if (iptc != null) {
                // Create the IPTC node.
                final IIOMetadataNode node = new IIOMetadataNode("unknown");
                node.setAttribute("MarkerTag", "237");
                node.setUserObject(iptc);
                // Append it to /markerSequence/unknown[@MarkerTag=237]
                final Node markerSequence =
                        baseTree.getElementsByTagName("markerSequence").item(0);
                markerSequence.insertBefore(node, markerSequence.getFirstChild());
            }

            final byte[] xmp = sourceMetadata.getXMP();
            if (xmp != null) {
                // Create the XMP node.
                final IIOMetadataNode node = new IIOMetadataNode("unknown");
                node.setAttribute("MarkerTag", "225");
                node.setUserObject(xmp);
                // Append it to /markerSequence/unknown[@MarkerTag=225]
                final Node markerSequence =
                        baseTree.getElementsByTagName("markerSequence").item(0);
                markerSequence.insertBefore(node, markerSequence.getFirstChild());
            }
        }
    }

    @Override
    String[] getApplicationPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.jpeg.JPEGImageWriter" };
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    String getUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        return config.getString(IMAGEIO_PLUGIN_CONFIG_KEY);
    }

    private ImageWriteParam getWriteParam() {
        final ImageWriteParam writeParam = iioWriter.getDefaultWriteParam();
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionType("JPEG");

        // Quality
        final int quality = encode.getQuality();
        writeParam.setCompressionQuality(quality * 0.01f);

        // Interlacing
        final boolean interlace = encode.isInterlacing();
        writeParam.setProgressiveMode(interlace ?
                ImageWriteParam.MODE_DEFAULT : ImageWriteParam.MODE_DISABLED);

        LOGGER.debug("Quality: {}; progressive: {}", quality, interlace);

        return writeParam;
    }

    /**
     * Removes the alpha channel from the given image, taking the return value
     * of the operation list's {@link Encode#getBackgroundColor()} method into
     * account, if available.
     *
     * @param image Image to remove alpha from.
     * @return      Flattened image.
     */
    private BufferedImage removeAlpha(BufferedImage image) {
        boolean haveBGColor = false;

        Color bgColor = encode.getBackgroundColor();
        if (bgColor != null) {
            haveBGColor = true;
            image = Java2DUtil.removeAlpha(image, bgColor);
        }

        if (!haveBGColor) {
            image = Java2DUtil.removeAlpha(image);
        }
        return image;
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
        if (image instanceof BufferedImage) {
            write((BufferedImage) image, outputStream);
        } else if (image instanceof PlanarImage) {
            write((PlanarImage) image, outputStream);
        } else {
            throw new IllegalArgumentException(
                    "image must be either a BufferedImage or PlanarImage.");
        }
    }

    /**
     * Writes a Java 2D {@link BufferedImage} to the given output stream.
     *
     * @param image        Image to write
     * @param outputStream Stream to write the image to
     */
    private void write(BufferedImage image,
                       OutputStream outputStream) throws IOException {
        // JPEG doesn't support alpha, so convert to RGB or else the
        // client will interpret as CMYK
        image = removeAlpha(image);
        final ImageWriteParam writeParam = getWriteParam();
        final IIOMetadata metadata = getMetadata(writeParam, image);
        final IIOImage iioImage = new IIOImage(image, null, metadata);

        try (ImageOutputStream os =
                     ImageIO.createImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);
            iioWriter.write(null, iioImage, writeParam);
        } finally {
            iioWriter.dispose();
        }
    }

    /**
     * Writes a JAI {@link PlanarImage} to the given output stream.
     *
     * @param image        Image to write
     * @param outputStream Stream to write the image to
     */
    @SuppressWarnings("deprecation")
    private void write(PlanarImage image,
                       OutputStream outputStream) throws IOException {
        if (!(image.getColorModel() instanceof IndexColorModel)) {
            // JPEGImageWriter will interpret a >3-band image as CMYK.
            // So, select only the first 3 bands.
            if (OpImage.getExpandedNumBands(image.getSampleModel(),
                    image.getColorModel()) > 3) {
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(image);
                final int[] bands = {0, 1, 2};
                pb.add(bands);
                image = JAI.create("bandselect", pb, null);
            }
        }
        final ImageWriteParam writeParam = getWriteParam();
        final IIOMetadata metadata = getMetadata(writeParam, image);
        // JPEGImageWriter doesn't like RenderedOps, so give it a
        // BufferedImage.
        final IIOImage iioImage = new IIOImage(
                image.getAsBufferedImage(), null, metadata);

        try (ImageOutputStream os =
                     ImageIO.createImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);
            iioWriter.write(null, iioImage, writeParam);
        } finally {
            iioWriter.dispose();
        }

    }

}
