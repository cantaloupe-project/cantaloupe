package edu.illinois.library.cantaloupe.request;

import edu.illinois.library.cantaloupe.util.NumberUtil;

public class Rotation {

    private Float degrees;
    private boolean mirror = false;

    public static Rotation fromUri(String rotationUri)
            throws IllegalArgumentException {
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
            throw new IllegalArgumentException("Invalid rotation");
        }
        return rotation;
    }

    public Float getDegrees() {
        return degrees;
    }

    /**
     * @param degrees Degrees of rotation between 0 and 360
     * @throws IllegalArgumentException
     */
    public void setDegrees(Float degrees) throws IllegalArgumentException {
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
     * @return Value compatible with the rotation component of an IIIF URI.
     */
    public String toString() {
        String str = "";
        if (this.shouldMirror()) {
            str += "!";
        }
        str += NumberUtil.removeTrailingZeroes(this.getDegrees());
        return str;
    }

}
