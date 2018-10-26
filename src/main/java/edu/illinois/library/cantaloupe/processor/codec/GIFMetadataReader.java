package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * <p>Reads various metadata from a GIF image.</p>
 *
 * <p>The main use case for this class is as a workaround for the Image I/O
 * GIF reader's inability to read XMP data.</p>
 *
 * @see <a href="https://www.w3.org/Graphics/GIF/spec-gif89a.txt">Graphics
 *      Interchange Format Version 89a</a>
 * @see <a href="https://www.fileformat.info/format/gif/egff.htm">GIF File
 *      Format Summary</a>
 * @author Alex Dolski UIUC
 */
final class GIFMetadataReader implements AutoCloseable {

    private enum Version {
        GIF87A(new byte[] { 0x38, 0x37, 0x61 }),
        GIF89A(new byte[] { 0x38, 0x39, 0x61 }),
        UNKNOWN(new byte[] {});

        private byte[] signature;

        static Version forBytes(byte[] bytes) {
            return Arrays.stream(values())
                    .filter(v -> Arrays.equals(v.signature, bytes))
                    .findFirst()
                    .orElse(UNKNOWN);
        }

        Version(byte[] signature) {
            this.signature = signature;
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFMetadataReader.class);

    private static final byte[] GIF_SIGNATURE = new byte[] { 0x47, 0x49, 0x46 };

    private static final byte[] NETSCAPE_APPLICATION_IDENTIFIER = new byte[] {
            (byte) 'N', (byte) 'E', (byte) 'T', (byte) 'S',
            (byte) 'C', (byte) 'A', (byte) 'P', (byte) 'E' };

    private static final byte[] NETSCAPE_APPLICATION_AUTH_CODE = new byte[] {
            (byte) '2', (byte) '.', (byte) '0' };

    private static final byte[] XMP_APPLICATION_IDENTIFIER = new byte[] {
            (byte) 'X', (byte) 'M', (byte) 'P', (byte) ' ',
            (byte) 'D', (byte) 'a', (byte) 't', (byte) 'a' };

    private static final byte[] XMP_APPLICATION_AUTH_CODE = new byte[] {
            (byte) 'X', (byte) 'M', (byte) 'P' };

    /**
     * Set to {@literal true} once reading begins.
     */
    private boolean isReadAttempted;

    /**
     * Stream from which to read the image data.
     */
    private ImageInputStream inputStream;

    private int width, height, loopCount, delayTime;

    private String xmp;

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }

    /**
     * @return Delay time in hundredths of a second.
     */
    int getDelayTime() throws IOException {
        readData();
        return delayTime;
    }

    /**
     * @return Height of the image grid.
     */
    int getHeight() throws IOException {
        readData();
        return height;
    }

    int getLoopCount() throws IOException {
        readData();
        return loopCount;
    }

    /**
     * @return Width of the image grid.
     */
    int getWidth() throws IOException {
        readData();
        return width;
    }

    /**
     * @return RDF/XML string.
     */
    String getXMP() throws IOException {
        readData();
        return xmp;
    }

