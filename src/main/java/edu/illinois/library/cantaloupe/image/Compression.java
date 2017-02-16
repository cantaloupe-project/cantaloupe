package edu.illinois.library.cantaloupe.image;

public enum Compression {

    /**
     * Undefined compression type (possibly including no compression).
     */
    UNDEFINED,

    /**
     * DEFLATE compression, a.k.a. "Zip" or "ZLib."
     *
     * @see <a href="https://tools.ietf.org/html/rfc1951">DEFLATE Compressed
     * Data Format Specification version 1.3</a>
     */
    DEFLATE,

    /**
     * JPEG JFIF compression.
     */
    JPEG,

    JPEG2000,

    LZW,

    /**
     * No compression.
     */
    UNCOMPRESSED,

    /**
     * Unspecified run-length encoding.
     */
    RLE

}
