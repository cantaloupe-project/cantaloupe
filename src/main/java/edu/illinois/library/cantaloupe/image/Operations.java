package edu.illinois.library.cantaloupe.image;

/**
 * <p>Encapsulates a set of image transform operations. Operations can be
 * expected to be applied in the following order:</p>
 *
 * <ol>
 *     <li>Cropping</li>
 *     <li>Scaling</li>
 *     <li>Rotation</li>
 *     <li>Filtering</li>
 *     <li>Format conversion</li>
 * </ol>
 */
public class Operations implements Comparable<Operations> {

    private Identifier identifier;
    private OutputFormat outputFormat;
    private Quality quality;
    private Crop region;
    private Rotation rotation;
    private Scale scale;

    /**
     * No-op constructor.
     */
    public Operations() {}

    /**
     * @param identifier
     * @param region
     * @param scale
     * @param rotation
     * @param quality
     * @param format
     */
    public Operations(Identifier identifier, Crop region, Scale scale,
                      Rotation rotation, Quality quality, OutputFormat format) {
        setIdentifier(identifier);
        setRegion(region);
        setScale(scale);
        setRotation(rotation);
        setQuality(quality);
        setOutputFormat(format);
    }

    @Override
    public int compareTo(Operations ops) {
        int last = this.toString().compareTo(ops.toString());
        return (last == 0) ? this.toString().compareTo(ops.toString()) : last;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public Quality getQuality() {
        return quality;
    }

    public Crop getRegion() {
        return region;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public Scale getScale() {
        return scale;
    }

    /**
     * @return Whether the operations are effectively requesting the unmodified
     * source image, i.e. whether they specify full region, full scale, 0
     * rotation, no mirroring, default or color quality, and the same output
     * format as the source format.
     */
    public boolean isRequestingUnmodifiedSource() {
        final Scale scale = this.getScale();
        final boolean isFullSize = (scale.getScaleMode() == Scale.Mode.FULL) ||
                (scale.getPercent() != null && Math.abs(scale.getPercent() - 100f) < 0.000001f ||
                        (scale.getPercent() == null && scale.getWidth() == null && scale.getHeight() == null));
        return this.getRegion().isFull() && isFullSize &&
                this.getRotation().getDegrees() == 0 &&
                !(this.getRotation().shouldMirror() &&
                        this.getRotation().getDegrees() != 0) &&
                (this.getQuality().equals(Quality.DEFAULT) ||
                        this.getQuality().equals(Quality.COLOR)) &&
                this.getOutputFormat().isEqual(
                        SourceFormat.getSourceFormat(this.getIdentifier()));
    }

    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public void setQuality(Quality quality) {
        this.quality = quality;
    }

    public void setRegion(Crop region) {
        this.region = region;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public void setScale(Scale scale) {
        this.scale = scale;
    }

    /**
     * @return String representation of the instance, guaranteed to represent
     * the instance, but not guaranteed to be meaningful.
     */
    public String toString() {
        return String.format("%s_%s_%s_%s_%s_%s", getIdentifier(), getRegion(),
                getScale(), getRotation(),
                getQuality().toString().toLowerCase(), getOutputFormat());
    }

}
