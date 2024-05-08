package edu.illinois.library.cantaloupe.processor.codec.gif;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.xmp.Utils;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.BufferedImageSequence;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * GIF image writer wrapping an Image I/O GIF plugin, capable of writing both
 * Java 2D {@link BufferedImage}s and JAI {@link PlanarImage}s as GIFs.
 *
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/package-summary.html#gif_plugin_notes">
 *     Writing GIF Images</a>
 * @see <a href="http://justsolve.archiveteam.org/wiki/GIF">GIF</a>
 */
public final class GIFImageWriter extends AbstractIIOImageWriter
        implements ImageWriter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFImageWriter.class);

    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.gif.writer";
    private static final String DEFAULT_IMAGEIO_WRITER =
            "com.sun.imageio.plugins.gif.GIFImageWriter";

    @Override
    protected void addMetadata(final IIOMetadataNode baseTree) {
        if (DEFAULT_IMAGEIO_WRITER.equals(iioWriter.getClass().getName())) {
            /* This writer doesn't support XMP metadata. From the Adobe XMP
            specification part 3:

            "GIF actually treats the Application Data as a series of GIF data
            sub-blocks. The first byte of each sub-block is the length of the
            sub-block’s content, not counting the first byte itself. ...
            Software that is unaware of XMP views packet data bytes as sub-
            block lengths."

            The net result is that the writer inserts sub-block length bytes
            where data bytes should go, corrupting it. */
            return;
        }
        final Metadata metadata = encode.getMetadata();
        if (metadata != null) {
            metadata.getXMP().ifPresent(xmp -> {
                xmp = Utils.encapsulateXMP(xmp);

                // Get the /ApplicationExtensions node, creating it if necessary.
                final NodeList appExtensionsList =
                        baseTree.getElementsByTagName("ApplicationExtensions");
                IIOMetadataNode appExtensions;
                if (appExtensionsList.getLength() > 0) {
                    appExtensions = (IIOMetadataNode) appExtensionsList.item(0);
                } else {
                    appExtensions = new IIOMetadataNode("ApplicationExtensions");
                    baseTree.appendChild(appExtensions);
                }

                // Create /ApplicationExtensions/ApplicationExtension
                final IIOMetadataNode appExtensionNode =
                        new IIOMetadataNode("ApplicationExtension");
                appExtensionNode.setAttribute("applicationID", "XMP Data");
                appExtensionNode.setAttribute("authenticationCode", "XMP");
                appExtensionNode.setUserObject(xmp.getBytes(StandardCharsets.UTF_8));
                appExtensions.appendChild(appExtensionNode);
            });
        }
    }

    /**
     * {@link #addMetadata(IIOMetadataNode)} is used for copying descriptive
     * metadata; this method is used for copying over structural image
     * metadata.
     */
    private void addStructuralMetadata(final IIOMetadataNode baseTree) {
        final Metadata metadata = encode.getMetadata();

        if (metadata instanceof GIFMetadata) {
            final GIFMetadata gifMetadata = (GIFMetadata) metadata;
            gifMetadata.getNativeMetadata().ifPresent(nativeMetadata -> {
                // Copy loop count.
                final int loopCount = nativeMetadata.getLoopCount();
                if (loopCount != 1) {
                    // Get the /ApplicationExtensions node, creating it if it does
                    // not exist.
                    final NodeList appExtensionsList =
                            baseTree.getElementsByTagName("ApplicationExtensions");
                    IIOMetadataNode appExtensions;
                    if (appExtensionsList.getLength() > 0) {
                        appExtensions = (IIOMetadataNode) appExtensionsList.item(0);
                    } else {
                        appExtensions = new IIOMetadataNode("ApplicationExtensions");
                        baseTree.appendChild(appExtensions);
                    }

                    // Create /ApplicationExtensions/ApplicationExtension
                    final IIOMetadataNode appExtensionNode =
                            new IIOMetadataNode("ApplicationExtension");
                    appExtensionNode.setAttribute("applicationID", "NETSCAPE");
                    appExtensionNode.setAttribute("authenticationCode", "2.0");
                    appExtensionNode.setUserObject(new byte[] {
                            0x1,
                            (byte) (loopCount & 0xFF),
                            (byte) ((loopCount >> 8) & 0xFF)
                    });
                    appExtensions.appendChild(appExtensionNode);
                }

                // Copy delay time.
                final int delayTime = nativeMetadata.getDelayTime();
                if (delayTime != 0) {
                    // Get the /GraphicControlExtension node, creating it if it
                    // does not exist.
                    final NodeList gcExtensionsList =
                            baseTree.getElementsByTagName("GraphicControlExtension");
                    IIOMetadataNode gcExtension;
                    if (gcExtensionsList.getLength() > 0) {
                        gcExtension = (IIOMetadataNode) gcExtensionsList.item(0);
                    } else {
                        gcExtension = new IIOMetadataNode("GraphicControlExtension");
                        baseTree.appendChild(gcExtension);
                    }

                    // Set /GraphicControlExtension node attributes.
                    gcExtension.setAttribute("disposalMethod", "none");
                    gcExtension.setAttribute("userInputFlag", "FALSE");
                    gcExtension.setAttribute("transparentColorFlag", "FALSE");
                    gcExtension.setAttribute("delayTime",
                            Integer.toString(delayTime));
                    gcExtension.setAttribute("transparentColorIndex", "0");
                }
            });
        }
    }

    @Override
    protected String[] getApplicationPreferredIIOImplementations() {
        return new String[] {DEFAULT_IMAGEIO_WRITER};
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

    @Override
    public void write(RenderedImage image,
                      OutputStream outputStream) throws IOException {
        if (image instanceof PlanarImage) {
            // ImageIO's GIFImageWriter can't deal with a non-0,0 origin
            // ("coordinate out of bounds!")
            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            pb.add((float) -image.getMinX());
            pb.add((float) -image.getMinY());
            image = JAI.create("translate", pb);
        }

        final ImageWriteParam writeParam = iioWriter.getDefaultWriteParam();
        final IIOMetadata metadata = getMetadata(writeParam, image);
        final IIOImage iioImage = new IIOImage(image, null, metadata);

        try (ImageOutputStream os = ImageIO.
                createImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);
            iioWriter.write(iioImage);
            os.flush(); // http://stackoverflow.com/a/14489406
        } finally {
            iioWriter.dispose();
        }
    }

    @Override
    public void write(BufferedImageSequence sequence,
                      OutputStream outputStream) throws IOException {
        final ImageWriteParam writeParam = iioWriter.getDefaultWriteParam();
        final IIOMetadata metadata = getMetadata(writeParam, sequence.get(0));

        String metaFormatName = metadata.getNativeMetadataFormatName();

        IIOMetadataNode root = (IIOMetadataNode)
                metadata.getAsTree(metaFormatName);
        addStructuralMetadata(root);
        metadata.setFromTree(metaFormatName, root);

        try (ImageOutputStream os = ImageIO.
                createImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);

            iioWriter.prepareWriteSequence(null);

            for (BufferedImage image : sequence) {
                final IIOImage iioImage = new IIOImage(image, null, metadata);
                iioWriter.writeToSequence(iioImage, writeParam);
            }
            iioWriter.endWriteSequence();
            os.flush();
        } finally {
            iioWriter.dispose();
        }
    }

}
