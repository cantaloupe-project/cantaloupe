package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.ParameterList;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.Reference;

import java.awt.Dimension;
import java.net.URL;
import java.util.Map;

/**
 * Encapsulates the parameters of an IIIF request.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#parameters">IIIF Image API
 * 1.1</a>
 */
class Parameters implements ParameterList, Comparable<Parameters> {

    private Format outputFormat;
    private Identifier identifier;
    private Quality quality;
    private Region region;
    private Rotation rotation;
    private Size size;

    /**
     * @param paramsStr URI path fragment beginning from the identifier onward
     * @throws IllegalArgumentException if the given string does not have the
     * correct format
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
                params.setOutputFormat(Format.valueOf(subparts[1].toUpperCase()));
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
     * @param identifier Encoded URI value
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
        this.setOutputFormat(Format.valueOf(format.toUpperCase()));
    }

    @Override
    public int compareTo(Parameters params) {
        int last = this.toString().compareTo(params.toString());
        return (last == 0) ? this.toString().compareTo(params.toString()) : last;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Format getOutputFormat() {
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

    public void setOutputFormat(Format outputFormat) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationList toOperationList() {
        OperationList ops = new OperationList();
        ops.setIdentifier(getIdentifier());
        ops.setOutputFormat(getOutputFormat());
        if (!getRegion().isFull()) {
            ops.add(getRegion().toCrop());
        }
        if (!Size.ScaleMode.FULL.equals(getSize().getScaleMode())) {
            ops.add(getSize().toScale());
        }
        if (getRotation().getDegrees() != 0) {
            ops.add(getRotation().toRotate());
        }
        ops.add(getQuality().toColorTransform());
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
