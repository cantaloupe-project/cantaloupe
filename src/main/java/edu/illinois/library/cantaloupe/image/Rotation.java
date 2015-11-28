package edu.illinois.library.cantaloupe.image;

/**
 * Encapsulates a rotation with optional mirroring.
 */
public class Rotation {

    private float degrees = 0f;
    private boolean mirror = false; // TODO: split out into a Flip class

    /**
     * No-op constructor.
     */
    public Rotation() {}

    /**
     * @param degrees Degrees of rotation between 0 and 360
     */
    public Rotation(float degrees) {
        this.setDegrees(degrees);
    }

    public float getDegrees() {
        return degrees;
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
     * @param mirror Whether the request should be mirrored before being
     *               rotated.
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
     * @return String representation of the instance, guaranteed to represent
     * the instance, but not guaranteed to be meaningful.
     */
    @Override
    public String toString() {
        String str = "";
        if (this.shouldMirror()) {
            str += "!";
        }
        str += NumberUtil.removeTrailingZeroes(this.getDegrees());
        return str;
    }

}
