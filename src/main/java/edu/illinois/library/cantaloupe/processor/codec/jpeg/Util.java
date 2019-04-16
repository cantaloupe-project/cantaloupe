package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.util.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

final class Util {

    static byte[] assembleAPP1Segment(String xmp) {
        try {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            final byte[] headerBytes = Constants.STANDARD_XMP_SEGMENT_HEADER;
            final byte[] xmpBytes = Metadata.encapsulateXMP(xmp).
                    getBytes(StandardCharsets.UTF_8);
            // write segment marker
            os.write(new byte[]{(byte) 0xff, (byte) 0xe1});
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

    static boolean isAdobeSegment(byte[] segmentData) {
        return (segmentData.length >= 12 &&
                segmentData[0] == 'A' &&
                segmentData[1] == 'd' &&
                segmentData[2] == 'o' &&
                segmentData[3] == 'b' &&
                segmentData[4] == 'e');
    }

    static boolean isEXIFSegment(byte[] segmentData) {
        return Arrays.equals(
                Constants.EXIF_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, Constants.EXIF_SEGMENT_HEADER.length));
    }

    static boolean isExtendedXMPSegment(byte[] segmentData) {
        return Arrays.equals(
                Constants.EXTENDED_XMP_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, Constants.EXTENDED_XMP_SEGMENT_HEADER.length));
    }

    static boolean isPhotoshopSegment(byte[] segmentData) {
        return Arrays.equals(
                Constants.PHOTOSHOP_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, Constants.PHOTOSHOP_SEGMENT_HEADER.length));
    }

    static boolean isStandardXMPSegment(byte[] segmentData) {
        return Arrays.equals(
                Constants.STANDARD_XMP_SEGMENT_HEADER,
                Arrays.copyOfRange(segmentData, 0, Constants.STANDARD_XMP_SEGMENT_HEADER.length));
    }

    /**
     * Merges the given list of chunks, or returns the single chunk if the
     * list has only one element; or returns {@code null} if the list is empty.
     */
    static byte[] mergeChunks(List<byte[]> chunks) {
        final int numChunks = chunks.size();
        if (numChunks > 1) {
            return ArrayUtils.merge(chunks);
        }
        return chunks.get(0);
    }

    private Util() {}

}
