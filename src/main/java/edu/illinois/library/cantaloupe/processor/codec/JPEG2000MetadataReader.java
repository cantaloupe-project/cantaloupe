package edu.illinois.library.cantaloupe.processor.codec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.Arrays;

/**
 * <p>Reads various metadata from a JPEG2000 image.</p>
 *
 * <p>This class is very incomplete and only exists to get some basic metadata
 * that can't be obtained easily via ImageIO. It is also naive in that it reads
 * only the main header.</p>
 *
 * @author Alex Dolski UIUC
 */
final class JPEG2000MetadataReader {

    private enum Marker {

        /**
         * Start of codestream.
         */
        SOC(0xFF, 0x4F),

        /**
         * Image and tile size.
         */
        SIZ(0xFF, 0x51),

        /**
         * Coding style default.
         */
        COD(0xFF, 0x52),

        /**
         * Coding style component.
         */
        COC(0xFF, 0x53),

        /**
         * Region-of-interest.
         */
        RGN(0xFF, 0x5E),

        /**
         * Quantization default.
         */
        QCD(0xFF, 0x5C),

        /**
         * Quantization component.
         */
        QCC(0xFF, 0x5D),

        /**
         * Progression order change.
         */
        POC(0xFF, 0x5F),

        /**
         * Pointer marker segments.
         */
        TLM(0xFF, 0x55),

        /**
         * Tile-part lengths.
         */
        PLM(0xFF, 0x57),

        /**
         * Packet length, tile-part header.
         */
        PPM(0xFF, 0x60),

        /**
         * Component registration.
         */
        CRG(0xFF, 0x63),

        /**
         * Comment.
         */
        COM(0xFF, 0x64),

        /**
         * Some other marker, which may be perfectly legitimate but is not
         * understood by this reader.
         */
        UNKNOWN(0x00, 0x00);

        private static Marker forBytes(int byte1, int byte2) {
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

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEG2000MetadataReader.class);

    private static final byte[] JP2_SIGNATURE = new byte[] { 0x00, 0x00, 0x00,
            0x0c, 0x6a, 0x50, 0x20, 0x20, 0x0d, 0x0a, (byte) 0x87, 0x0a };

    /**
     * Set to {@literal true} once reading begins.
     */
    private boolean isReadAttempted;

    /**
     * Stream from which to read the image data.
     */
    private ImageInputStream inputStream;

    private int width, height, tileWidth, tileHeight,
            componentSize, numComponents, numDecompositionLevels;

    /**
     * @param inputStream Fresh stream from which to read the image.
     */
    void setSource(ImageInputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * @return Component/sample size.
     */
    int getComponentSize() throws IOException {
        readImage();
        return componentSize;
    }

    /**
     * @return Height of the image grid.
     */
    int getHeight() throws IOException {
        readImage();
        return height;
    }

    /**
     * @return Number of components/bands.
     */
    int getNumComponents() throws IOException {
        readImage();
        return numComponents;
    }

    /**
     * @return Number of available decomposition levels, which will be
     *         one less than the number of available resolutions. Note that
     *         contrary to the spec, only the main header {@link Marker#COD}
     *         and {@link Marker#COC} segments are consulted, and not any tile-
     *         part segments.
     */
    int getNumDecompositionLevels() throws IOException {
        readImage();
        return numDecompositionLevels;
    }

    /**
     * @return Height of a reference tile, or the full image height if the
     *         image is not tiled.
     */
    int getTileHeight() throws IOException {
        readImage();
        return tileHeight;
    }

    /**
     * @return Width of a reference tile, or the full image width if the image
     *         is not tiled.
     */
    int getTileWidth() throws IOException {
        readImage();
        return tileWidth;
    }

    /**
     * @return Width of the image grid.
     */
    int getWidth() throws IOException {
        readImage();
        return width;
    }

    @Override
    public String toString() {
        return String.format("size: %dx%d; tileSize: %dx%d; %d components; " +
                        "%d bpc; %d DWT levels",
                width, height, tileWidth, tileHeight, numComponents,
                componentSize, numDecompositionLevels);
    }

    /**
     * <p>Main reading method. Reads image info into instance variables. May
     * call other private reading methods that will all expect {@link
     * #inputStream} to be pre-positioned for reading.</p>
     *
     * <p>JPEG2000 files are based on a box structure, and several of the boxes
     * contain various metadata that we are interested in. But, we ignore all
     * of them except the codestream box, because we need to read the DWT level
     * count, which is only present in the codestream.</p>
     *
     * <p>It's safe to call this method multiple times.</p>
     */
    private void readImage() throws IOException {
        if (isReadAttempted) {
            return;
        } else if (inputStream == null) {
            throw new IllegalStateException("Source not set");
        }

        checkSignature();

        isReadAttempted = true;

        // Scan for the Contiguous Codestream box. This isn't very efficient,
        // but it's easier than parsing a potentially complicated box structure.
        int b, b1 = 0, b2 = 0, b3 = 0;
        while ((b = inputStream.read()) != -1) {
            if (b3 == 0x6a && b2 == 0x70 && b1 == 0x32 && b == 0x63) {
                break;
            }
            b3 = b2; b2 = b1; b1 = b;
        }

        // Find the codestream SOC marker and position the stream immediately
        // after it.
        b1 = 0;
        while ((b = inputStream.read()) != -1) {
            if (Marker.SOC.equals(Marker.forBytes(b1, b))) {
                break;
            }
            b1 = b;
        }

        while (readSegment() != -1) {
            // keep reading
        }

        LOGGER.debug("{}", this);
    }

    private void checkSignature() throws IOException {
        byte[] bytes = read(JP2_SIGNATURE.length);

        if (!Arrays.equals(JP2_SIGNATURE, bytes)) {
            String hexStr = DatatypeConverter.printHexBinary(bytes);
            throw new IOException("Invalid signature: " + hexStr +
                    " (is this a JP2?)");
        }
    }

    /**
     * @return {@literal -1} if there are no more relevant segments to read;
     *         some other value otherwise.
     */
    private int readSegment() throws IOException {
        // The SIZ segment must come first, but the rest can appear in any
        // order.
        int status = 0;
        switch (Marker.forBytes(inputStream.read(), inputStream.read())) {
            case SIZ:
                readSIZSegment();
                break;
            case COD:
                readCODSegment();
                break;
            case COC:
                readCOCSegment();
                break;
            case UNKNOWN:
                status = -1;
                break;
            default:
                skipSegment();
                break;
        }
        return status;
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
        int n, offset = 0;
        while ((n = inputStream.read(
                data, offset, data.length - offset)) < offset) {
            offset += n;
        }
        return data;
    }

}
