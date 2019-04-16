package edu.illinois.library.cantaloupe.processor.codec.png;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * PNG image writer using Image I/O, capable of writing both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s as PNGs.
 *
 * @see <a href="http://libpng.org/pub/png/spec/1.2/PNG-Contents.html">
 *     PNG Specification, Version 1.2</a>
 */
public final class PNGImageWriter extends AbstractIIOImageWriter
        implements ImageWriter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PNGImageWriter.class);

    static final String IMAGEIO_PLUGIN_CONFIG_KEY =
            "processor.imageio.png.writer";

    @Override
    protected void addMetadata(final IIOMetadataNode baseTree) {
        final Metadata metadata = encode.getMetadata();

        if (metadata != null) {
            // Add native metadata, if available.
            if (metadata instanceof PNGMetadata) {
                ((PNGMetadata) metadata).getNativeMetadata().ifPresent(nativeMetadata -> {
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
                    nativeMetadata.forEach((key, value) -> {
                        IIOMetadataNode node = new IIOMetadataNode("tEXtEntry");
                        node.setAttribute("keyword", key);
                        node.setAttribute("value", value);
                        textNode.appendChild(node);
                    });
                });
            }

            // Add XMP metadata, if available.
            metadata.getXMP().ifPresent(xmp -> {
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
                xmpNode.setAttribute("text", xmp);
                itxtNode.appendChild(xmpNode);
            });
        }
    }

    @Override
    protected String[] getApplicationPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.png.PNGImageWriter" };
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

    /**
     * Writes the given image to the given output stream.
     *
     * @param image        Image to write.
     * @param outputStream Stream to write the image to.
     */
    @Override
    public void write(RenderedImage image,
                      OutputStream outputStream) throws IOException {
        final IIOMetadata metadata = getMetadata(
                iioWriter.getDefaultWriteParam(), image);
        final IIOImage iioImage = new IIOImage(image, null, metadata);

        try (ImageOutputStream os =
                     ImageIO.createImageOutputStream(outputStream)) {
            iioWriter.setOutput(os);
            iioWriter.write(iioImage);
        } finally {
            iioWriter.dispose();
        }
    }

}
