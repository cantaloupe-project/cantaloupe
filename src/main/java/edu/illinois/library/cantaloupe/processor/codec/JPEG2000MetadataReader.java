package edu.illinois.library.cantaloupe.processor.codec;

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

    private static boolean isCODMarker(int byte1, int byte2) {
        return (byte1 == 0xFF && byte2 == 0x52);
    }

    private static boolean isSIZMarker(int byte1, int byte2) {
        return (byte1 == 0xFF && byte2 == 0x51);
    }

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
     * @return Number of decomposition levels, a.k.a. DWT levels. This will be
     *         one less than the number of available resolutions.
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
            }

            checkSignature();

            isReadAttempted = true;

            int previousByte = 0, b;
            while ((b = inputStream.read()) != -1) {
                if (isSIZMarker(previousByte, b)) {
                    break;
                }
                previousByte = b;
            }

            readSIZSegment();
            if (isCODMarker(inputStream.read(), inputStream.read())) {
                readCODSegment();
            } else {
                throw new IOException("Unable to read COD segment");
            }
        }
    }

    private void checkSignature() throws IOException {
        final byte[] bytes = new byte[JP2_SIGNATURE.length];
        inputStream.read(bytes);

        if (!Arrays.equals(JP2_SIGNATURE, bytes)) {
            String hexStr = DatatypeConverter.printHexBinary(bytes);
            throw new IOException("Invalid signature: " + hexStr +
                    " (is this a JP2?)");
        }
    }

    private void readSIZSegment() throws IOException {
        byte[] bytes = new byte[2];

        // Read the segment length.
        inputStream.read(bytes);
        final int segmentLength = ((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff) - 2;

        // Read the segment data.
        bytes = new byte[segmentLength];
        inputStream.read(bytes);

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
        byte[] bytes = new byte[2];

        // Read the segment length.
        inputStream.read(bytes);
        final int segmentLength = ((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff) - 2;

        // Read the segment data.
        bytes = new byte[segmentLength];
        inputStream.read(bytes);

        // Read the number of decomposition levels (SPcod byte 0).
        numDecompositionLevels = bytes[7] & 0xff;
    }

}
