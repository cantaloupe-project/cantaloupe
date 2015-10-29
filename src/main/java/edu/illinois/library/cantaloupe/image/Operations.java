package edu.illinois.library.cantaloupe.image;

/**
 * A list of image transform operations.
 */
public class Operations implements Comparable<Operations> {

    private OutputFormat outputFormat;
    private Identifier identifier;
    private Quality quality;
    private Region region;
    private Rotation rotation;
    private Scale size;

    /**
     * No-op constructor.
     */
    public Operations() {}

     /**
     * @param identifier From URI
     * @param region From URI
     * @param size From URI
     * @param rotation From URI
     * @param quality From URI
     * @param format From URI
     */
    public Operations(Identifier identifier, Region region, Scale size,
                      Rotation rotation, Quality quality, OutputFormat format) {
        setIdentifier(identifier);
        setOutputFormat(format);
        setQuality(quality);
        setRegion(region);
        setRotation(rotation);
        setSize(size);
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

    public Region getRegion() {
        return region;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public Scale getSize() {
        return size;
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

    public void setRegion(Region region) {
        this.region = region;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    public void setSize(Scale size) {
        this.size = size;
    }

    /**
     * @return Whether the operations are effectively requesting the unmodified
     * source image, i.e. whether they specify full region, full scale, 0
     * rotation, no mirroring, default or color quality, and the same output
     * format as the source format.
     */
    public boolean isRequestingUnmodifiedSource() {
        final Scale size = this.getSize();
        final boolean isFullSize = (size.getScaleMode() == Scale.Mode.FULL) ||
                (size.getPercent() != null && Math.abs(size.getPercent() - 100f) < 0.000001f);
        return this.getRegion().isFull() && isFullSize &&
                this.getRotation().getDegrees() == 0 &&
                !(this.getRotation().shouldMirror() &&
                        this.getRotation().getDegrees() != 0) &&
                (this.getQuality().equals(Quality.DEFAULT) ||
                        this.getQuality().equals(Quality.COLOR)) &&
                this.getOutputFormat().isEqual(
                        SourceFormat.getSourceFormat(this.getIdentifier()));
    }

}
