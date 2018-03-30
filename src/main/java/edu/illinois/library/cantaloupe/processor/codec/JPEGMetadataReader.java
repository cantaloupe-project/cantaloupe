package edu.illinois.library.cantaloupe.processor.codec;

import javax.imageio.stream.ImageInputStream;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Reads various metadata from a JPEG image.</p>
 *
 * <p>This class is very incomplete and only exists to work around some
 * limitations in the default ImageIO JPEG reader.</p>
 *
 * <p>This class does not rely on ImageIO and has no relationship to {@link
 * JPEGMetadata}.</p>
 *
 * @see <a href="http://dev.exiv2.org/projects/exiv2/wiki/The_Metadata_in_JPEG_files">
 *     The Metadata in JPEG Files</a>
 * @see <a href="http://www.vip.sugovica.hu/Sardi/kepnezo/JPEG%20File%20Layout%20and%20Format.htm">
 *     JPEG File Layout and Format</a>
 * @author Alex Dolski UIUC
 */
final class JPEGMetadataReader {

    enum Marker {

        /**
         * Start-of-image, expected to be the very first marker in the stream.
         */
        SOI,

        APP2, APP14,

        /**
         * Define Huffman Table marker; our effective "stop reading" marker.
         */
        DHT,

        /**
         * Marker not recognized by this reader, which may still be perfectly
         * valid.
         */
        UNKNOWN;

        static Marker forBytes(int byte1, int byte2) {
            if (byte1 == 0xFF && byte2 == 0xD8) {
                return SOI;
            } else if (byte1 == 0xFF && byte2 == 0xE2) {
                return APP2;
            } else if (byte1 == 0xFF && byte2 == 0xEE) {
                return APP14;
            } else if (byte1 == 0xFF && byte2 == 0xC4) {
                return DHT;
            }
            return UNKNOWN;
        }

    }

    /**
     * Represents a color transform in an Adobe APP14 marker segment.
     */
    enum AdobeColorTransform {
        YCBCR(1), YCCK(2), UNKNOWN(0);

        private int app14Value;

        private static AdobeColorTransform forAPP14Value(int value) {
            for (AdobeColorTransform transform : AdobeColorTransform.values()) {
                if (transform.getAPP14Value() == value) {
                    return transform;
                }
            }
            return null;
        }

        AdobeColorTransform(int app14Value) {
            this.app14Value = app14Value;
        }

        private int getAPP14Value() {
            return app14Value;
        }
    }

    /**
     * Header immediately following an {@literal APP2} segment marker
     * indicating that the segment contains an ICC profile.
     */
    private static final char[] ICC_SEGMENT_HEADER =
            "ICC_PROFILE\0".toCharArray();

    /**
     * Set to {@literal true} once reading begins.
     */
    private boolean isReadAttempted;

    /**
     * Stream from which to read the image data.
     */
    private ImageInputStream inputStream;

    private AdobeColorTransform colorTransform;

    private boolean hasAdobeSegment;

    private final List<byte[]> iccProfileChunks = new ArrayList<>();

