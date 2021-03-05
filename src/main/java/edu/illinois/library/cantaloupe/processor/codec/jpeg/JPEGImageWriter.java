package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.processor.Java2DUtil;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * JPEG image writer using ImageIO, capable of writing both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s as JPEGs.
 */
public final class JPEGImageWriter extends AbstractIIOImageWriter
        implements ImageWriter {

    /**
     * Wraps an {@link OutputStream}, injecting an {@literal APP1} segment
     * into it at the appropriate position if there is any metadata to write.
     * This works around an apparent bug in the Sun JPEG writer (see {@link
     * #addMetadata(IIOMetadataNode)}).
     */
    private static class SegmentInjectingOutputStream
            extends FilterOutputStream {
        private byte[] app1;

        SegmentInjectingOutputStream(String xmp, OutputStream os) {
            super(os);
            app1 = Util.assembleAPP1Segment(xmp);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (app1 != null) {
                out.write(Arrays.copyOfRange(b, off, 20));
                out.write(app1);
                out.write(Arrays.copyOfRange(b, 20, len));
                app1 = null;
            } else {
                out.write(b, off, len);
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEGImageWriter.class);

    private static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE;
    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.jpg.writer";

    /**
     * <p>When an {@literal unknown} node representing an {@literal APPn}
     * segment is appended to the {@literal markerSegment} node per the "Native
     * Metadata Format Tree Structure and Editing" section of
     * <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html">
     * JPEG Metadata Format Specification and Usage Notes</a>, the Sun JPEG
     * writer writes that segment before the {@literal APP0} segment, producing
     * a corrupt image (that resilient readers can nevertheless still
     * read).</p>
     *
     * <p>To avoid that, this method does nothing and an alternative metadata-
     * writing technique involving {@link SegmentInjectingOutputStream} is
     * used instead.</p>
     */
    @Override
    protected void addMetadata(final IIOMetadataNode baseTree) {
    }

    @Override
    protected String[] getApplicationPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.jpeg.JPEGImageWriter" };
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected String getUserPreferredIIOImplementation() {
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
        Color bgColor = encode.getBackgroundColor();
        if (bgColor == null) {
            bgColor = DEFAULT_BACKGROUND_COLOR;
        }
        return Java2DUtil.removeAlpha(image, bgColor);
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
        final Metadata metadata = encode.getMetadata();
        if (metadata != null && metadata.getXMP().isPresent()) {
            outputStream = new SegmentInjectingOutputStream(
                    metadata.getXMP().get(), outputStream);
        }

        // JPEG doesn't support alpha, so convert to RGB or else the
        // client will interpret as CMYK
        image = removeAlpha(image);
        final ImageWriteParam writeParam = getWriteParam();
        final IIOMetadata iioMetadata = getMetadata(writeParam, image);
        final IIOImage iioImage = new IIOImage(image, null, iioMetadata);

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
        final Metadata metadata = encode.getMetadata();
        if (metadata != null && metadata.getXMP().isPresent()) {
            outputStream = new SegmentInjectingOutputStream(
                    metadata.getXMP().get(), outputStream);
        }

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
        final IIOMetadata iioMetadata = getMetadata(writeParam, image);
        // JPEGImageWriter doesn't like RenderedOps, so give it a
        // BufferedImage.
        final IIOImage iioImage = new IIOImage(
                image.getAsBufferedImage(), null, iioMetadata);

        try (ImageOutputStream os =
                     ImageIO.createImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);
            iioWriter.write(null, iioImage, writeParam);
        } finally {
            iioWriter.dispose();
        }

    }

}
