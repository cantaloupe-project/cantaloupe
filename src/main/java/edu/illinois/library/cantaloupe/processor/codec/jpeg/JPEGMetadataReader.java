package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import javax.imageio.stream.ImageInputStream;
import java.awt.color.ICC_Profile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Reads various metadata from a JPEG image.</p>
 *
 * @see <a href="http://dev.exiv2.org/projects/exiv2/wiki/The_Metadata_in_JPEG_files">
 *     The Metadata in JPEG Files</a>
 * @author Alex Dolski UIUC
 */
public final class JPEGMetadataReader {

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
    private final List<byte[]> xmpChunks        = new ArrayList<>();
    private byte[] exif, iptc;
    private int width, height;

    private transient String xmp;

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
     * javax.imageio.metadata.IIOMetadata} objects, but as of JDK 11, the
     * JDK Image I/O JPEG reader does not support JPEGs with CMYK color and
     * will throw an exception before the metadata can be read.</p>
     *
     * @return ICC profile, or {@code null} if one is not contained in the
     *         stream.
     * @see <a href="http://www.color.org/specification/ICC1v43_2010-12.pdf">
     *     ICC Specification ICC.1: 2010</a> Annex B.4
     */
    public ICC_Profile getICCProfile() throws IOException {
        readImage();
        if (!iccProfileChunks.isEmpty()) {
            byte[] data = Util.mergeChunks(iccProfileChunks);
            return ICC_Profile.getInstance(data);
        }
        return null;
    }

    /**
     * @return IPTC data from the {@literal APP13} segment.
     */
    public byte[] getIPTC() throws IOException {
        readImage();
        return iptc;
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
     * @return Fully formed XMP tree from one or more {@literal APP1} segments.
     *         {@code rdf:RDF} is the outermost element and some properties
     *         (especially large ones) may be removed.
     */
    public String getXMP() throws IOException {
        readImage();
        if (xmp == null) {
            xmp = Util.assembleXMP(xmpChunks);
        }
        return xmp;
    }

    public boolean hasAdobeSegment() throws IOException {
        readImage();
        return hasAdobeSegment;
    }

    /**
     * @param inputStream Fresh stream from which to read the image.
     */
    public void setSource(ImageInputStream inputStream) {
        this.inputStream     = inputStream;
        this.xmp             = null;
        this.isReadAttempted = false;
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

        //noinspection StatementWithEmptyBody
        while (readSegment() != -1) {
        }
    }

    /**
     * @return {@literal -1} when there are no more segments to read; some
     *         other value otherwise.
     */
    private int readSegment() throws IOException {
        switch (Marker.forBytes(inputStream.read(), inputStream.read())) {
            case SOF0:
            case SOF1:
            case SOF2:
            case SOF3:
            case SOF5:
            case SOF6:
            case SOF7:
            case SOF9:
            case SOF10:
            case SOF11:
            case SOF13:
            case SOF14:
            case SOF15:
                readSOFSegment();
                break;
            case APP1:
                readAPP1Segment();
                break;
            case APP2:
                readAPP2Segment();
                break;
            case APP13:
                readAPP13Segment();
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
        byte[] segment = read(segmentLength);
        byte[] data = Util.readAPP1Segment(segment);

        if (Util.isEXIFSegment(segment)) {
            exif = data;
        } else if (Util.isStandardXMPSegment(segment) ||
                Util.isExtendedXMPSegment(segment)) {
            xmpChunks.add(data);
        }
    }

    /**
     * <p>ICC profiles are packed into {@literal APP2} segments. Profiles
     * larger than 65,533 bytes are split into chunks which appear in
     * sequential segments. The segment marker is immediately followed (in
     * order) by:</p>
     *
     * <ol>
     *     <li>{@link Constants#ICC_SEGMENT_HEADER}</li>
     *     <li>The sequence number of the chunk (one byte)</li>
     *     <li>The total number of chunks (one byte)</li>
     *     <li>Profile data</li>
     * </ol>
     */
    private void readAPP2Segment() throws IOException {
        int segmentLength = readSegmentLength();
        byte[] data = read(segmentLength);

        if (data[0] == 'I' && data[1] == 'C' && data[2] == 'C') {
            final int headerLength = Constants.ICC_SEGMENT_HEADER.length + 2; // +2 for chunk sequence and chunk count
            data = Arrays.copyOfRange(data, headerLength, segmentLength);
            iccProfileChunks.add(data);
        }
    }

    private void readAPP13Segment() throws IOException {
        int segmentLength = readSegmentLength();
        byte[] data = read(segmentLength);

        if (Util.isPhotoshopSegment(data)) {
            // Check for IPTC data.
            if (data[14] == '8' && data[15] == 'B' && data[16] == 'I' &&
                    data[17] == 'M' && data[18] == 0x04 && data[19] == 0x04) {
                iptc = Arrays.copyOfRange(data, 26, data.length);
            }
        }
    }

    private void readAPP14Segment() throws IOException {
        int segmentLength = readSegmentLength();
        byte[] data = read(segmentLength);

        if (Util.isAdobeSegment(data)) {
            hasAdobeSegment = true;
            colorTransform = AdobeColorTransform.forAPP14Value(data[11] & 0xff);
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
