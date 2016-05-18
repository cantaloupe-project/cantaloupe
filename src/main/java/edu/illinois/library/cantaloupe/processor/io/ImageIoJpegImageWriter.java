package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.Java2dUtil;
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
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * JPEG image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * JPEGs.
 *
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html">
 *     JPEG Metadata Format Specification and Usage Notes</a>
 * @see <a href="http://www.color.org/icc_specs2.xalter">ICC Specifications</a>
 */
class ImageIoJpegImageWriter extends AbstractImageIoImageWriter {

    static final String JAVA2D_JPG_QUALITY_CONFIG_KEY =
            "Java2dProcessor.jpg.quality";
    static final String JAI_JPG_QUALITY_CONFIG_KEY =
            "JaiProcessor.jpg.quality";

    ImageIoJpegImageWriter(RequestAttributes attrs) {
        super(attrs);
    }

    /**
     * @param metadata Metadata to embed the profile into.
     * @param profile Profile to embed.
     * @throws IOException
     */
    protected IIOMetadata embedIccProfile(final IIOMetadata metadata,
                                          final IccProfile profile)
            throws IOException {
        final IIOMetadataNode iccNode = new IIOMetadataNode("app2ICC");
        iccNode.setUserObject(profile.getProfile());

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
        return metadata;
    }

    private ImageWriteParam getJaiWriteParam(ImageWriter writer) {
        final Configuration config = Configuration.getInstance();
        final ImageWriteParam writeParam = writer.getDefaultWriteParam();
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(config.
                getFloat(JAI_JPG_QUALITY_CONFIG_KEY, 0.7f));
        writeParam.setCompressionType("JPEG");
        return writeParam;
    }

    private ImageWriteParam getJava2dWriteParam(ImageWriter writer) {
        final Configuration config = Configuration.getInstance();
        final ImageWriteParam writeParam = writer.getDefaultWriteParam();
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(config.
                getFloat(JAVA2D_JPG_QUALITY_CONFIG_KEY, 0.7f));
        writeParam.setCompressionType("JPEG");
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
                Format.JPG.getPreferredMediaType().toString());
        if (writers.hasNext()) {
            final ImageWriter writer = writers.next();
            try {
                // JPEG doesn't support alpha, so convert to RGB or else the
                // client will interpret as CMYK
                image = Java2dUtil.removeAlpha(image);
                final ImageWriteParam writeParam = getJava2dWriteParam(writer);
                final ImageOutputStream os =
                        ImageIO.createImageOutputStream(outputStream);
                writer.setOutput(os);
                final IIOMetadata metadata = getMetadata(writer, writeParam,
                        image);
                final IIOImage iioImage = new IIOImage(image, null, metadata);
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
        if (writers.hasNext()) {
            final ImageWriter writer = writers.next();
            try {
                // JPEGImageWriter will interpret a >3-band image as CMYK.
                // So, select only the first 3 bands.
                if (OpImage.getExpandedNumBands(image.getSampleModel(),
                        image.getColorModel()) == 4) {
                    ParameterBlock pb = new ParameterBlock();
                    pb.addSource(image);
                    final int[] bands = {0, 1, 2};
                    pb.add(bands);
                    image = JAI.create("bandselect", pb, null);
                }
                final ImageWriteParam writeParam = getJaiWriteParam(writer);
                final IIOMetadata metadata = getMetadata(writer, writeParam,
                        image);
                // JPEGImageWriter doesn't like RenderedOps, so give it
                // a BufferedImage.
                final IIOImage iioImage = new IIOImage(
                        image.getAsBufferedImage(), null, metadata);
                final ImageOutputStream os =
                        ImageIO.createImageOutputStream(outputStream);
                writer.setOutput(os);
                writer.write(null, iioImage, writeParam);
            } finally {
                writer.dispose();
            }
        }
    }

}
