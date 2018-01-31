package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.util.StringUtil;

/**
 * Encapsulates the "rotation" component of a URI.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#parameters-rotation">IIIF Image
 * API 1.1</a>
 */
class Rotation implements Comparable<Object> {

    private float degrees = 0f;

    /**
     * @param rotationUri Rotation component of a URI.
     * @return            Instance corresponding to the given argument.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    public static Rotation fromUri(String rotationUri) {
        Rotation rotation = new Rotation();
        try {
            rotation.setDegrees(Float.parseFloat(rotationUri));
        } catch (NumberFormatException e) {
            throw new IllegalClientArgumentException("Invalid rotation");
        } catch (IllegalArgumentException e) {
            throw new IllegalClientArgumentException(e.getMessage(), e);
        }
        return rotation;
    }

    @Override
    public int compareTo(Object object) {
        if (object instanceof Rotate) {
            Rotate rotate = (Rotate) object;
            return Float.valueOf(this.getDegrees()).
                    compareTo(rotate.getDegrees());
        } else if (object instanceof Rotation) {
            Rotation rotation = (Rotation) object;
            return Float.valueOf(this.getDegrees()).
                    compareTo(rotation.getDegrees());
        }
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return this.compareTo(obj) == 0;
    }

    public float getDegrees() {
        return degrees;
    }

    @Override
    public int hashCode(){
        return Float.valueOf(this.getDegrees()).hashCode();
    }

    /**
     * @param degrees Degrees of rotation between 0 and 360
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    public void setDegrees(float degrees) {
        if (degrees < 0 || degrees > 360) {
            throw new IllegalClientArgumentException(
                    "Degrees must be between 0 and 360");
        }
        this.degrees = degrees;
    }

    public Rotate toRotate() {
        return new Rotate(getDegrees());
    }

    /**
     * @return Value compatible with the rotation component of a URI.
     */
    public String toString() {
        return StringUtil.removeTrailingZeroes(getDegrees());
    }

}