    /**
     * @param inputStream Fresh stream from which to read the image.
     */
    void setSource(ImageInputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public String toString() {
        return String.format("[size: %dx%d] [XMP? %b] ",
                width, height, (xmp != null));
    }

    /**
     * <p>Main reading method. Reads image info into instance variables. May
     * call other private reading methods that will all expect {@link
     * #inputStream} to be pre-positioned for reading.</p>
     *
     * <p>It's safe to call this method multiple times.</p>
     */
    private void readData() throws IOException {
        if (isReadAttempted) {
            return;
        } else if (inputStream == null) {
            throw new IllegalStateException("Source not set");
        }

        final Stopwatch watch = new Stopwatch();

        checkSignature();
        readVersion();

        isReadAttempted = true;

        // Read the Logical Screen Descriptor block.
        byte[] bytes = read(7);
        width = ((bytes[1] & 0xff) << 8) | (bytes[0] & 0xff);
        height = ((bytes[3] & 0xff) << 8) | (bytes[2] & 0xff);

        // If the Global Color Table Flag bit == 1...
        if ((bytes[4] & 0x01) == 1) {
            // Read the GCT length from bits 0-3 in order to skip the GCT.
            int numEntries = bytes[4] & 0b111;
            int gctLength = 3 * (1 << (numEntries + 1));
            inputStream.skipBytes(gctLength);
        }

        while (readBlock() != -1) {
            // Read the stream block-by-block.
        }

        LOGGER.debug("Read in {}: {}", watch, this);
    }

    private void checkSignature() throws IOException {
        byte[] bytes = read(3);
        if (!Arrays.equals(bytes, GIF_SIGNATURE)) {
            String hexStr = DatatypeConverter.printHexBinary(bytes);
            throw new IOException("Invalid signature: " + hexStr +
                    " (is this a GIF?)");
        }
    }

    private void readVersion() throws IOException {
        final byte[] bytes = read(3);
        if (Version.UNKNOWN.equals(Version.forBytes(bytes))) {
            String hexStr = DatatypeConverter.printHexBinary(bytes);
            throw new IOException("Invalid GIF version: " + hexStr);
        }
    }

    private int readBlock() throws IOException {
        // Blocks may occur in any order.
        switch (inputStream.readByte()) {
            case 0x2c: // Image Descriptor
                //readImage();
                break;
            case 0x21: // Extension Introducer
                readExtension();
                break;
            case 0x3b: // Trailer
                return -1;
        }
        return 0;
    }

    private void readImage() throws IOException {
        byte[] bytes = read(9);
        // If the Local Color Table Flag bit == 1...
        if ((bytes[8] & 0x01) == 1) {
            // Read the LCT size from bits 0-3 in order to skip the LCT.
            int size = bytes[8] & 0b111;
            int lctLength = 3 * (1 << (size + 1));
            inputStream.skipBytes(lctLength);
        }
        skipSubBlocks();
    }

    private void skipSubBlocks() throws IOException {
        while (skipSubBlock() != -1) {
        }
    }

    private short skipSubBlock() throws IOException {
        int length = inputStream.readByte() & 0xff;
        if (length > 0) {
            inputStream.skipBytes(length);
            return 0;
        }
        return -1;
    }

    private void readExtension() throws IOException {
        byte[] bytes = read(1); // read the label
        switch (bytes[0]) {
            case 0x01:
                readPlainTextExtension();
                break;
            case (byte) 0xff:
                readApplicationExtension();
                break;
            case (byte) 0xf9:
                readGraphicControlExtension();
                break;
            case (byte) 0xfe:
                readCommentExtension();
                break;
        }
    }

    private void readApplicationExtension() throws IOException {
        inputStream.skipBytes(1); // skip block size (always 0x0b)

        final byte[] identifier = read(8);
        final byte[] authCode = read(3);
        if (Arrays.equals(NETSCAPE_APPLICATION_IDENTIFIER, identifier) &&
                Arrays.equals(NETSCAPE_APPLICATION_AUTH_CODE, authCode)) {
            readNetscapeApplicationExtension();
        } else if (Arrays.equals(XMP_APPLICATION_IDENTIFIER, identifier) &&
                Arrays.equals(XMP_APPLICATION_AUTH_CODE, authCode)) {
            readXMPApplicationExtension();
        } else {
            skipSubBlocks();
        }
    }

    private void readNetscapeApplicationExtension() throws IOException {
        inputStream.skipBytes(2);
        byte[] bytes = read(2);
        loopCount = ((bytes[1] & 0xff) << 8) | (bytes[0] & 0xff);
    }

    private void readXMPApplicationExtension() throws IOException {
        // Data in this block is not arranged in sub-blocks like other GIF
        // data, and there is no preceding length indicator, so it must be read
        // as a stream of unknown length. We read byte-by-byte into a buffer
        // and stop when we encounter zero (which the XMP data is guaranteed
        // not to contain) at the end of the magic trailer.
        // See: http://wwwimages.adobe.com/www.adobe.com/content/dam/acom/en/devnet/xmp/pdfs/XMP%20SDK%20Release%20cc-2016-08/XMPSpecificationPart3.pdf
        // sec. 1.1.2 (p. 11)
        final ByteBuffer buffer = ByteBuffer.allocate(65536);
        byte b;
        while ((b = inputStream.readByte()) != 0) {
            buffer.put(b);
        }
        // Discard the "magic trailer" and block terminator.
        byte[] data = Arrays.copyOfRange(buffer.array(), 0, buffer.position() - 256);
        xmp = new String(data, StandardCharsets.UTF_8);
        xmp = xmp.substring(xmp.indexOf("<rdf:RDF "), xmp.indexOf("</rdf:RDF>") + 10);
    }

    private void readCommentExtension() throws IOException {
        skipSubBlocks();
    }

    private void readGraphicControlExtension() throws IOException {
        inputStream.skipBytes(2);
        byte[] data = read(2);
        delayTime = ((data[1] & 0xff) << 8) | (data[0] & 0xff);
        inputStream.skipBytes(1);
    }

    private void readPlainTextExtension() throws IOException {
        inputStream.skipBytes(13); // read remaining fields
        skipSubBlocks();
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
