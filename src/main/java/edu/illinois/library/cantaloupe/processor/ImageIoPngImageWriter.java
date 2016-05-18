package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.RequestAttributes;
import org.w3c.dom.Node;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import javax.script.ScriptException;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.zip.DeflaterOutputStream;

import static edu.illinois.library.cantaloupe.processor.IccProfileService.
        ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY;
import static edu.illinois.library.cantaloupe.processor.IccProfileService.
        ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY;

/**
 * PNG image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * PNGs.
 *
 * @see <a href="http://www.color.org/icc_specs2.xalter">ICC Specifications</a>
 * @see <a href="http://libpng.org/pub/png/spec/1.2/PNG-Contents.html">
 *     PNG Specification, Version 1.2</a>
 */
class ImageIoPngImageWriter extends AbstractImageIoImageWriter {

    ImageIoPngImageWriter(RequestAttributes attrs) {
        super(attrs);
    }

    /**
     * @param metadata Metadata to populate.
     * @throws IOException
     */
    protected IIOMetadata addMetadataUsingBasicStrategy(
            final IIOMetadata metadata) throws IOException {
        final String profileName = Configuration.getInstance().
                getString(ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY);
        if (profileName != null) {
            final String profileFilename = Configuration.getInstance().
                    getString(ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY);
            if (profileFilename != null) {
                final ICC_Profile profile = new IccProfileService().
                        getProfile(profileFilename);
                embedIccProfile(metadata, profile, profileName);
            }
        }
        return metadata;
    }

    /**
     * @param metadata Metadata to populate.
     * @throws IOException
     */
    protected IIOMetadata addMetadataUsingScriptStrategy(
            final IIOMetadata metadata) throws IOException, ScriptException {
        final ICC_Profile profile = new IccProfileService().
                getProfileFromDelegateMethod(
                        requestAttributes.getOperationList().getIdentifier(),
                        requestAttributes.getHeaders(),
                        requestAttributes.getClientIp());
        if (profile != null) {
            embedIccProfile(metadata, profile, profile.toString());
        }
        return metadata;
    }

    /**
     * Applies Deflate compression to the given bytes.
     *
     * @param data Data to compress.
     * @return Deflate-compressed data.
     * @throws IOException
     */
    private byte[] deflate(final byte[] data) throws IOException {
        ByteArrayOutputStream deflated = new ByteArrayOutputStream();
        DeflaterOutputStream deflater = new DeflaterOutputStream(deflated);
        deflater.write(data);
        deflater.flush();
        deflater.close();
        return deflated.toByteArray();
    }

    /**
     * @param metadata Metadata to embed the profile into.
     * @param profile Profile to embed.
     * @param profileName Name of the profile.
     * @throws IOException
     */
    private void embedIccProfile(final IIOMetadata metadata,
                                 final ICC_Profile profile,
                                 final String profileName)
            throws IOException {
        final ICC_ColorSpace colorSpace = new ICC_ColorSpace(profile);
        final byte[] compressedProfile =
                deflate(colorSpace.getProfile().getData());
        final IIOMetadataNode iccNode =
                new IIOMetadataNode("iCCP");
        iccNode.setAttribute("compressionMethod", "deflate");
        iccNode.setAttribute("profileName", profileName);
        iccNode.setUserObject(compressedProfile);

        final Node nativeTree = metadata.
                getAsTree(metadata.getNativeMetadataFormatName());
        nativeTree.appendChild(iccNode);
        metadata.mergeTree(metadata.getNativeMetadataFormatName(),
                nativeTree);
    }

    /**
     * Writes a Java 2D {@link BufferedImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(BufferedImage image,
               final OutputStream outputStream) throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.PNG.getPreferredMediaType().toString());
        if (writers.hasNext()) {
            final ImageWriter writer = writers.next();
            try {
                final IIOMetadata metadata = getMetadata(
                        writer, writer.getDefaultWriteParam(), image);
                final IIOImage iioImage = new IIOImage(image, null, metadata);
                final ImageOutputStream os =
                        ImageIO.createImageOutputStream(outputStream);
                writer.setOutput(os);
                writer.write(iioImage);
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
    void write(PlanarImage image,
               OutputStream outputStream) throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.PNG.getPreferredMediaType().toString());
        if (writers.hasNext()) {
            final ImageWriter writer = writers.next();
            try {
                final IIOMetadata metadata = getMetadata(
                        writer, writer.getDefaultWriteParam(), image);
                final IIOImage iioImage = new IIOImage(image, null, metadata);
                final ImageOutputStream os =
                        ImageIO.createImageOutputStream(outputStream);
                writer.setOutput(os);
                writer.write(iioImage);
            } finally {
                writer.dispose();
            }
        }
    }

}
