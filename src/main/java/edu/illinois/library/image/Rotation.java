package edu.illinois.library.image;

/**
 * Created by alexd on 9/2/15.
 */
public class Rotation {

    private Float degrees;
    private boolean mirror = false;

    public static Rotation fromUri(String rotationUri) {
        Rotation rotation = new Rotation();
        if (rotationUri.startsWith("!")) {
            rotation.setMirror(true);
            rotation.setDegrees(Float.parseFloat(rotationUri.substring(1)));
        } else {
            rotation.setMirror(false);
            rotation.setDegrees(Float.parseFloat(rotationUri));
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
     * @param mirror Whether the image should be mirrored before being
     *               rotated.
     */
    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    /**
     * @return Whether the image should be mirrored before being rotated.
     */
    public boolean shouldMirror() {
        return mirror;
    }

}
