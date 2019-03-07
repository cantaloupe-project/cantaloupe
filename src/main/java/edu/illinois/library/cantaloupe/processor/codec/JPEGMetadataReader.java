package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.util.ArrayUtils;

import javax.imageio.stream.ImageInputStream;
import java.awt.color.ICC_Profile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Reads various metadata from a JPEG image.</p>
 *
 * <p>This class is very incomplete and only contains the bare minimum of
 * functionality needed by some other application components.</p>
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
         * Start Of Image, expected to be the very first marker in the stream.
         */
        SOI(0xff, 0xd8),

        /**
         * Start Of Frame for baseline DCT images.
         */
        SOF0(0xff, 0xc0),

        /**
         * Start Of Frame for extended sequential DCT images.
         */
        SOF1(0xff, 0xc1),

        /**
         * Start Of Frame for progressive DCT images.
         */
        SOF2(0xff, 0xc2),

        /**
         * Start Of Frame for lossless (sequential) images.
         */
        SOF3(0xff, 0xc3),

        /**
         * Start Of Frame for differential sequential DCT images.
         */
        SOF5(0xff, 0xc5),

        /**
         * Start Of Frame for differential progressive DCT images.
         */
        SOF6(0xff, 0xc6),

        /**
         * Start Of Frame for differential lossless (sequential) images.
         */
        SOF7(0xff, 0xc7),

        /**
         * Start Of Frame for extended sequential DCT images.
         */
        SOF9(0xff, 0xc9),

        /**
         * Start Of Frame for progressive DCT images.
         */
        SOF10(0xff, 0xca),

        /**
         * Start Of Frame for lossless (sequential) images.
         */
        SOF11(0xff, 0xcb),

        /**
         * Start Of Frame for differential sequential DCT images.
         */
        SOF13(0xff, 0xcd),

        /**
         * Start Of Frame for differential progressive DCT images.
         */
        SOF14(0xff, 0xce),

        /**
         * Start Of Frame for differential lossless (sequential) images.
         */
        SOF15(0xff, 0xcf),

        /**
         * EXIF data.
         */
        APP1(0xff, 0xe1),

        /**
         * ICC profile.
         */
        APP2(0xff, 0xe2),

        /**
         * Adobe.
         */
        APP14(0xff, 0xee),

        /**
         * Define Huffman Table marker; our effective "stop reading" marker.
         */
        DHT(0xff, 0xc4),

        /**
         * Marker not recognized by this reader, which may still be perfectly
         * valid.
         */
        UNKNOWN(0x00, 0x00);

        static Marker forBytes(int byte1, int byte2) {
            for (Marker marker : values()) {
                if (marker.byte1 == byte1 && marker.byte2 == byte2) {
                    return marker;
                }
            }
            return UNKNOWN;
        }

        private int byte1, byte2;

        Marker(int byte1, int byte2) {
            this.byte1 = byte1;
            this.byte2 = byte2;
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
     * Header immediately following an {@literal APP1} segment marker
     * indicating that the segment contains EXIF data.
     */
    private static final byte[] EXIF_SEGMENT_HEADER = "Exif\0\0".getBytes();

    /**
     * Header immediately following an {@literal APP2} segment marker
     * indicating that the segment contains an ICC profile.
     */
    private static final char[] ICC_SEGMENT_HEADER =
            "ICC_PROFILE\0".toCharArray();

    /**
     * Header immediately following an {@literal APP1} segment marker
     * indicating that the segment contains XMP data.
     */
    private static final byte[] XMP_SEGMENT_HEADER =
            "http://ns.adobe.com/xap/1.0/\0".getBytes();

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
    private byte[] exif, xmp;
    private int width, height;

    /**
     * Merges the given list of chunks, or returns the single chunk if the
     * list has only one element; or returns {@literal null} if the list is
     * empty.
     */
    private static byte[] mergeICCProfileChunks(List<byte[]> chunks) {
        final int numChunks = chunks.size();
        if (numChunks > 1) {
            return ArrayUtils.merge(chunks);
        } else if (numChunks == 1) {
            return chunks.get(0);
        }
        return null;
    }

    /**
     * @return Color transform from the {@literal APP14} segment.
     */
    public AdobeColorTransform getColorTransform() throws IOException {
        readImage();
        return colorTransform;
    }

    /**
     * @return EXIF data from the {@literal APP1} segment.
     */
    public byte[] getEXIF() throws IOException {
        readImage();
        return exif;
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

    public int getWidth() throws IOException {
        readImage();
        return width;
    }

    public int getHeight() throws IOException {
        readImage();
        return height;
    }

    /**
     * @return XMP data from the {@literal APP1} segment.
     */
    public byte[] getXMP() throws IOException {
        readImage();
        return xmp;
    }

    boolean hasAdobeSegment() throws IOException {
        readImage();
        return hasAdobeSegment;
    }

    /**
     * @param inputStream Fresh stream from which to read the image.
     */
    void setSource(ImageInputStream inputStream) {
        this.inputStream = inputStream;
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
            case SOF0:
                readSOFSegment();
                break;
            case SOF1:
                readSOFSegment();
                break;
            case SOF2:
                readSOFSegment();
                break;
            case SOF3:
                readSOFSegment();
                break;
            case SOF5:
                readSOFSegment();
                break;
            case SOF6:
                readSOFSegment();
                break;
            case SOF7:
                readSOFSegment();
                break;
            case SOF9:
                readSOFSegment();
                break;
            case SOF10:
                readSOFSegment();
                break;
            case SOF11:
                readSOFSegment();
                break;
            case SOF13:
                readSOFSegment();
                break;
            case SOF14:
                readSOFSegment();
                break;
            case SOF15:
                readSOFSegment();
                break;
            case APP1:
                readAPP1Segment();
                break;
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
     * Reads the SOFn segment, which contains image dimensions.
     */
    private void readSOFSegment() throws IOException {
        int segmentLength = readSegmentLength();
        byte[] data = read(segmentLength);
        height = ((data[1] & 0xff) << 8) | (data[2] & 0xff);
        width = ((data[3] & 0xff) << 8) | (data[4] & 0xff);
    }

    /**
     * Reads EXIF or XMP data from the APP1 segment.
     */
    private void readAPP1Segment() throws IOException {
        int segmentLength = readSegmentLength();
        byte[] data = read(segmentLength);

        // Check for EXIF.
        boolean isEXIF = true;
        for (int i = 0; i < EXIF_SEGMENT_HEADER.length; i++) {
            if (data[i] != EXIF_SEGMENT_HEADER[i]) {
                isEXIF = false;
                break;
            }
        }
        if (isEXIF) {
            final int exifLength = data.length - EXIF_SEGMENT_HEADER.length;
            exif = new byte[exifLength];
            System.arraycopy(data, EXIF_SEGMENT_HEADER.length,
                    exif, 0, exifLength);
        } else {
            // Check for XMP.
            boolean isXMP = true;
            for (int i = 0; i < XMP_SEGMENT_HEADER.length; i++) {
                if (data[i] != XMP_SEGMENT_HEADER[i]) {
                    isXMP = false;
                    break;
                }
            }
            if (isXMP) {
                final int xmpLength = data.length - XMP_SEGMENT_HEADER.length;
                xmp = new byte[xmpLength];
                System.arraycopy(data, XMP_SEGMENT_HEADER.length,
                        xmp, 0, xmpLength);
            }
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
