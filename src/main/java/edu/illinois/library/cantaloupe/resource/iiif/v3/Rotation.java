package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.util.StringUtils;

/**
 * Encapsulates the rotation component of a URI.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#43-rotation">IIIF Image API
 * 3.0: Rotation</a>
 */
class Rotation {

    private static final double DELTA = 0.00000001;

    private double degrees = 0.0;
    private boolean mirror = false;

    /**
     * @param rotationUri Rotation URI component.
     * @return Instance corresponding to the given argument.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    static Rotation fromURI(String rotationUri) {
        Rotation rotation = new Rotation();
        try {
            if (rotationUri.startsWith("!")) {
                rotation.setMirror(true);
                rotation.setDegrees(Double.parseDouble(rotationUri.substring(1)));
            } else {
                rotation.setMirror(false);
                rotation.setDegrees(Double.parseDouble(rotationUri));
            }
        } catch (NumberFormatException e) {
            throw new IllegalClientArgumentException("Invalid rotation");
        } catch (IllegalArgumentException e) {
            throw new IllegalClientArgumentException(e.getMessage(), e);
        }
        return rotation;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Rotation) {
            Rotation other = (Rotation) obj;
            return Math.abs(other.getDegrees() - getDegrees()) < DELTA &&
                    other.shouldMirror() == shouldMirror();
        }
        return super.equals(obj);
    }

    double getDegrees() {
        return degrees;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @return Whether the instance describes a zero rotation. (This would be
     *         a no-op if {@link #shouldMirror()} returns {@code false}.
     */
    boolean isZero() {
        return Math.abs(degrees) < DELTA || Math.abs(degrees - 360) < DELTA;
    }

    /**
     * @param degrees Degrees of rotation between 0 and 360.
     * @throws IllegalClientArgumentException if the argument is less than zero
     *         or greater than or equal to 360.
     */
    void setDegrees(double degrees) {
        if (degrees < 0 || degrees > 360) {
            throw new IllegalClientArgumentException(
                    "Degrees must be greater than or equal to 0 and less than 360");
        }
        this.degrees = degrees;
    }

    /**
     * @param mirror Whether the image should be mirrored before being rotated.
     */
    void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    /**
     * @return Whether the image should be mirrored before being rotated.
     */
    boolean shouldMirror() {
        return mirror;
    }

    Rotate toRotate() {
        return new Rotate(getDegrees());
    }

    /**
     * @return Transpose, or null if there is no transposition.
     */
    Transpose toTranspose() {
        if (shouldMirror()) {
            return Transpose.HORIZONTAL;
        }
        return null;
    }

    /**
     * @return Value compatible with the rotation component of an IIIF URI.
     */
    @Override
    public String toString() {
        String str = "";
        if (shouldMirror()) {
            str += "!";
        }
        str += StringUtils.removeTrailingZeroes(getDegrees());
        return str;
    }

    /**
     * @return Canonical value compatible with the rotation component of an
     *         IIIF URI.
     * @see    #toString()
     */
    String toCanonicalString() {
        return toString();
    }

}
