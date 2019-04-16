package edu.illinois.library.cantaloupe.processor.codec.jpeg;

/**
 * Wraps image data returned from {@link TurboJPEGImageReader#read()}.
 */
public final class TurboJPEGImage {

    private boolean isDecompressed;
    private byte[] data;
    private int dataLength = -1, scaledWidth, scaledHeight;

    /**
     * @return {@link #isDecompressed Compressed or decompressed} data. Note
     *         that this array may be longer than the {@link #getDataLength()
     *         actual amount of JPEG data} it contains.
     */
    byte[] getData() {
        return data;
    }

    /**
     * @return Actual length of data contained in {@link #getData()}.
     */
    int getDataLength() {
        if (dataLength >= 0) {
            return dataLength;
        } else if (data != null) {
            return data.length;
        }
        return dataLength;
    }

    int getScaledWidth() {
        return scaledWidth;
    }

    int getScaledHeight() {
        return scaledHeight;
    }

    /**
     * @return Whether the {@link #getData() data} has been decompressed. If
     *         {@literal false}, it can be written into a new JPEG without
     *         re-compression.
     */
    boolean isDecompressed() {
        return isDecompressed;
    }

    void setDecompressed(boolean isDecompressed) {
        this.isDecompressed = isDecompressed;
    }

    void setData(byte[] data) {
        this.data = data;
    }

    void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    void setScaledWidth(int scaledWidth) {
        this.scaledWidth = scaledWidth;
    }

    void setScaledHeight(int scaledHeight) {
        this.scaledHeight = scaledHeight;
    }

}
