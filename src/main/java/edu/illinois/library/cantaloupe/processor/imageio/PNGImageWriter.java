package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

/**
 * PNG image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * PNGs.
 *
 * @see <a href="http://libpng.org/pub/png/spec/1.2/PNG-Contents.html">
 *     PNG Specification, Version 1.2</a>
 */
final class PNGImageWriter extends AbstractImageWriter {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(PNGImageWriter.class);

    PNGImageWriter(OperationList opList,
                   Metadata sourceMetadata) {
        super(opList, sourceMetadata);
    }

    /**
     * PNG doesn't (formally) support EXIF or IPTC, though it does support
     * EXIF tags in XMP.
     */
    @Override
    protected void addMetadata(final IIOMetadataNode baseTree)
            throws IOException {
        if (sourceMetadata instanceof PNGMetadata) {
            // Add native metadata.
            final List<IIOMetadataNode> nativeMetadata =
                    ((PNGMetadata) sourceMetadata).getNativeMetadata();
            if (nativeMetadata.size() > 0) {
                // Get the /tEXt node, creating it if it does not already exist.
                final NodeList textNodes = baseTree.getElementsByTagName("tEXt");
                IIOMetadataNode textNode;
                if (textNodes.getLength() > 0) {
                    textNode = (IIOMetadataNode) textNodes.item(0);
                } else {
                    textNode = new IIOMetadataNode("tEXt");
                    baseTree.appendChild(textNode);
                }
                // Append the metadata.
                for (IIOMetadataNode node : nativeMetadata) {
                    textNode.appendChild(node);
                }
            }

            // Add XMP metadata.
            final byte[] xmp = sourceMetadata.getXMP();
            if (xmp != null) {
                try {
                    // Get the /iTXt node, creating it if it does not already exist.
                    final NodeList itxtNodes = baseTree.getElementsByTagName("iTXt");
                    IIOMetadataNode itxtNode;
                    if (itxtNodes.getLength() > 0) {
                        itxtNode = (IIOMetadataNode) itxtNodes.item(0);
                    } else {
                        itxtNode = new IIOMetadataNode("iTXt");
                        baseTree.appendChild(itxtNode);
                    }
                    // Append the XMP.
                    final IIOMetadataNode xmpNode = new IIOMetadataNode("iTXtEntry");
                    xmpNode.setAttribute("keyword", "XML:com.adobe.xmp");
                    xmpNode.setAttribute("compressionFlag", "FALSE");
                    xmpNode.setAttribute("compressionMethod", "0");
                    xmpNode.setAttribute("languageTag", "");
                    xmpNode.setAttribute("translatedKeyword", "");
                    xmpNode.setAttribute("text", new String(xmp, "UTF-8"));
                    itxtNode.appendChild(xmpNode);
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error("addMetadata(): {}", e.getMessage());
                }
            }
        }
    }

    private ImageWriter getImageIOWriter() {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.PNG.getPreferredMediaType().toString());
        if (writers.hasNext()) {
            return writers.next();
        }
        return null;
    }

    /**
     * Writes the given image to the given output stream.
     *
     * @param image        Image to write.
     * @param outputStream Stream to write the image to.
     */
    void write(RenderedImage image,
               OutputStream outputStream) throws IOException {
        final ImageWriter writer = getImageIOWriter();
        if (writer != null) {
            final IIOMetadata metadata = getMetadata(
                    writer, writer.getDefaultWriteParam(), image);
            final IIOImage iioImage = new IIOImage(image, null, metadata);

            try (ImageOutputStream os =
                         ImageIO.createImageOutputStream(outputStream)) {
                writer.setOutput(os);
                writer.write(iioImage);
            } finally {
                writer.dispose();
            }
        } else {
            throw new IOException("Unable to obtain a " +
                    "javax.imageio.ImageWriter instance. This is a bug.");
        }
    }

}
