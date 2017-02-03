package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.Java2dUtil;
import edu.illinois.library.cantaloupe.processor.Processor;
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

    private ImageWriteParam getWriteParam(ImageWriter writer) {
        final ImageWriteParam writeParam = writer.getDefaultWriteParam();
        // Compression
        final int quality = (int) opList.getOptions().
                getOrDefault(Processor.JPG_QUALITY_CONFIG_KEY, 80);
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(quality * 0.01f);
        // Interlacing
        final boolean interlace = (boolean) opList.getOptions().
                getOrDefault(Processor.JPG_INTERLACE_CONFIG_KEY, false);
        writeParam.setProgressiveMode(interlace ?
                ImageWriteParam.MODE_DEFAULT : ImageWriteParam.MODE_DISABLED);

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
            final ImageWriteParam writeParam = getWriteParam(writer);
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
                    image.getColorModel()) > 3) {
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(image);
                final int[] bands = {0, 1, 2};
                pb.add(bands);
                image = JAI.create("bandselect", pb, null);
            }
            final ImageWriteParam writeParam = getWriteParam(writer);
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
