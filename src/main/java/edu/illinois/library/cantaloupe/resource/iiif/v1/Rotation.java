package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.util.StringUtil;

/**
 * Encapsulates the "rotation" component of an IIIF request URI.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#parameters-rotation">IIIF Image
 * API 1.1</a>
 */
class Rotation implements Comparable<Object> {

    private float degrees = 0f;

    public static Rotation fromUri(String rotationUri)
            throws IllegalArgumentException {
        Rotation rotation = new Rotation();
        try {
            rotation.setDegrees(Float.parseFloat(rotationUri));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid rotation");
        }
        return rotation;
    }

    @Override
    public int compareTo(Object object) {
        if (object instanceof Rotate) {
            Rotate rotate =
                    (Rotate) object;
            return new Float(this.getDegrees()).
                    compareTo(rotate.getDegrees());
        } else if (object instanceof Rotation) {
            Rotation rotation = (Rotation) object;
            return new Float(this.getDegrees()).
                    compareTo(rotation.getDegrees());
        }
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        return this.compareTo(obj) == 0;
    }

    public float getDegrees() {
        return degrees;
    }

    @Override
    public int hashCode(){
        return new Float(this.getDegrees()).hashCode();
    }

    /**
     * @param degrees Degrees of rotation between 0 and 360
     * @throws IllegalArgumentException
     */
    public void setDegrees(float degrees) throws IllegalArgumentException {
        if (degrees < 0 || degrees > 360) {
            throw new IllegalArgumentException("Degrees must be between 0 and 360");
        }
        this.degrees = degrees;
    }

    public Rotate toRotate() {
        return new Rotate(this.getDegrees());
    }

    /**
     * @return Value compatible with the rotation component of an IIIF URI.
     */
    public String toString() {
        return StringUtil.removeTrailingZeroes(this.getDegrees());
    }

}
