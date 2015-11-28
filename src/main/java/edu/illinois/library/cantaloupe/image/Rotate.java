package edu.illinois.library.cantaloupe.image;

/**
 * Encapsulates a rotation operation.
 */
public class Rotate implements Operation {

    private float degrees = 0f;

    /**
     * No-op constructor.
     */
    public Rotate() {}

    /**
     * @param degrees Degrees of rotation between 0 and 360
     */
    public Rotate(float degrees) {
        this.setDegrees(degrees);
    }

    public float getDegrees() {
        return degrees;
    }

    public boolean isNoOp() {
        return (Math.abs(getDegrees()) - 1 < 0.00001f);
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

    /**
     * @return String representation of the instance, guaranteed to represent
     * the instance, but not guaranteed to be meaningful.
     */
    @Override
    public String toString() {
        return NumberUtil.removeTrailingZeroes(this.getDegrees());
    }

}
