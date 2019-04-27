package edu.illinois.library.cantaloupe.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ArrayUtils {

    /**
     * Splits the given data into chunks no larger than the given size. The
     * last chunk may be smaller than the others.
     *
     * @param bytes        Data to chunkify.
     * @param maxChunkSize Maximum chunk size.
     * @return             Chunked data.
     */
    public static List<byte[]> chunkify(byte[] bytes, int maxChunkSize) {
        final List<byte[]> chunks = new ArrayList<>(10);
        if (bytes.length <= maxChunkSize) {
            chunks.add(bytes);
        } else {
            for (int startOffset = 0;
                 startOffset < bytes.length;
                 startOffset += maxChunkSize) {
                int endOffset = startOffset + maxChunkSize;
                if (endOffset > bytes.length) {
                    endOffset = bytes.length;
                }
                byte[] chunk = Arrays.copyOfRange(bytes, startOffset, endOffset);
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    /**
     * @param chunks Ordered list of chunks to merge.
     * @return       Merged chunks.
     */
    public static byte[] merge(List<byte[]> chunks) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            for (byte[] chunk : chunks) {
                os.write(chunk);
            }
            os.flush();
            return os.toByteArray();
        } catch (IOException ignore) {
            // ByteArrayOutputStream is not really going to throw this.
            return new byte[0];
        }
    }

    private ArrayUtils() {}

}
