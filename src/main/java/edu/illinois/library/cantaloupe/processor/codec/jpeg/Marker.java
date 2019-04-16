package edu.illinois.library.cantaloupe.processor.codec.jpeg;

enum Marker {

    /**
     * Start Of Image, expected to be the very first marker in the stream.
     */
    SOI(0xff, 0xd8),

    /**
     * Start Of Frame for baseline DCT images.
     */
    SOF0(0xff, 0xc0),

    /**
     * Start Of Frame for extended sequential DCT images.
     */
    SOF1(0xff, 0xc1),

    /**
     * Start Of Frame for progressive DCT images.
     */
    SOF2(0xff, 0xc2),

    /**
     * Start Of Frame for lossless (sequential) images.
     */
    SOF3(0xff, 0xc3),

    /**
     * Start Of Frame for differential sequential DCT images.
     */
    SOF5(0xff, 0xc5),

    /**
     * Start Of Frame for differential progressive DCT images.
     */
    SOF6(0xff, 0xc6),

    /**
     * Start Of Frame for differential lossless (sequential) images.
     */
    SOF7(0xff, 0xc7),

    /**
     * Start Of Frame for extended sequential DCT images.
     */
    SOF9(0xff, 0xc9),

    /**
     * Start Of Frame for progressive DCT images.
     */
    SOF10(0xff, 0xca),

    /**
     * Start Of Frame for lossless (sequential) images.
     */
    SOF11(0xff, 0xcb),

    /**
     * Start Of Frame for differential sequential DCT images.
     */
    SOF13(0xff, 0xcd),

    /**
     * Start Of Frame for differential progressive DCT images.
     */
    SOF14(0xff, 0xce),

    /**
     * Start Of Frame for differential lossless (sequential) images.
     */
    SOF15(0xff, 0xcf),

    /**
     * EXIF data.
     */
    APP1(0xff, 0xe1),

    /**
     * ICC profile.
     */
    APP2(0xff, 0xe2),

    /**
     * Photoshop.
     */
    APP13(0xff, 0xed),

    /**
     * Adobe.
     */
    APP14(0xff, 0xee),

    /**
     * Define Huffman Table marker; our effective "stop reading" marker.
     */
    DHT(0xff, 0xc4),

    /**
     * Marker not recognized by this reader, which may still be perfectly
     * valid.
     */
    UNKNOWN(0x00, 0x00);

    static Marker forBytes(int byte1, int byte2) {
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

    byte[] marker() {
        return new byte[] { (byte) byte1, (byte) byte2 };
    }

}
