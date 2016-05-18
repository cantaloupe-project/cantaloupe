package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.RequestAttributes;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.script.ScriptException;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import static edu.illinois.library.cantaloupe.processor.IccProfileService.
        ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY;

/**
 * GIF image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * GIFs.
 */
class ImageIoGifImageWriter extends AbstractImageIoImageWriter {

    ImageIoGifImageWriter(RequestAttributes attrs) {
        super(attrs);
    }

    /**
     * @param metadata Metadata to populate.
     * @throws IOException
     */
    protected IIOMetadata addMetadataUsingBasicStrategy(
            final IIOMetadata metadata) throws IOException {
        final String profileFilename = Configuration.getInstance().
                getString(ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY);
        if (profileFilename != null) {
            final ICC_Profile profile = new IccProfileService().
                    getProfile(profileFilename);
            embedIccProfile(metadata, profile);
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
            embedIccProfile(metadata, profile);
        }
        return metadata;
    }

    /**
     * @param metadata Metadata to embed the profile into.
     * @param profile Profile to embed.
     * @throws IOException
     */
    private void embedIccProfile(final IIOMetadata metadata,
                                 final ICC_Profile profile)
            throws IOException {
        final IIOMetadataNode iccNode = new IIOMetadataNode("app2ICC");
        iccNode.setUserObject(profile);

        final Node nativeTree = metadata.
                getAsTree(metadata.getNativeMetadataFormatName());

        // Append the app2ICC node we just created to /JPEGvariety/app0JFIF
        NodeList level1Nodes = nativeTree.getChildNodes();
        for (int i = 0; i < level1Nodes.getLength(); i++) {
            Node level1Node = level1Nodes.item(i);
            if (level1Node.getNodeName().equals("JPEGvariety")) {
                NodeList level2Nodes = level1Node.getChildNodes();
                for (int j = 0; j < level2Nodes.getLength(); j++) {
                    Node level2Node = level2Nodes.item(j);
                    if (level2Node.getNodeName().equals("app0JFIF")) {
                        level2Node.appendChild(iccNode);
                        break;
                    }
                }
            }
        }
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
    void write(BufferedImage image, final OutputStream outputStream)
            throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.GIF.getPreferredMediaType().toString());
        if (writers.hasNext()) {
            final ImageWriter writer = writers.next();
            try {
                final ImageWriteParam writeParam =
                        writer.getDefaultWriteParam();
                final IIOMetadata metadata = getMetadata(writer, writeParam,
                        image);
                final IIOImage iioImage = new IIOImage(image, null, metadata);
                final ImageOutputStream ios =
                        ImageIO.createImageOutputStream(outputStream);
                writer.setOutput(ios);
                writer.write(iioImage);
                ios.flush();
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
                Format.GIF.getPreferredMediaType().toString());
        if (writers.hasNext()) {
            final ImageWriter writer = writers.next();
            try {
                // GIFWriter can't deal with a non-0,0 origin ("coordinate
                // out of bounds!")
                final ParameterBlock pb = new ParameterBlock();
                pb.addSource(image);
                pb.add((float) -image.getMinX());
                pb.add((float) -image.getMinY());
                image = JAI.create("translate", pb);

                final ImageWriteParam writeParam =
                        writer.getDefaultWriteParam();
                final IIOMetadata metadata = getMetadata(writer, writeParam,
                        image);
                final IIOImage iioImage = new IIOImage(image, null, metadata);
                final ImageOutputStream os = ImageIO.
                        createImageOutputStream(outputStream);
                writer.setOutput(os);
                writer.write(iioImage);
                os.flush(); // http://stackoverflow.com/a/14489406
            } finally {
                writer.dispose();
            }
        }
    }

}
