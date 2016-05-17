package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import static edu.illinois.library.cantaloupe.processor.IccProfileService.
        ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY;
import static edu.illinois.library.cantaloupe.processor.IccProfileService.
        ICC_ENABLED_CONFIG_KEY;
import static edu.illinois.library.cantaloupe.processor.IccProfileService.
        ICC_STRATEGY_CONFIG_KEY;

/**
 * JPEG image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * JPEGs.
 *
 * @see <a href="http://www.color.org/icc_specs2.xalter">ICC Specifications</a>
 */
class ImageIoJpegImageWriter {

    private static Logger logger = LoggerFactory.
            getLogger(ImageIoJpegImageWriter.class);

    /**
     * @param writer Writer from which to obtain default metadata.
     * @param writeParam Image writer parameters, already populated for writing.
     * @param image Image to apply the metadata to.
     * @return Metadata with optional embedded color profile according to the
     *         configuration.
     * @throws IOException
     */
    private IIOMetadata getMetadata(ImageWriter writer,
                                    ImageWriteParam writeParam,
                                    RenderedImage image) throws IOException {
        final Configuration config = Configuration.getInstance();

        if (config.getBoolean(ICC_ENABLED_CONFIG_KEY, false)) {
            logger.debug("getMetadata(): ICC profiles enabled ({} = true)",
                    ICC_ENABLED_CONFIG_KEY);
            final IIOMetadata metadata = writer.getDefaultImageMetadata(
                    ImageTypeSpecifier.createFromRenderedImage(image),
                    writeParam);
            switch (config.getString(ICC_STRATEGY_CONFIG_KEY, "")) {
                case "BasicStrategy":
                    return addMetadataUsingBasicStrategy(metadata);
                case "ScriptStrategy":
                    return addMetadataUsingScriptStrategy(metadata);
            }
        }
        logger.debug("ICC profile disabled ({} = false)",
                ICC_ENABLED_CONFIG_KEY);
        return null;
    }

    /**
     * @param inMetadata Metadata to populate.
     * @return Metadata instance with ICC profile added.
     * @throws IOException
     */
    private IIOMetadata addMetadataUsingBasicStrategy(IIOMetadata inMetadata)
            throws IOException {
        IIOMetadata metadata = inMetadata;
        final String profileFilename = Configuration.getInstance().
                getString(ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY);
        if (profileFilename != null) {
            metadata = embedIccProfile(metadata, profileFilename);
        }
        return metadata;
    }

    /**
     * @param inMetadata Metadata to populate.
     * @return Metadata instance with ICC profile added.
     * @throws IOException
     */
    private IIOMetadata addMetadataUsingScriptStrategy(IIOMetadata inMetadata)
            throws IOException {
        // TODO: write this
        return inMetadata;
    }

    /**
     * @param metadata Metadata to embed the profile into.
     * @param profileFilename Pathname or filename of the profile.
     * @return Metadata instance with ICC profile added.
     * @throws IOException
     */
    private IIOMetadata embedIccProfile(final IIOMetadata metadata,
                                        final String profileFilename)
            throws IOException {
        logger.debug("embedIccProfile(): using profile: {}", profileFilename);
        // TODO: write this
        return metadata;
    }

    /**
     * Writes a Java 2D {@link BufferedImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(BufferedImage image, final OutputStream outputStream)
            throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.JPG.getPreferredMediaType().toString());
        final Configuration config = Configuration.getInstance();
        if (writers.hasNext()) {
            // JPEG doesn't support alpha, so convert to RGB or else the
            // client will interpret as CMYK
            image = Java2dUtil.removeAlpha(image);
            ImageWriter writer = writers.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(config.
                    getFloat(Java2dProcessor.JPG_QUALITY_CONFIG_KEY, 0.7f));
            writeParam.setCompressionType("JPEG");
            ImageOutputStream os =
                    ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(os);
            IIOMetadata metadata = getMetadata(writer, writeParam, image);
            IIOImage iioImage = new IIOImage(image, null, metadata);
            try {
                writer.write(null, iioImage, writeParam);
            } finally {
                writer.dispose();
            }
        }
    }

    /**
     * Writes a JAI {@link PlanarImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    @SuppressWarnings({"deprecation"})
    void write(PlanarImage image, OutputStream outputStream)
            throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.JPG.getPreferredMediaType().toString());
        final Configuration config = Configuration.getInstance();
        if (writers.hasNext()) {
            ImageWriter writer = writers.next();
            try {
                // JPEGImageWriter will interpret a >3-band image as
                // CMYK. So, select only the first 3 bands.
                if (OpImage.getExpandedNumBands(image.getSampleModel(),
                        image.getColorModel()) == 4) {
                    ParameterBlock pb = new ParameterBlock();
                    pb.addSource(image);
                    final int[] bands = {0, 1, 2};
                    pb.add(bands);
                    image = JAI.create("bandselect", pb, null);
                }
                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(config.getFloat(
                        JaiProcessor.JPG_QUALITY_CONFIG_KEY, 0.7f));
                writeParam.setCompressionType("JPEG");
                ImageOutputStream os =
                        ImageIO.createImageOutputStream(outputStream);
                writer.setOutput(os);
                // JPEGImageWriter doesn't like RenderedOps, so give it
                // a BufferedImage.
                IIOImage iioImage = new IIOImage(
                        image.getAsBufferedImage(), null, null);
                writer.write(null, iioImage, writeParam);
            } finally {
                writer.dispose();
            }
        }
    }

}
