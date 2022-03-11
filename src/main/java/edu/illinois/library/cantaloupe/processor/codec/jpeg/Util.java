package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.image.xmp.Utils;
import edu.illinois.library.cantaloupe.util.ArrayUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

final class Util {

    /**
     * @param xmp XMP data with a root {@literal <rdf:RDF>} element.
     * @return    Full {@literal APP1} segment data including marker and length.
     */
    static byte[] assembleAPP1Segment(String xmp) {
        try {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            final byte[] headerBytes = Constants.STANDARD_XMP_SEGMENT_HEADER;
            final byte[] xmpBytes = Utils.encapsulateXMP(xmp).
                    getBytes(StandardCharsets.UTF_8);
            // write segment marker
            os.write(Marker.APP1.marker());
            // write segment length
            os.write(ByteBuffer.allocate(2)
                    .putShort((short) (headerBytes.length + xmpBytes.length + 3))
                    .array());
            // write segment header
            os.write(headerBytes);
            // write XMP data
            os.write(xmpBytes);
            // write null terminator
            os.write(0);
            return os.toByteArray();
        } catch (IOException ignore) {
            // Call ByteArrayOutputStream's bluff on throwing this.
        }
        return new byte[0];
    }

    /**
     * @param xmpChunks Zero or more chunks of XMP data (one per {@literal
     *                  APP1} segment).
     * @return          Fully formed XML tree, or {@code null} if no chunks
     *                  were supplied.
     */
    static String assembleXMP(final List<byte[]> xmpChunks) throws IOException {
        String standardXMP = null;
        final int numChunks = xmpChunks.size();
        if (numChunks > 0) {
            standardXMP = new String(xmpChunks.get(0), StandardCharsets.UTF_8);
            standardXMP = Utils.trimXMP(standardXMP);
            if (numChunks > 1) {
                String extendedXMP = new String(
                        mergeChunks(xmpChunks.subList(1, numChunks)),
                        StandardCharsets.UTF_8);
                extendedXMP = Utils.trimXMP(extendedXMP);
                return mergeXMPModels(standardXMP, extendedXMP);
            }
        }
        return standardXMP;
    }

    /**
     * Merges the "StandardXMP" and "ExtendedXMP" models.
     *
     * Note: certain properties that are known to be large are excluded from
     * the merge and discarded. An explanation follows:
     *
     * If the serialized form of the returned model is larger than 65502
     * bytes, the XMP will have to be split again during writing. Most XMP
     * properties are simple and it would take hundreds of them to reach this
     * length, and since there probably aren't that many, it's almost certainly
     * an embedded thumbnail or some other property from a small set of known
     * offenders that is bloating up the model.
     *
     * Large models are slower to process and deliver. 65KB is already quite
     * a lot of data and maybe even larger than the image it's going to be
     * embedded in. One of the use cases of this application is thumbnail
     * delivery, so why embed a thumbnail inside a thumbnail, especially when
     * doing so will slow down the delivery?
     *
     * The Adobe XMP Spec Part 3 recommends a procedure for writing XMP that
     * may require iterative serialization&mdash;for example, "find a large
     * property value in StandardXMP, move it into ExtendedXMP, serialize the
     * StandardXMP, check its length, find another large value, move it,
     * serialize again, check length again," etc. This may be viable for
     * Photoshop, but we are trying to be mindful of efficiency here.
     *
     * @param standardXMP Fully formed "StandardXMP" tree.
     * @param extendedXMP Fully formed "ExtendedXMP" tree.
     * @return            Merged tree.
     */
    private static String mergeXMPModels(String standardXMP,
                                         String extendedXMP) throws IOException {
        // Merge the models.
        Model model = readModel(standardXMP);
        model = model.union(readModel(extendedXMP));

        // Normalize the merged model.
        normalize(model);

        // Write the model to RDF/XML.
        try (StringWriter writer = new StringWriter()) {
            model.write(writer);
            return writer.toString();
        }
    }

    private static Model readModel(String rdfXML) {
        Model model = ModelFactory.createDefaultModel();
        try (StringReader reader = new StringReader(rdfXML)) {
            model.read(reader, null, "RDF/XML");
        }
        return model;
    }

