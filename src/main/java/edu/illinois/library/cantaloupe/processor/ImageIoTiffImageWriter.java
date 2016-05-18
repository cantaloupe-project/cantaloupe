package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.RequestAttributes;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import javax.script.ScriptException;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import static edu.illinois.library.cantaloupe.processor.IccProfileService.
        ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY;

/**
 * TIFF image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * TIFFs.
 *
 * @see <a href="http://www.color.org/icc_specs2.xalter">ICC Specifications</a>
 * @see <a href="http://download.java.net/media/jai-imageio/javadoc/1.1/com/sun/media/imageio/plugins/tiff/package-summary.html">
 *     ImageIO TIFF Plugin Documentation</a>
 */
class ImageIoTiffImageWriter extends AbstractImageIoImageWriter {

    ImageIoTiffImageWriter(RequestAttributes attrs) {
        super(attrs);
    }

    /**
     * @param inMetadata Metadata to populate.
     * @return Metadata instance with ICC profile added.
     * @throws IOException
     */
    protected IIOMetadata addMetadataUsingBasicStrategy(IIOMetadata inMetadata)
            throws IOException {
        IIOMetadata metadata = inMetadata;
        final String profileFilename = Configuration.getInstance().
                getString(ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY);
        if (profileFilename != null) {
            final ICC_Profile profile = new IccProfileService().
                    getProfile(profileFilename);
            metadata = embedIccProfile(metadata, profile);
        }
        return metadata;
    }

    /**
     * @param inMetadata Metadata to populate.
     * @return Metadata instance with ICC profile added.
     * @throws IOException
     */
    protected IIOMetadata addMetadataUsingScriptStrategy(IIOMetadata inMetadata)
            throws IOException, ScriptException {
        IIOMetadata metadata = inMetadata;
        final ICC_Profile profile = new IccProfileService().
                getProfileFromDelegateMethod(
                        requestAttributes.getOperationList().getIdentifier(),
                        requestAttributes.getHeaders(),
                        requestAttributes.getClientIp());
        if (profile != null) {
            metadata = embedIccProfile(metadata, profile);
        }
        return metadata;
    }

    /**
     * @param metadata Metadata to embed the profile into.
     * @param profile Profile to embed.
     * @return Metadata instance with ICC profile added.
     * @throws IOException
     */
    private IIOMetadata embedIccProfile(final IIOMetadata metadata,
                                        final ICC_Profile profile)
            throws IOException {
        if (profile != null) {
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            final BaselineTIFFTagSet base = BaselineTIFFTagSet.getInstance();
            final TIFFTag iccTag = base.getTag(BaselineTIFFTagSet.TAG_ICC_PROFILE);
            final ICC_ColorSpace colorSpace = new ICC_ColorSpace(profile);

            final byte[] data = colorSpace.getProfile().getData();
            final TIFFField iccField = new TIFFField(
                    iccTag, TIFFTag.TIFF_UNDEFINED, data.length, data);
            dir.addTIFFField(iccField);
            return dir.getAsMetadata();
        }
        return metadata;
    }

    /**
     * @param writer Writer to abtain parameters for
     * @return Write parameters respecting the application configuration.
     */
    private ImageWriteParam getWriteParam(ImageWriter writer) {
        final ImageWriteParam writeParam = writer.getDefaultWriteParam();
        final Configuration config = Configuration.getInstance();
        final String compressionType = config.getString(
                JaiProcessor.TIF_COMPRESSION_CONFIG_KEY);
        if (compressionType != null) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType(compressionType);
        }
        return writeParam;
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
                Format.TIF.getPreferredMediaType().toString());
        if (writers.hasNext()) {
            final ImageWriter writer = writers.next();
            try {
                final ImageWriteParam writeParam = getWriteParam(writer);
                final IIOMetadata metadata = getMetadata(writer, writeParam, image);
                final IIOImage iioImage = new IIOImage(image, null, metadata);
                final ImageOutputStream ios =
                        ImageIO.createImageOutputStream(outputStream);
                writer.setOutput(ios);
                writer.write(metadata, iioImage, writeParam);
                ios.flush(); // http://stackoverflow.com/a/14489406
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
    void write(PlanarImage image, OutputStream outputStream)
            throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.TIF.getPreferredMediaType().toString());
        if (writers.hasNext()) {
            final ImageWriter writer = writers.next();
            try {
                final ImageWriteParam writeParam = getWriteParam(writer);
                final IIOMetadata metadata = getMetadata(writer, writeParam, image);
                final IIOImage iioImage = new IIOImage(image, null, metadata);
                final ImageOutputStream ios =
                        ImageIO.createImageOutputStream(outputStream);
                writer.setOutput(ios);
                writer.write(null, iioImage, writeParam);
                ios.flush(); // http://stackoverflow.com/a/14489406
            } finally {
                writer.dispose();
            }
        }
    }

}
