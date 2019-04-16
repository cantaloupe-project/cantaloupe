package edu.illinois.library.cantaloupe.image.exif;

import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * <p>TIFF Image File Directory (IFD) reader for EXIF data.</p>
 *
 * <ul>
 *     <li>In a TIFF file, EXIF data is stored in a native TIFF IFD structure,
 *     which includes both the {@link TagSet#BASELINE_TIFF baseline TIFF tags}
 *     in IFD0, as well as one or more sub-IFDs containing other, more
 *     specialized tags. This reader is aware of the EXIF-relevant ones.</li>
 *     <li>In other formats, EXIF data is stored as an embedded TIFF file in
 *     the structure described above. Only IFD0 (and sub-IFDs) is
 *     included.</li>
 * </ul>
 *
 * @author Alex Dolski UIUC
 */
public final class Reader implements AutoCloseable {

    private enum ByteAlignment {

        INTEL(new byte[] { 0x49, 0x49 }, ByteOrder.LITTLE_ENDIAN),
        MOTOROLA(new byte[] { 0x4d, 0x4d }, ByteOrder.BIG_ENDIAN);

        private ByteOrder byteOrder;
        private byte[] signature;

        static ByteAlignment forSignature(byte[] signature) {
            return Arrays.stream(values())
                    .filter(a -> Arrays.equals(a.signature, signature))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unrecognized alignment signature."));
        }

        ByteAlignment(byte[] signature, ByteOrder byteOrder) {
            this.signature = signature;
            this.byteOrder = byteOrder;
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Reader.class);

    private static final char[] JFIF_APP1_EXIF_HEADER =
            "Exif\u0000\u0000".toCharArray();

    private ImageInputStream inputStream;

    /**
     * Position on which file/stream offsets are based. This will be the length
     * of {@link #JFIF_APP1_EXIF_HEADER} if it is present in the stream;
     * otherwise {@literal 0}.
     */
    private int startPos = JFIF_APP1_EXIF_HEADER.length;

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
    }

    public void setSource(byte[] exifBytes) {
        setSource(new MemoryCacheImageInputStream(
                new ByteArrayInputStream(exifBytes)));
    }

    public void setSource(ImageInputStream inputStream) {
        if (this.inputStream != null) {
            throw new IllegalStateException("Source has already been set. " +
                    "close() must be called before setting it again.");
        }
        this.inputStream = inputStream;
    }

    /**
     * Reads an IFD, including any {@link TagSet recognized sub-IFDs}.
     */
    public Directory read() throws IOException {
        final Stopwatch watch = new Stopwatch();

        // If we are reading the IFD from e.g. a JPEG APP1 segment, it may be
        // preceded by an EXIF marker, which is not part of the IFD. Here we'll
        // check for that and skip past it if it exists.
        inputStream.mark();
        final char[] chars = readChars(JFIF_APP1_EXIF_HEADER.length);
        if (!Arrays.equals(chars, JFIF_APP1_EXIF_HEADER)) {
            startPos = 0;
            inputStream.reset();
        }

        // Read the TIFF header, which contains the byte alignment.
        byte[] bytes = readBytes(2);
        inputStream.setByteOrder(ByteAlignment.forSignature(bytes).byteOrder);
        inputStream.skipBytes(2);

        // Find the location of IFD0 and seek to it.
        int ifdOffset = inputStream.readInt();
        seek(ifdOffset);

        Directory dir = read(TagSet.BASELINE_TIFF);

        LOGGER.trace("read(): read {}-field IFD from offset {} in {}",
                dir.size(), ifdOffset, watch);

        return dir;
    }

    /**
     * @param tagSet Limits the reading to the tags in this set. (It's not
     *               really feasible to read unknown tags, because the values
     *               can't be interpreted correctly without foreknowledge of
     *               the tag semantics.)
     */
    private Directory read(TagSet tagSet) throws IOException {
        final Directory dir  = new Directory(tagSet);
        final int numEntries = inputStream.readUnsignedShort();

        for (int i = 0; i < numEntries; i++) {
            final int tagNum        = inputStream.readUnsignedShort();
            final int dataFormat    = inputStream.readUnsignedShort();
            final int numComponents = inputStream.readInt();
            // Will contain a value if <= 4 bytes, otherwise an offset.
            final int valueOrOffset = inputStream.readInt();

            if (tagSet.containsTag(tagNum)) {
                final Tag tag = tagSet.getTag(tagNum);
                // Is the tag value a pointer to a sub-IFD?
                final TagSet subIFDTagSet = TagSet.forIFDPointerTag(tagNum);
                if (subIFDTagSet != null) {
                    final long pos = inputStream.getStreamPosition();
                    seek(valueOrOffset);
                    Directory subDir = read(subIFDTagSet);
                    dir.put(tag, DataType.SLONG, subDir);
                    seek(pos);
                } else {
                    final DataType format = DataType.forValue(dataFormat);
                    final int valueLength = format.getNumBytesPerComponent() *
                            numComponents;
                    final Field field = new Field(tag, format);

                    if (valueLength <= 4) {
                        dir.put(field, format.decode(BigInteger.valueOf(valueOrOffset).toByteArray()));
                    } else {
                        inputStream.mark();
                        seek(valueOrOffset);
                        byte[] value = readBytes(valueLength);
                        inputStream.reset();
                        dir.put(field, format.decode(value));
                    }
                }
            }
        }
        return dir;
    }

    private byte[] readBytes(int length) throws IOException {
        byte[] data = new byte[length];
        int n, offset = 0;
        while ((n = inputStream.read(data, offset, data.length - offset)) < offset) {
            offset += n;
        }
        return data;
    }

    private char[] readChars(int length) throws IOException {
        byte[] data = readBytes(length);
        return StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(data)).array();
    }

    private void seek(long offset) throws IOException {
        // Offset is relative to the beginning of the TIFF signature.
        inputStream.seek(startPos + offset);
    }

}
