package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;

/**
 * Image writer using ImageIO, capable of writing both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s.
 */
class ImageIoImageWriter {

    private static Logger logger = LoggerFactory.
            getLogger(ImageIoImageWriter.class);

    static final String ICC_ENABLED_CONFIG_KEY = "icc.enabled";
    static final String ICC_STRATEGY_CONFIG_KEY = "icc.strategy";
    static final String ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY =
            "icc.BasicStrategy.profile";
    static final String ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY =
            "icc.BasicStrategy.profile_name";

    /**
     * @return Set of supported output formats.
     */
    public static Set<Format> supportedFormats() {
        return new HashSet<>(Arrays.asList(Format.GIF, Format.JPG,
                Format.PNG, Format.TIF));
    }

    /**
     * @param data Data to compress.
     * @return Deflate-compressed data.
     * @throws IOException
     */
    private byte[] deflate(byte[] data) throws IOException {
        ByteArrayOutputStream deflated = new ByteArrayOutputStream();
        DeflaterOutputStream deflater = new DeflaterOutputStream(deflated);
        deflater.write(data);
        deflater.flush();
        deflater.close();
        return deflated.toByteArray();
    }

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
            logger.debug("ICC profiles enabled ({} = true)",
                    ICC_ENABLED_CONFIG_KEY);
            final IIOMetadata metadata = writer.getDefaultImageMetadata(
                    ImageTypeSpecifier.createFromRenderedImage(image),
                    writeParam);

            final IIOMetadataNode iccNode = new IIOMetadataNode("iCCP");
            iccNode.setAttribute("compressionMethod", "deflate");

