package edu.illinois.library.cantaloupe.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class ArrayUtils {

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
