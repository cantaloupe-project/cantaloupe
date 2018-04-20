package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.Reference;

/**
 * Encapsulates the parameters of a request URI.
 *
 * @see <a href="http://iiif.io/api/image/1.1/#parameters">IIIF Image API
 * 1.1</a>
 */
class Parameters {

    private Format outputFormat;
    private Identifier identifier;
    private Quality quality;
    private Region region;
    private Rotation rotation;
    private Size size;

    /**
     * @param paramsStr URI path fragment beginning from the identifier onward
     * @throws IllegalClientArgumentException if the argument does not have the
     *         correct format, or any of its components are invalid.
     */
    public static Parameters fromUri(String paramsStr) {
        Parameters params = new Parameters();
        String[] parts = StringUtils.split(paramsStr, "/");
        try {
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
                    throw new IllegalClientArgumentException("Invalid parameters format");
                }
            } else {
                throw new IllegalClientArgumentException("Invalid parameters format");
            }
        } catch (IllegalClientArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalClientArgumentException(e.getMessage(), e);
        }
        return params;
    }

    /**
     * No-op constructor.
     */
    public Parameters() {}

    /**
     * @param identifier Decoded identifier.
     * @param region From URI
     * @param size From URI
     * @param rotation From URI
     * @param quality From URI
     * @param format From URI
     * @throws UnsupportedOutputFormatException if the {@literal format}
     *         argument is invalid.
     */
    public Parameters(Identifier identifier,
                      String region,
                      String size,
                      String rotation,
                      String quality,
                      String format) {
        setIdentifier(identifier);
        setRegion(Region.fromUri(region));
        setSize(Size.fromUri(size));
        setRotation(Rotation.fromUri(rotation));
        try {
            setQuality(Quality.valueOf(quality.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalClientArgumentException(e.getMessage(), e);
        }
        try {
            setOutputFormat(Format.valueOf(format.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new UnsupportedOutputFormatException(format);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Parameters) {
            return obj.toString().equals(toString());
        }
        return super.equals(obj);
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

    @Override
    public int hashCode() {
        return toString().hashCode();
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
     * @return Analog of the request parameters for processing, excluding any
     *         additional operations that may need to be performed, such as
     *         overlays, etc.
     */
    OperationList toOperationList() {
        OperationList ops = new OperationList(getIdentifier());

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
        ops.add(new Encode(getOutputFormat()));

        return ops;
    }

    /**
     * @return URI parameters with no leading slash.
     */
    public String toString() {
        return String.format("%s/%s/%s/%s/%s.%s", getIdentifier(), getRegion(),
                getSize(), getRotation(), getQuality().toString().toLowerCase(),
                getOutputFormat());
    }

}
