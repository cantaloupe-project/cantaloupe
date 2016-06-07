package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.icc.IccProfile;
import edu.illinois.library.cantaloupe.processor.Java2dUtil;
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
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

    ImageIoJpegImageWriter(OperationList opList) {
        super(opList);
    }

    ImageIoJpegImageWriter(OperationList opList,
                           ImageIoMetadata sourceMetadata) {
        super(opList, sourceMetadata);
    }

    /**
     * @param baseTree Metadata to embed the profile into.
     * @param profile Profile to embed.
     * @throws IOException
     */
    @Override
    protected void addIccProfile(final IIOMetadataNode baseTree,
                                 final IccProfile profile)
            throws IOException {
        final IIOMetadataNode iccNode = new IIOMetadataNode("app2ICC");
        iccNode.setUserObject(profile.getProfile());

        // Append the app2ICC node we just created to /JPEGvariety/app0JFIF
        // TODO: simplify this
        NodeList level1Nodes = baseTree.getChildNodes();
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
    }

    /**
     * @param baseTree Tree to embed the metadata into.
     * @throws IOException
     */
    @Override
    protected void addMetadata(final IIOMetadataNode baseTree)
            throws IOException {
        final byte[] iptc = extractSourceIptc();
        if (iptc != null) {
            // Create the IPTC node.
            final IIOMetadataNode exifNode = new IIOMetadataNode("unknown");
            exifNode.setAttribute("MarkerTag", "237");
            exifNode.setUserObject(iptc);

            // Append the node we just created to
            // /markerSequence/unknown[@MarkerTag=237]
            final Node markerSequence = baseTree.
                    getElementsByTagName("markerSequence").item(0);
            markerSequence.appendChild(exifNode);
        }

        for (byte[] data : extractSourceExifAndXmp()) {
            // Create the EXIF node.
            final IIOMetadataNode metadataNode = new IIOMetadataNode("unknown");
            metadataNode.setAttribute("MarkerTag", "225");
            metadataNode.setUserObject(data);
            // Append it to /markerSequence/unknown[@MarkerTag=225]
            baseTree.getElementsByTagName("markerSequence").item(0).
                    appendChild(metadataNode);
        }
    }

    /**
     * EXIF and XMP metadata both appear in the {@link IIOMetadataNode} tree as
     * identical nodes at <code>/markerSequence/unknown[@MarkerTag=225]</code>
     * -- so, there is no way to tell them apart, other than by reading and
     * analyzing the data from both, which is inefficient. This method
     * therefore adds both EXIF and XMP metadata in one shot.
     *
     * @return EXIF and XMP data, or null if none was found in the source
     *         metadata.
     */
    private Collection<byte[]> extractSourceExifAndXmp() {
        final Set<byte[]> datas = new HashSet<>();
        final IIOMetadataNode markerSequence = (IIOMetadataNode) sourceMetadata.
                getAsTree().getElementsByTagName("markerSequence").item(0);
        final NodeList unknowns = markerSequence.getElementsByTagName("unknown");
        for (int i = 0; i < unknowns.getLength(); i++) {
            final IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
            if ("225".equals(marker.getAttribute("MarkerTag"))) {
                datas.add((byte[]) marker.getUserObject());
            }
        }
        return datas;
    }

    /**
     * @return IPTC data, or null if none was found in the source metadata.
     */
    private byte[] extractSourceIptc() {
        // IPTC metadata is located at /markerSequence/unknown[@MarkerTag=237]
        final IIOMetadataNode markerSequence = (IIOMetadataNode) sourceMetadata.
                getAsTree().getElementsByTagName("markerSequence").item(0);
        NodeList unknowns = markerSequence.getElementsByTagName("unknown");
        for (int i = 0; i < unknowns.getLength(); i++) {
            IIOMetadataNode marker = (IIOMetadataNode) unknowns.item(i);
            if ("237".equals(marker.getAttribute("MarkerTag"))) {
                return (byte[]) marker.getUserObject();
            }
        }
        return null;
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
