package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * <p>Image orientation.</p>
 *
 * <p>This is used to support images whose orientation may be different from
 * that of their pixel data. It aligns with the EXIF {@literal Orientation}
 * tag, but it currently supports only rotation and not flipping.
 */
@JsonSerialize(using = OrientationSerializer.class)
@JsonDeserialize(using = OrientationDeserializer.class)
public enum Orientation {

    /**
     * No rotation. (EXIF Orientation value 1)
     */
    ROTATE_0(1, 0),

    /**
     * The image is rotated counter-clockwise (or, the view is rotated
     * clockwise) 90 degrees. (EXIF Orientation value 6; "right top")
     */
    ROTATE_90(6, 90),

    /**
     * The image is rotated 180 degrees. (EXIF Orientation value 3; "bottom
     * right")
     */
    ROTATE_180(3, 180),

    /**
     * The image is rotated counter-clockwise (or, the view is rotated
     * clockwise) 270 degrees. (EXIF Orientation value 8; "left bottom")
     */
    ROTATE_270(8, 270);

    private int degrees, exifValue;

    /**
     * @param value EXIF Orientation tag value.
     * @return Orientation corresponding to the given tag value.
     * @throws IllegalArgumentException if the value is not supported.
     */
    public static Orientation forEXIFOrientation(int value) {
        for (Orientation orientation : values()) {
            if (orientation.getEXIFValue() == value) {
                return orientation;
            }
        }
        throw new IllegalArgumentException("EXIF value " + value +
                " is not supported.");
    }

    Orientation(int exifValue, int degrees) {
        this.exifValue = exifValue;
        this.degrees = degrees;
    }

    /**
     * @return Orientation-adjusted size.
     */
    public Dimension adjustedSize(Dimension size) {
        if (ROTATE_90.equals(this) || ROTATE_270.equals(this)) {
            return new Dimension(size.height(), size.width());
        }
        return size;
    }

    public int getDegrees() {
        return degrees;
    }

    public int getEXIFValue() {
        return exifValue;
    }

}
