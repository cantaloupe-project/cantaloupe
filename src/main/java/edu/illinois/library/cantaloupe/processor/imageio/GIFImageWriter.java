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
 * GIF image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * GIFs.
 *
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/package-summary.html#gif_plugin_notes">
 *     Writing GIF Images</a>
 * @see <a href="http://justsolve.archiveteam.org/wiki/GIF">GIF</a>
 */
final class GIFImageWriter extends AbstractImageWriter {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(BMPImageReader.class);

    GIFImageWriter(OperationList opList,
                   Metadata sourceMetadata) {
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

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    /**
     * Writes the given image to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     */
    @Override
    void write(RenderedImage image,
               OutputStream outputStream) throws IOException {
        if (image instanceof PlanarImage) {
            // GIFImageWriter can't deal with a non-0,0 origin ("coordinate
            // out of bounds!")
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

}