            switch (config.getString(ICC_STRATEGY_CONFIG_KEY)) {
                case "BasicStrategy":
                    addMetadataUsingBasicStrategy(metadata, iccNode);
                    return metadata;
                case "ScriptStrategy":
                    addMetadataUsingScriptStrategy(metadata, iccNode);
                    return metadata;
            }
        }
        logger.debug("ICC profile disabled ({} = false)",
                ICC_ENABLED_CONFIG_KEY);
        return null;
    }

    private void addMetadataUsingBasicStrategy(IIOMetadata metadata,
                                               IIOMetadataNode iccNode)
            throws IOException {
        final String profileName = Configuration.getInstance().
                getString(ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY);
        if (profileName != null) {
            logger.debug("Embedding {} ICC profile", profileName);

            final String profileFilename = Configuration.getInstance().
                    getString(ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY);
            if (profileFilename != null) {
                final ICC_ColorSpace colorSpace = getColorSpace(profileFilename);
                final byte[] compressedProfile =
                        deflate(colorSpace.getProfile().getData());
                iccNode.setUserObject(compressedProfile);
                iccNode.setAttribute("profileName", profileName);

                final Node nativeTree =
                        metadata.getAsTree(metadata.getNativeMetadataFormatName());
                nativeTree.appendChild(iccNode);
                metadata.mergeTree(metadata.getNativeMetadataFormatName(),
                        nativeTree);
            } else {
                logger.warn("Skipping ICC profile ({} is not set)",
                        ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY);
            }
        }
    }

    private void addMetadataUsingScriptStrategy(IIOMetadata metadata,
                                                IIOMetadataNode iccNode)
            throws IOException {
        // TODO: write this
    }

    /**
     * Writes a Java 2D {@link BufferedImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    public void write(BufferedImage image,
                      final Format outputFormat,
                      final OutputStream outputStream) throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                outputFormat.getPreferredMediaType().toString());
        final Configuration config = Configuration.getInstance();
        if (writers.hasNext()) {
            switch (outputFormat) {
                case JPG: // TODO: fix ICC profiles
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
                    break;
                case PNG:
                    writer = writers.next();
                    os = ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(os);
                    metadata = getMetadata(
                            writer, writer.getDefaultWriteParam(), image);
                    iioImage = new IIOImage(image, null, metadata);
                    try {
                        writer.write(iioImage);
                    } finally {
                        writer.dispose();
                    }
                    break;
                case TIF: // TODO: fix ICC profiles
                    writer = writers.next();
                    writeParam = writer.getDefaultWriteParam();
                    final String compressionType = config.
                            getString(Java2dProcessor.TIF_COMPRESSION_CONFIG_KEY);
                    if (compressionType != null) {
                        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        writeParam.setCompressionType(compressionType);
                    }
                    metadata = getMetadata(writer, writeParam, image);
                    iioImage = new IIOImage(image, null, metadata);
                    ImageOutputStream ios =
                            ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(ios);
                    try {
                        writer.write(null, iioImage, writeParam);
                        ios.flush(); // http://stackoverflow.com/a/14489406
                    } finally {
                        writer.dispose();
                    }
                    break;
                default:
                    writer = writers.next();
                    metadata = getMetadata(writer,
                            writer.getDefaultWriteParam(), image);
                    iioImage = new IIOImage(image, null, metadata);
                    ios = ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(ios);
                    try {
                        writer.write(iioImage);
                        ios.flush();
                    } finally {
                        writer.dispose();
                    }
                    break;
            }
        }
    }

    /**
     * Writes a JAI {@link PlanarImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    @SuppressWarnings({"deprecation"})
    public void write(PlanarImage image,
                      Format outputFormat,
                      OutputStream outputStream) throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                outputFormat.getPreferredMediaType().toString());
        final Configuration config = Configuration.getInstance();
        if (writers.hasNext()) {
            switch (outputFormat) {
                case GIF: // TODO: fix ICC profiles
                    // GIFWriter can't deal with a non-0,0 origin ("coordinate
                    // out of bounds!")
                    ParameterBlock pb = new ParameterBlock();
                    pb.addSource(image);
                    pb.add((float) -image.getMinX());
                    pb.add((float) -image.getMinY());
                    image = JAI.create("translate", pb);

                    ImageWriter writer = writers.next();
                    ImageOutputStream os = ImageIO.
                            createImageOutputStream(outputStream);
                    writer.setOutput(os);
                    try {
                        writer.write(image);
                        os.flush(); // http://stackoverflow.com/a/14489406
                    } finally {
                        writer.dispose();
                    }
                    break;
                case JPG: // TODO: fix ICC profiles
                    writer = writers.next();
                    try {
                        // JPEGImageWriter will interpret a >3-band image as
                        // CMYK. So, select only the first 3 bands.
                        if (OpImage.getExpandedNumBands(image.getSampleModel(),
                                image.getColorModel()) == 4) {
                            pb = new ParameterBlock();
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
                        os = ImageIO.createImageOutputStream(outputStream);
                        writer.setOutput(os);
                        // JPEGImageWriter doesn't like RenderedOps, so give it
                        // a BufferedImage.
                        IIOImage iioImage = new IIOImage(
                                image.getAsBufferedImage(), null, null);
                        writer.write(null, iioImage, writeParam);
                    } finally {
                        writer.dispose();
                    }
                    break;
                case PNG: // TODO: test ICC profiles
                    writer = writers.next();
                    os = ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(os);
                    IIOMetadata metadata = getMetadata(
                            writer, writer.getDefaultWriteParam(), image);
                    IIOImage iioImage = new IIOImage(image, null, metadata);
                    try {
                        writer.write(iioImage);
                    } finally {
                        writer.dispose();
                    }
                    break;
                case TIF: // TODO: fix ICC profiles
                    writer = writers.next();
                    final String compressionType = config.getString(
                            JaiProcessor.TIF_COMPRESSION_CONFIG_KEY);
                    ImageWriteParam writeParam = writer.getDefaultWriteParam();
                    if (compressionType != null) {
                        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        writeParam.setCompressionType(compressionType);
                    }
                    metadata = getMetadata(writer, writeParam, image);
                    iioImage = new IIOImage(image, null, metadata);
                    ImageOutputStream ios =
                            ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(ios);
                    try {
                        writer.write(null, iioImage, writeParam);
                        ios.flush(); // http://stackoverflow.com/a/14489406
                    } finally {
                        writer.dispose();
                    }
                    break;
                default:
                    writer = writers.next();
                    metadata = getMetadata(
                            writer, writer.getDefaultWriteParam(), image);
                    iioImage = new IIOImage(image, null, metadata);
                    ios = ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(ios);
                    try {
                        writer.write(iioImage);
                        ios.flush();
                    } finally {
                        writer.dispose();
                    }
                    break;
            }
        }
    }

    /**
     * Finds the given profile whether it is given in the form of an absolute
     * path or a filename. If the latter, it will be searched for in the same
     * folder as the application config (if available), or the current working
     * directory if not.
     *
     * @param filenameOrPathname Filename or absolute pathname
     * @return File corresponding to <var>profileFilename</var>
     */
    private File findProfile(String filenameOrPathname) {
        File profileFile = new File(filenameOrPathname);
        if (!profileFile.isAbsolute()) {
            final File configFile =
                    Configuration.getInstance().getConfigurationFile();
            if (configFile != null) {
                profileFile = new File(configFile.getParent() + "/" +
                        profileFile.getName());
            } else {
                profileFile = new File("./" + profileFile.getName());
            }
        }
        return profileFile;
    }

    private ICC_ColorSpace getColorSpace(String profileFilenameOrPathname)
            throws IOException {
        final FileInputStream in =
                new FileInputStream(findProfile(profileFilenameOrPathname));
        try {
            final ICC_Profile profile = ICC_Profile.getInstance(in);
            return new ICC_ColorSpace(profile);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

}
