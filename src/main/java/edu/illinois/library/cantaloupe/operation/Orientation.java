package edu.illinois.library.cantaloupe.operation;

/**
 * <p>Signifies an image orientation.</p>
 *
 * <p>Mainly this is used in support of images whose orientation may be
 * different from that of their pixel data. It aligns with the EXIF Orientation
 * tag, although it currently supports only rotation and not flipping.
 */
public enum Orientation {

    /**
     * No rotation. (EXIF Orientation value 1)
     */
    ROTATE_0(0),

    /**
     * The image is rotated counter-clockwise (or, the view is rotated
     * clockwise) 90 degrees. (EXIF Orientation value 6; "right top")
     */
    ROTATE_90(90),

    /**
     * The image is rotated 180 degrees. (EXIF Orientation value 3; "bottom
     * right")
     */
    ROTATE_180(180),

    /**
     * The image is rotated counter-clockwise (or, the view is rotated
     * clockwise) 270 degrees. (EXIF Orientation value 8; "left bottom")
     */
    ROTATE_270(270);

    private int degrees;

    /**
     * @param value EXIF Orientation tag value.
     * @return Orientation corresponding to the given tag value.
     * @throws IllegalArgumentException If the value is not supported.
     */
    public static Orientation forEXIFOrientation(int value) {
        switch (value) {
            case 1:
                return ROTATE_0;
            case 3:
                return ROTATE_180;
            case 6:
                return ROTATE_90;
            case 8:
                return ROTATE_270;
            default:
                throw new IllegalArgumentException("EXIF value " + value +
                        " is not supported.");
        }
    }

    Orientation(int degrees) {
        this.degrees = degrees;
    }

    public int getDegrees() {
        return degrees;
    }

}
