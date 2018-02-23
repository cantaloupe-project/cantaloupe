package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.operation.OperationList;
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

/**
 * GIF image writer using ImageIO, capable of writing both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s as GIFs.
 *
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/package-summary.html#gif_plugin_notes">
 *     Writing GIF Images</a>
 * @see <a href="http://justsolve.archiveteam.org/wiki/GIF">GIF</a>
 */
final class GIFImageWriter extends AbstractImageWriter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BMPImageReader.class);

    GIFImageWriter(OperationList opList) {
        super(opList);
    }

    GIFImageWriter(OperationList opList, Metadata sourceMetadata) {
        super(opList, sourceMetadata);
    }

    @Override
    protected void addMetadata(final IIOMetadataNode baseTree) {
        if (sourceMetadata instanceof GIFMetadata) {
            // GIF doesn't support EXIF or IPTC metadata -- only XMP.
            // The XMP node will be located at /ApplicationExtensions/
            // ApplicationExtension[@applicationID="XMP Data" @authenticationCode="XMP"]
            final byte[] xmp = sourceMetadata.getXMP();
            if (xmp != null) {
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
                appExtensionNode.setAttribute("applicationID", "XMP Data");
                appExtensionNode.setAttribute("authenticationCode", "XMP");
                appExtensionNode.setUserObject(xmp);
                appExtensions.appendChild(appExtensionNode);
            }
        }
    }

    /**
     * {@link #addMetadata(IIOMetadataNode)} is used for copying descriptive
     * metadata; this method is used for copying over structural image
     * metadata.
     */
    private void addStructuralMetadata(final IIOMetadataNode baseTree) {
        if (sourceMetadata instanceof GIFMetadata) {
            final GIFMetadata srcMetadata = (GIFMetadata) sourceMetadata;

            // Copy loop count.
            final int loopCount = srcMetadata.getLoopCount();
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

            // Copy frame interval.
            final int frameInterval = srcMetadata.getFrameInterval();
            if (frameInterval != 0) {
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
                        Integer.toString(Math.round(frameInterval / 10f)));
                gcExtension.setAttribute("transparentColorIndex", "0");
            }
        }
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    @Override
    String[] preferredIIOImplementations() {
        // We don't want com.sun.media.imageioimpl.plugins.gif.GIFImageWriter!
        return new String[] { "com.sun.imageio.plugins.gif.GIFImageWriter" };
    }

    @Override
    void write(RenderedImage image,
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
    void write(final BufferedImageSequence sequence,
               final OutputStream outputStream) throws IOException {
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
