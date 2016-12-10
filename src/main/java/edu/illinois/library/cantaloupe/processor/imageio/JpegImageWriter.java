package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Java2dUtil;
import org.w3c.dom.Node;

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
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * JPEG image writer using ImageIO, capable of writing both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s as JPEGs.
 */
class JpegImageWriter extends AbstractImageWriter {

    static final String JAVA2D_JPG_QUALITY_CONFIG_KEY =
            "Java2dProcessor.jpg.quality";
    static final String JAI_JPG_QUALITY_CONFIG_KEY =
            "JaiProcessor.jpg.quality";

    JpegImageWriter(OperationList opList) {
        super(opList);
    }

    JpegImageWriter(OperationList opList,
                    Metadata sourceMetadata) {
        super(opList, sourceMetadata);
    }

    /**
     * @param baseTree Tree to embed the metadata into.
     * @throws IOException
     */
    @Override
    protected void addMetadata(final IIOMetadataNode baseTree)
            throws IOException {
        if (sourceMetadata instanceof JpegMetadata) {
            final Object exif = sourceMetadata.getExif();
            if (exif != null) {
                // Create the EXIF node.
                final IIOMetadataNode node = new IIOMetadataNode("unknown");
                node.setAttribute("MarkerTag", "225");
                node.setUserObject(exif);
                // Append it to /markerSequence/unknown[@MarkerTag=225]
                final Node markerSequence =
                        baseTree.getElementsByTagName("markerSequence").item(0);
                markerSequence.insertBefore(node, markerSequence.getFirstChild());
            }

            final Object iptc = sourceMetadata.getIptc();
            if (iptc != null) {
                // Create the IPTC node.
                final IIOMetadataNode node = new IIOMetadataNode("unknown");
                node.setAttribute("MarkerTag", "237");
                node.setUserObject(iptc);
                // Append it to /markerSequence/unknown[@MarkerTag=237]
                final Node markerSequence =
                        baseTree.getElementsByTagName("markerSequence").item(0);
                markerSequence.insertBefore(node, markerSequence.getFirstChild());
            }

            final byte[] xmp = sourceMetadata.getXmp();
            if (xmp != null) {
                // Create the XMP node.
                final IIOMetadataNode node = new IIOMetadataNode("unknown");
                node.setAttribute("MarkerTag", "225");
                node.setUserObject(xmp);
                // Append it to /markerSequence/unknown[@MarkerTag=225]
                final Node markerSequence =
                        baseTree.getElementsByTagName("markerSequence").item(0);
                markerSequence.insertBefore(node, markerSequence.getFirstChild());
            }
        }
    }

    private ImageWriteParam getJaiWriteParam(ImageWriter writer) {
        final Configuration config = ConfigurationFactory.getInstance();
        final ImageWriteParam writeParam = writer.getDefaultWriteParam();
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(config.
                getFloat(JAI_JPG_QUALITY_CONFIG_KEY, 0.7f));
        writeParam.setCompressionType("JPEG");
        return writeParam;
    }

    private ImageWriteParam getJava2dWriteParam(ImageWriter writer) {
        final Configuration config = ConfigurationFactory.getInstance();
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
            final IIOMetadata metadata = getMetadata(writer, writeParam, image);
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
