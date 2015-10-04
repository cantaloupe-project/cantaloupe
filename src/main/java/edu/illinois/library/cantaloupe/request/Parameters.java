package edu.illinois.library.cantaloupe.request;

import edu.illinois.library.cantaloupe.image.SourceFormat;

/**
 * Encapsulates the parameters of an IIIF request.
 *
 * @see <a href="http://iiif.io/api/request/2.0/#request-request-parameters">IIIF
 *      Image API 2.0</a>
 */
public class Parameters implements Comparable<Parameters> {

    private OutputFormat outputFormat;
    private Identifier identifier;
    private Quality quality;
    private Region region;
    private Rotation rotation;
    private Size size;

    /**
     * @param identifier From URI
     * @param region From URI
     * @param size From URI
     * @param rotation From URI
     * @param quality From URI
     * @param format From URI
     */
    public Parameters(String identifier, String region, String size,
                      String rotation, String quality, String format) {
        this.identifier = Identifier.fromUri(identifier);
        this.outputFormat = OutputFormat.valueOf(format.toUpperCase());
        this.quality = Quality.valueOf(quality.toUpperCase());
        this.region = Region.fromUri(region);
        this.rotation = Rotation.fromUri(rotation);
        this.size = Size.fromUri(size);
    }

    @Override
    public int compareTo(Parameters params) {
        int last = this.toString().compareTo(params.toString());
        return (last == 0) ? this.toString().compareTo(params.toString()) : last;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public Identifier getIdentifier() {
        return identifier;
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

    public Size getSize() {
        return size;
    }

    /**
     * @return Whether the parameters are effectively requesting the unmodified
     * source image, i.e. whether they specify full region, full scale, 0
     * rotation, no mirroring, default or color quality, and the same output
     * format as the source format.
     */
    public boolean isUnmodified() {
        return this.getRegion().isFull() &&
                this.getSize().getScaleMode() == Size.ScaleMode.FULL &&
                this.getRotation().getDegrees() == 0 &&
                !(this.getRotation().shouldMirror() &&
                        this.getRotation().getDegrees() != 0) &&
                (this.getQuality().equals(Quality.DEFAULT) ||
                        this.getQuality().equals(Quality.COLOR)) &&
                this.getOutputFormat().isEqual(
                        SourceFormat.getSourceFormat(this.getIdentifier()));
    }

    /**
     * @return IIIF URI parameters with no leading slash.
     */
    public String toString() {
        return String.format("%s/%s/%s/%s/%s.%s", getIdentifier(), getRegion(),
                getSize(), getRotation(), getQuality().toString().toLowerCase(),
                getOutputFormat());
    }

}