    /**
     * Removes any {@code xmpNote:HasExtendedXMP} property per the Adobe XMP
     * Spec Part 3. Also removes other known-large properties (see {@link
     * #mergeXMPModels}).
     */
    private static void normalize(Model model) {
        for (String property : Set.of(
                Constants.EXTENDED_XMP_PREDICATE,
                "http://ns.adobe.com/xap/1.0/Thumbnails",
                "http://ns.adobe.com/xap/1.0/g/img/image",
                "http://ns.adobe.com/photoshop/1.0/History")) {
            final StmtIterator it = model.listStatements(
                    null,
                    model.createProperty(property),
                    (RDFNode) null);
            while (it.hasNext()) {
                it.removeNext();
            }
        }
    }

    /**
     * @param segmentData Segment data including marker.
     */
    static boolean isAdobeSegment(byte[] segmentData) {
        return (segmentData.length >= 12 &&
                segmentData[0] == 'A' &&
                segmentData[1] == 'd' &&
                segmentData[2] == 'o' &&
                segmentData[3] == 'b' &&
                segmentData[4] == 'e');
    }

    /**
     * @param segmentData Segment data including marker.
     */
    static boolean isEXIFSegment(byte[] segmentData) {
        return Arrays.equals(
                Constants.EXIF_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, Constants.EXIF_SEGMENT_HEADER.length));
    }

    /**
     * @param segmentData Segment data including marker.
     */
    static boolean isExtendedXMPSegment(byte[] segmentData) {
        return Arrays.equals(
                Constants.EXTENDED_XMP_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, Constants.EXTENDED_XMP_SEGMENT_HEADER.length));
    }

    /**
     * @param segmentData Segment data including marker.
     */
    static boolean isPhotoshopSegment(byte[] segmentData) {
        return Arrays.equals(
                Constants.PHOTOSHOP_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, Constants.PHOTOSHOP_SEGMENT_HEADER.length));
    }

    /**
     * @param segmentData Segment data including marker.
     */
    static boolean isStandardXMPSegment(byte[] segmentData) {
        return Arrays.equals(
                Constants.STANDARD_XMP_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, Constants.STANDARD_XMP_SEGMENT_HEADER.length));
    }

    /**
     * Merges the given list of chunks.
     *
     * @return The single chunk if the argument has only one element; or {@code
     *         null} if the argument is empty.
     */
    static byte[] mergeChunks(List<byte[]> chunks) {
        final int numChunks = chunks.size();
        if (numChunks > 1) {
            return ArrayUtils.merge(chunks);
        }
        return chunks.get(0);
    }

    /**
     * @param segment Segment data including header and length.
     * @return        Segment data excluding header and length.
     */
    static byte[] readAPP1Segment(byte[] segment) {
        byte[] data = null;
        if (isEXIFSegment(segment)) {
            final int dataLength = segment.length -
                    Constants.EXIF_SEGMENT_HEADER.length;
            data = new byte[dataLength];
            System.arraycopy(segment, Constants.EXIF_SEGMENT_HEADER.length,
                    data, 0, dataLength);
        } else if (isStandardXMPSegment(segment)) {
            // Note that XMP models > 65502 bytes will be split across multiple
            // APP1 segments. In this case, the first one (the "StandardXMP")
            // will be a fully formed tree, and will contain an
            // xmpNote:HasExtendedXMP property containing the GUID of the
            // "ExtendedXMP."
            final int dataLength = segment.length -
                    Constants.STANDARD_XMP_SEGMENT_HEADER.length;
            data = new byte[dataLength];
            System.arraycopy(segment, Constants.STANDARD_XMP_SEGMENT_HEADER.length,
                    data, 0, dataLength);
        } else if (isExtendedXMPSegment(segment)) {
            // If the ExtendedXMP is <= 65502 bytes, it will be a fully formed
            // tree; otherwise it will be split across however many more APP1
            // segments without regard to XML or even UTF-8 structure.
            // The structure of an ExtendedXMP segment is:
            // 1. 32-byte GUID, which is an ASCII MD5 digest of the full
            //    ExtendedXMP serialization
            // 2. 4-byte unsigned data length
            // 3. 4-byte unsigned offset
            // 4. ExtendedXMP data
            // GUIDs are currently ignored as we assume that it is unlikely
            // that they would ever differ.
            data = Arrays.copyOfRange(
                    segment,
                    Constants.EXTENDED_XMP_SEGMENT_HEADER.length + 32 + 4 + 4,
                    segment.length);
        }
        return data;
    }

    private Util() {}

}
