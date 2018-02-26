package edu.illinois.library.cantaloupe.processor.imageio;

import javax.imageio.stream.ImageInputStream;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
 * @see <a href="http://www.vip.sugovica.hu/Sardi/kepnezo/JPEG%20File%20Layout%20and%20Format.htm">
 *     JPEG File Layout and Format</a>
 * @author Alex Dolski UIUC
 */
final class JPEGMetadataReader {

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
     * Header immediately following the {@literal APP2} segment marker
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

    private byte[] iccProfile;

    /**
     * @return Whether the given two bytes mark the start-of-image (SOI), the
     *         very first marker in the file.
     */
    private static boolean isSOIMarker(int byte1, int byte2) {
        return (byte1 == 0xFF && byte2 == 0xD8);
    }

    /**
     * @return Whether the given two bytes mark an {@literal APP2} segment.
     */
    private static boolean isAPP2Marker(int byte1, int byte2) {
        return (byte1 == 0xFF && byte2 == 0xE2);
    }

    /**
     * @return Whether the given two bytes mark an {@literal APP14} segment.
     */
    private static boolean isAPP14Marker(int byte1, int byte2) {
        return (byte1 == 0xFF && byte2 == 0xEE);
    }

    /**
     * The Define Quantization Table (DQT) segment is next after the application
     * segments. Since there is no marker for end-of-segments, and there is no
     * guarantee of segment order, this is what will be searched for instead.
     *
     * @return Whether the given two bytes mark the start of the DQT segment.
     */
    private static boolean isDQTMarker(int byte1, int byte2) {
        return (byte1 == 0xFF && byte2 == 0xDB);
    }

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
     * <p>Reads an embedded ICC profile.</p>
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
        return (iccProfile != null) ?
                ICC_Profile.getInstance(iccProfile) : null;
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
        if (!isReadAttempted) {
            if (inputStream == null) {
                throw new IllegalStateException("Source not set");
            } else if (!isSOIMarker(inputStream.read(), inputStream.read())) {
                throw new IOException("Invalid SOI marker (is this a JPEG?)");
            }

            isReadAttempted = true;

            final List<byte[]> iccProfileChunks = new ArrayList<>();

            int previousByte = 0, b;
            while ((b = inputStream.read()) != -1) {
                if (isAPP2Marker(previousByte, b)) {
                    byte[] chunkData = readICCSegment();
                    if (chunkData != null) {
                        iccProfileChunks.add(chunkData);
                    }
                } else if (isAPP14Marker(previousByte, b)) {
                    readAdobeSegment();
                } else if (isDQTMarker(previousByte, b)) {
                    // Done reading.
                    break;
                }
                previousByte = b;
            }

            iccProfile = mergeICCProfileChunks(iccProfileChunks);
        }
    }

    private int readSegmentLength() throws IOException {
        return 256 * inputStream.read() + inputStream.read() - 2;
    }

    /**
     * The Adobe segment appears in an {@literal APP14} segment. The segment
     * marker is immediately followed by the string {@literal Adobe}.
     */
    private void readAdobeSegment() throws IOException {
        final int segmentLength = readSegmentLength();
        final byte[] data = new byte[segmentLength];

        inputStream.read(data, 0, segmentLength);

        if (data.length >= 12 && data[0] == 'A' && data[1] == 'd' &&
                data[2] == 'o' && data[3] == 'b' && data[4] == 'e') {
            hasAdobeSegment = true;
            colorTransform = AdobeColorTransform.forAPP14Value(data[11] & 0xFF);
        }
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
     *
     * @return Segment data, or {@literal null} if none was found.
     */
    private byte[] readICCSegment() throws IOException {
        final int segmentLength = readSegmentLength();
        final int headerLength = ICC_SEGMENT_HEADER.length + 2; // +2 for chunk sequence and chunk count
        final int dataLength = segmentLength - headerLength;

        // Read the segment header which we don't care about.
        for (int i = 0; i < headerLength; i++) {
            inputStream.read();
        }

        // Read the data.
        final byte[] buffer = new byte[dataLength];
        inputStream.read(buffer, 0, dataLength);

        return buffer;
    }

}
