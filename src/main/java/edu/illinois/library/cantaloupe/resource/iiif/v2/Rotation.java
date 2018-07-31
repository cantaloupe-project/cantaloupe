package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.util.StringUtils;

/**
 * Encapsulates the "rotation" component of a URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#rotation">IIIF Image API 2.0</a>
 * @see <a href="http://iiif.io/api/image/2.1/#rotation">IIIF Image API 2.1</a>
 */
class Rotation implements Comparable<Object> {

    private float degrees = 0f;
    private boolean mirror = false;

    /**
     * @param rotationUri Rotation URI component.
     * @return Instance corresponding to the given argument.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    public static Rotation fromUri(String rotationUri) {
        Rotation rotation = new Rotation();
        try {
            if (rotationUri.startsWith("!")) {
                rotation.setMirror(true);
                rotation.setDegrees(Float.parseFloat(rotationUri.substring(1)));
            } else {
                rotation.setMirror(false);
                rotation.setDegrees(Float.parseFloat(rotationUri));
            }
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
            if (getDegrees() == rotate.getDegrees()) {
                return 0;
            }
        } else if (object instanceof Transpose) {
            Transpose t = (Transpose) object;
            if (this.shouldMirror() && t == Transpose.HORIZONTAL) {
                return 0;
            }
        } else if (object instanceof Rotation) {
            Rotation rotation = (Rotation) object;
            if (rotation.shouldMirror() == shouldMirror()) {
                return Float.valueOf(getDegrees()).
                        compareTo(rotation.getDegrees());
            }
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
        return toString().hashCode();
    }

    /**
     * @param degrees Degrees of rotation between 0 and 360.
     * @throws IllegalClientArgumentException
     */
    public void setDegrees(float degrees) {
        if (degrees < 0 || degrees > 360) {
            throw new IllegalClientArgumentException(
                    "Degrees must be greater than or equal to 0 and less than 360");
        }
        this.degrees = degrees;
    }

    /**
     * @param mirror Whether the image should be mirrored before being rotated.
     */
    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    /**
     * @return Whether the request should be mirrored before being rotated.
     */
    public boolean shouldMirror() {
        return mirror;
    }

    /**
     * @return Transpose, or null if there is no transposition.
     */
    public Transpose toTranspose() {
        if (shouldMirror()) {
            return Transpose.HORIZONTAL;
        }
        return null;
    }

    public Rotate toRotate() {
        return new Rotate(getDegrees());
    }

    /**
     * @return Value compatible with the rotation component of an IIIF URI.
     */
    public String toString() {
        String str = "";
        if (shouldMirror()) {
            str += "!";
        }
        str += StringUtils.removeTrailingZeroes(getDegrees());
        return str;
    }

}
