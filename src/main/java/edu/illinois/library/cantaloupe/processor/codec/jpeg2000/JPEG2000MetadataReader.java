package edu.illinois.library.cantaloupe.processor.codec.jpeg2000;

import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * <p>Reads various information from a JPEG2000 box structure. Only used for
 * reading some relevant info that can't be obtained via Image I/O, as we would
 * rather not depend on the JPEG2000 plugin contained within the aging and
 * unsupported JAI Image I/O Tools.</p>
 *
 * <p>JPEG2000 files are structured into a series of boxes (which may be
 * nested, although this reader doesn't need to read any of those). First, the
 * JPEG2000 signature box is read to check validity. Then, the box structure is
 * scanned for a UUID box containing XMP data, or a Contiguous Codestream box.
 * Unrecognized boxes are skipped.</p>
 *
 * @author Alex Dolski UIUC
 */
public final class JPEG2000MetadataReader implements AutoCloseable {

    /**
     * JP2 box (only the ones this reader cares about).
     *
     * See: ISO/IEC 15444-1-2004 sec. 1.2: File organization (p. 133)
     */
    private enum Box {

        CONTIGUOUS_CODESTREAM(new byte[] { 0x6a, 0x70, 0x32, 0x63 }),

        UUID(new byte[] { 0x75, 0x75, 0x69, 0x64 }),

        /**
         * Some other box, which may be perfectly legitimate but is not
         * understood by this reader.
         */
        UNKNOWN(new byte[] { 0x00, 0x00, 0x00, 0x00 });

        private static Box forBytes(byte[] fourBytes) {
            return Arrays.stream(values())
                    .filter(v -> Arrays.equals(v.bytes, fourBytes))
                    .findFirst()
                    .orElse(UNKNOWN);
        }

        private final byte[] bytes;

        Box(byte[] bytes) {
            this.bytes = bytes;
        }
    }

    /**
     * Codestream segment marker (only the ones this reader cares about).
     *
     * See: ISO/IEC 15444-1-2004 sec. A.1: Markers, marker segments, and
     * headers (p. 12)
     */
    private enum SegmentMarker {

        /**
         * Start of codestream.
         */
        SOC(new byte[] { (byte) 0xff, 0x4f }),

        /**
         * Image and tile size.
         */
        SIZ(new byte[] { (byte) 0xff, 0x51 }),

        /**
         * Coding style default.
         */
        COD(new byte[] { (byte) 0xff, 0x52 }),

        /**
         * Coding style component.
         */
        COC(new byte[] { (byte) 0xff, 0x53 }),

        /**
         * Some other marker, which may be perfectly legitimate but is not
         * understood by this reader.
         */
        UNKNOWN(new byte[] { 0x00, 0x00 });

        private static SegmentMarker forBytes(byte[] twoBytes) {
            return Arrays.stream(values())
                    .filter(v -> Arrays.equals(v.bytes, twoBytes))
                    .findFirst()
                    .orElse(UNKNOWN);
        }

        private final byte[] bytes;

        SegmentMarker(byte[] bytes) {
            this.bytes = bytes;
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEG2000MetadataReader.class);

    private static final byte[] JP2_SIGNATURE = new byte[] {
            0x00, 0x00, 0x00, 0x0c, 0x6a, 0x50, 0x20, 0x20, 0x0d, 0x0a,
            (byte) 0x87, 0x0a };

    private static final byte[] EXIF_BOX_UUID = {
            0x4a, 0x70, 0x67, 0x54, 0x69, 0x66, 0x66, 0x45,
            0x78, 0x69, 0x66, 0x2d, 0x3e, 0x4a, 0x50, 0x32};
    private static final byte[] IPTC_BOX_UUID = {
            (byte) 0x33, (byte) 0xc7, (byte) 0xa4, (byte) 0xd2, (byte) 0xb8,
            0x1d, 0x47, 0x23, (byte) 0xa0, (byte) 0xba, (byte) 0xf1,
            (byte) 0xa3, (byte) 0xe0, (byte) 0x97, (byte) 0xad, 0x38};
    private static final byte[] XMP_BOX_UUID = {
            (byte) 0xbe, 0x7a, (byte) 0xcf, (byte) 0xcb, (byte) 0x97,
            (byte) 0xa9, 0x42, (byte) 0xe8, (byte) 0x9c, 0x71, (byte) 0x99,
            (byte) 0x94, (byte) 0x91, (byte) 0xe3, (byte) 0xaf, (byte) 0xac};

    /**
     * Set to {@literal true} once reading begins.
     */
    private boolean isReadAttempted;

    /**
     * Stream from which to read the image data.
     *
     * We use an {@link ImageInputStream} rather than an {@link
     * java.io.InputStream} for its {@link ImageInputStream#skipBytes} methods,
     * which, for some implementations, may be very efficient.
     */
    private ImageInputStream inputStream;

    private int width, height, tileWidth, tileHeight,
            componentSize, numComponents, numDecompositionLevels;

    private byte[] exif, iptc;
    private String xmp;

    @Override
    public void close() {
        IOUtils.closeQuietly(inputStream);
    }

    /**
     * @param inputStream Fresh stream from which to read the image.
     */
    public void setSource(ImageInputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * @return Component/sample size.
     */
    public int getComponentSize() throws IOException {
        readData();
        return componentSize;
    }

    /**
     * @return EXIF data from a UUID box.
     */
    public byte[] getEXIF() throws IOException {
        readData();
        return exif;
    }

    /**
     * @return Height of the image grid.
     */
    public int getHeight() throws IOException {
        readData();
        return height;
    }

    /**
     * @return IPTC data from a UUID box.
     */
    public byte[] getIPTC() throws IOException {
        readData();
        return iptc;
    }

    /**
     * @return Number of components/bands.
     */
    public int getNumComponents() throws IOException {
        readData();
        return numComponents;
    }

    /**
     * @return Number of available decomposition (DWT) levels, which will be
     *         one less than the number of available resolutions. Note that
     *         contrary to the spec, only the codestream's main {@link
     *         SegmentMarker#COD} and {@link SegmentMarker#COC} segments are
     *         consulted, and not any tile-part segments, but these are rare in
     *         the wild.
     */
    public int getNumDecompositionLevels() throws IOException {
        readData();
        return numDecompositionLevels;
    }

    /**
     * @return Height of a reference tile, or the full image height if the
     *         image is not tiled.
     */
    public int getTileHeight() throws IOException {
        readData();
        return tileHeight;
    }

    /**
     * @return Width of a reference tile, or the full image width if the image
     *         is not tiled.
     */
    public int getTileWidth() throws IOException {
        readData();
        return tileWidth;
    }

    /**
     * @return Width of the image grid.
     */
    public int getWidth() throws IOException {
        readData();
        return width;
    }

    /**
     * @return XMP string from a UUID box.
     */
    public String getXMP() throws IOException {
        readData();
        return xmp;
    }

    @Override
    public String toString() {
        return String.format("[size: %dx%d] [tileSize: %dx%d] [%d components] " +
                        "[%d bits/component] [%d DWT levels] [XMP? %b]",
                width, height, tileWidth, tileHeight, numComponents,
                componentSize, numDecompositionLevels, (xmp != null));
    }

    /**
     * <p>Main reading method. Reads image info into instance variables. May
     * call other private reading methods that will all expect {@link
     * #inputStream} to be pre-positioned.</p>
     *
     * <p>It's safe to call this method multiple times.</p>
     */
    private void readData() throws IOException {
        if (isReadAttempted) {
            return;
        } else if (inputStream == null) {
            throw new IllegalStateException("Source not set");
        }

        inputStream.mark();
        byte[] bytes = read(JP2_SIGNATURE.length);
        if (!Arrays.equals(JP2_SIGNATURE, bytes)) {
            String hexStr = DatatypeConverter.printHexBinary(bytes);
            throw new SourceFormatException("Invalid signature: " + hexStr +
                    " (is this a JP2?)");
        }
        inputStream.reset();

        final Stopwatch watch = new Stopwatch();

        while (readBox() != -1) {
            // Read boxes.
            isReadAttempted = true;
        }

        LOGGER.debug("Read in {}: {}", watch, this);
    }

    private int readBox() throws IOException {
        long dataLength = readInt(); // Read the integer box length (LBox).
        byte[] tbox = read(4);       // Read the box type (TBox).

        // If LBox == 1, then the length is actually contained in XLBox, which
        // is an 8-byte long immediately following TBox.
        if (dataLength == 1) {
            dataLength = readLong() - 16;
        } else {
            dataLength -= 8;
        }

        switch (Box.forBytes(tbox)) {
            case UUID:
                readUUIDBox(dataLength);
                return 0;
            case CONTIGUOUS_CODESTREAM:
                readContiguousCodestreamBox();
                return -1;
            default:
                skipBox(dataLength);
                return 0;
        }
    }

    private void skipBox(long dataLength) throws IOException {
        inputStream.skipBytes(dataLength);
    }

    private void readUUIDBox(long dataLength) throws IOException {
        byte[] uuid = read(16);
        // A UUID box can contain any kind of data, signified by its UUID.
        if (Arrays.equals(uuid, EXIF_BOX_UUID)) {
            exif = read((int) dataLength - 16);
        } else if (Arrays.equals(uuid, XMP_BOX_UUID)) {
            byte[] data = new byte[(int)dataLength  - 16];
            inputStream.readFully(data,  0, (int) dataLength - 16);
            xmp = new String(data, StandardCharsets.UTF_8);
            xmp = xmp.substring(xmp.indexOf("<rdf:RDF "),
                    xmp.indexOf("</rdf:RDF>") + 10);
        } else if (Arrays.equals(uuid, IPTC_BOX_UUID)) {
            iptc = read((int) dataLength - 16);
        } else {
            inputStream.skipBytes(dataLength - 16);
        }
    }

    private void readContiguousCodestreamBox() throws IOException {
        //noinspection StatementWithEmptyBody
        while (readSegment() != -1) {
        }
    }

    /**
     * @return {@literal -1} if there are no more relevant segments to read;
     *         some other value otherwise.
     */
    private int readSegment() throws IOException {
        // The SOC marker comes first, then the SIZ segment. The rest can
        // appear in any order.
        byte[] bytes = read(2);
        switch (SegmentMarker.forBytes(bytes)) {
            case SOC:
                return 0;
            case SIZ:
                readSIZSegment();
                return 0;
            case COD:
                readCODSegment();
                return 0;
            case COC:
                readCOCSegment();
                return 0;
            case UNKNOWN:
                return -1;
            default:
                skipSegment();
                return 0;
        }
    }

    private int readSegmentLength() throws IOException {
        byte[] bytes = read(2);
        return ((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff) - 2;
    }

    private void skipSegment() throws IOException {
        final int segmentLength = readSegmentLength();
        inputStream.skipBytes(segmentLength);
    }

    private void readSIZSegment() throws IOException {
        final int segmentLength = readSegmentLength();

        // Read the segment data.
        byte[] bytes = read(segmentLength);

        // Read the width (Xsiz).
        width = ((bytes[2] & 0xff) << 24) |
                ((bytes[3] & 0xff) << 16) |
                ((bytes[4] & 0xff) << 8) |
                (bytes[5] & 0xff);

        // Read the height (Ysiz).
        height = ((bytes[6] & 0xff) << 24) |
                 ((bytes[7] & 0xff) << 16) |
                 ((bytes[8] & 0xff) << 8) |
                 (bytes[9] & 0xff);

        // Read the reference tile width (XTsiz).
        tileWidth = ((bytes[18] & 0xff) << 24) |
                ((bytes[19] & 0xff) << 16) |
                ((bytes[20] & 0xff) << 8) |
                (bytes[21] & 0xff);

        // Read the reference tile height (YTsiz).
        tileHeight = ((bytes[22] & 0xff) << 24) |
                ((bytes[23] & 0xff) << 16) |
                ((bytes[24] & 0xff) << 8) |
                (bytes[25] & 0xff);

        // Read the number of components (Csiz).
        numComponents = ((bytes[34] & 0xff) << 8) | (bytes[35] & 0xff);

        // Read the component size (Ssiz).
        componentSize = (bytes[36] & 0xff) + 1;
    }

    private void readCODSegment() throws IOException {
        final int segmentLength = readSegmentLength();
        byte[] bytes = read(segmentLength);

        // Read the number of decomposition levels (SPcod byte 0).
        numDecompositionLevels = bytes[5] & 0xff;
    }

    private void readCOCSegment() throws IOException {
        final int segmentLength = readSegmentLength();
        byte[] bytes = read(segmentLength);

        // Read the number of decomposition levels (SPcoc byte 0).
        // This overrides the same value in the COD segment.
        numDecompositionLevels = bytes[5] & 0xff;
    }

    private byte[] read(int length) throws IOException {
        byte[] data = new byte[length];
        inputStream.readFully(data);
        return data;
    }

    private int readInt() throws IOException {
        byte[] bytes = read(4);
        return ((bytes[0] & 0xff) << 24) |
                ((bytes[1] & 0xff) << 16) |
                ((bytes[2] & 0xff) << 8) |
                (bytes[3] & 0xff);
    }

    private long readLong() throws IOException {
        byte[] bytes = read(8);
        return ((bytes[0] & 0xffL) << 56) |
                ((bytes[1] & 0xffL) << 48) |
                ((bytes[2] & 0xffL) << 40) |
                ((bytes[3] & 0xffL) << 32) |
                ((bytes[4] & 0xffL) << 24) |
                ((bytes[5] & 0xffL) << 16) |
                ((bytes[6] & 0xffL) << 8) |
                (bytes[7] & 0xffL);
    }

}
