package edu.illinois.library.cantaloupe.resource.iiif.v1_1;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.Reference;

/**
 * Encapsulates the parameters of an IIIF request.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#parameters">IIIF Image API
 * 1.1</a>
 */
class Parameters implements Comparable<Parameters> {

    private OutputFormat outputFormat;
    private Identifier identifier;
    private Quality quality;
    private Region region;
    private Rotation rotation;
    private Size size;

    /**
     * @param paramsStr URI path fragment beginning from the identifier onward
     * @throws IllegalArgumentException if the <code>params</code> is not in
     * the correct format
     */
    public static Parameters fromUri(String paramsStr)
            throws IllegalArgumentException {
        Parameters params = new Parameters();
        String[] parts = StringUtils.split(paramsStr, "/");
        if (parts.length == 5) {
            params.setIdentifier(new Identifier(Reference.decode(parts[0])));
            params.setRegion(Region.fromUri(parts[1]));
            params.setSize(Size.fromUri(parts[2]));
            params.setRotation(Rotation.fromUri(parts[3]));
            String[] subparts = StringUtils.split(parts[4], ".");
            if (subparts.length == 2) {
                params.setQuality(Quality.valueOf(subparts[0].toUpperCase()));
                params.setOutputFormat(OutputFormat.valueOf(subparts[1].toUpperCase()));
            } else {
                throw new IllegalArgumentException("Invalid parameters format");
            }
        } else {
            throw new IllegalArgumentException("Invalid parameters format");
        }
        return params;
    }

    /**
     * No-op constructor.
     */
    public Parameters() {}

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
        this.setIdentifier(new Identifier(Reference.decode(identifier)));
        this.setRegion(Region.fromUri(region));
        this.setSize(Size.fromUri(size));
        this.setRotation(Rotation.fromUri(rotation));
        this.setQuality(Quality.valueOf(quality.toUpperCase()));
        this.setOutputFormat(OutputFormat.valueOf(format.toUpperCase()));
    }

    @Override
    public int compareTo(Parameters params) {
        int last = this.toString().compareTo(params.toString());
        return (last == 0) ? this.toString().compareTo(params.toString()) : last;
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

    public Size getSize() {
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

    public void setSize(Size size) {
        this.size = size;
    }

    public Operations toOperations() {
        Operations ops = new Operations();
        ops.setIdentifier(getIdentifier());
        ops.setOutputFormat(getOutputFormat());
        ops.add(getRegion().toCrop());
        ops.add(getSize().toScale());
        ops.add(getRotation().toRotate());
        ops.add(getQuality().toFilter());
        return ops;
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