    /**
     * Merges the given list of chunks, or returns the single chunk if the
     * list has only one element; or returns {@literal null} if the list is
     * empty.
     */
    private static byte[] mergeICCProfileChunks(List<byte[]> chunks) {
        final int numChunks = chunks.size();
        if (numChunks > 1) {
            // Merge the chunks.
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                for (byte[] chunk : chunks) {
                    os.write(chunk);
                }
                os.flush();
                return os.toByteArray();
            } catch (IOException ignore) {
                // ByteArrayOutputStream is not really going to throw this.
            }
        } else if (numChunks == 1) {
            return chunks.get(0);
        }
        return null;
    }

    /**
     * @param inputStream Fresh stream from which to read the image.
     */
    void setSource(ImageInputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Reads the color transform from the {@literal APP14} marker segment.
     */
    public AdobeColorTransform getColorTransform() throws IOException {
        readImage();
        return colorTransform;
    }

    /**
     * <p>Reads an embedded ICC profile from an {@literal APP2} segment.</p>
     *
     * <p>N.B.: ICC profiles can also be extracted from {@link
     * javax.imageio.metadata.IIOMetadata} objects, but as of JDK 9, the
     * default ImageIO JPEG reader does not support JPEGs with CMYK color and
     * will throw an exception before the metadata can be read.</p>
     *
     * @return ICC profile, or {@literal null} if one is not contained in the
     *         stream.
     * @see <a href="http://www.color.org/specification/ICC1v43_2010-12.pdf">
     *     ICC Specification ICC.1: 2010</a> Annex B.4
     */
    ICC_Profile getICCProfile() throws IOException {
        readImage();
        byte[] data = mergeICCProfileChunks(iccProfileChunks);
        return (data != null) ? ICC_Profile.getInstance(data) : null;
    }

    boolean hasAdobeSegment() throws IOException {
        readImage();
        return hasAdobeSegment;
    }

    /**
     * <p>Main reading method. Reads image info into instance variables. May
     * call other private reading methods that will all expect {@link
     * #inputStream} to be pre-positioned for reading.</p>
     *
     * <p>Safe to call multiple times.</p>
     */
    private void readImage() throws IOException {
        if (isReadAttempted) {
            return;
        } else if (inputStream == null) {
            throw new IllegalStateException("Source not set");
        } else if (!Marker.SOI.equals(
                Marker.forBytes(inputStream.read(), inputStream.read()))) {
            throw new IOException("Invalid SOI marker (is this a JPEG?)");
        }

        isReadAttempted = true;

        while (readSegment() != -1) {
            // keep reading
        }
    }

    /**
     * @return {@literal -1} when there are no more segments to read; some
     *         other value otherwise.
     */
    private int readSegment() throws IOException {
        switch (Marker.forBytes(inputStream.read(), inputStream.read())) {
            case APP2:
                readAPP2Segment();
                break;
            case APP14:
                readAPP14Segment();
                break;
            case DHT:
                return -1;
            default:
                skipSegment();
                break;
        }
        return 0;
    }

    private int readSegmentLength() throws IOException {
        return 256 * inputStream.read() + inputStream.read() - 2;
    }

    private void skipSegment() throws IOException {
        int segmentLength = readSegmentLength();
        inputStream.skipBytes(segmentLength);
    }

    /**
     * <p>ICC profiles are packed into {@literal APP2} segments. Profiles
     * larger than 65,533 bytes are split into chunks which appear in
     * sequential segments. The segment marker is immediately followed (in
     * order) by:</p>
     *
     * <ol>
     *     <li>{@link #ICC_SEGMENT_HEADER}</li>
     *     <li>The sequence number of the chunk (one byte)</li>
     *     <li>The total number of chunks (one byte)</li>
     *     <li>Profile data</li>
     * </ol>
     */
    private void readAPP2Segment() throws IOException {
        int segmentLength = readSegmentLength();
        byte[] data = read(segmentLength);

        if (data[0] == 'I' && data[1] == 'C' && data[2] == 'C') {
            final int headerLength = ICC_SEGMENT_HEADER.length + 2; // +2 for chunk sequence and chunk count
            data = Arrays.copyOfRange(data, headerLength, segmentLength);
            iccProfileChunks.add(data);
        }
    }

    /**
     * May contain an Adobe segment, in which case the segment marker is
     * immediately followed by the string {@literal Adobe}.
     */
    private void readAPP14Segment() throws IOException {
        int segmentLength = readSegmentLength();
        byte[] data = read(segmentLength);

        if (data.length >= 12 && data[0] == 'A' && data[1] == 'd' &&
                data[2] == 'o' && data[3] == 'b' && data[4] == 'e') {
            hasAdobeSegment = true;
            colorTransform = AdobeColorTransform.forAPP14Value(data[11] & 0xFF);
        }
    }

    private byte[] read(int length) throws IOException {
        byte[] data = new byte[length];
        int n, offset = 0;
        while ((n = inputStream.read(
                data, offset, data.length - offset)) < offset) {
            offset += n;
        }
        return data;
    }

}
