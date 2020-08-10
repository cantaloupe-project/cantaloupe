package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.operation.ColorTransform;

/**
 * Encapsulates the rotation component of a URI.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#44-quality">IIIF Image API
 * 3.0: Quality</a>
 */
enum Quality {

    BITONAL("bitonal"),
    COLOR("color"),
    DEFAULT("default"),
    GRAY("gray");

    private final String uriValue;

    Quality(String uriValue) {
        this.uriValue = uriValue;
    }

    public String getURIValue() {
        return uriValue;
    }

    public ColorTransform toColorTransform() {
        switch (this) {
            case BITONAL:
                return ColorTransform.BITONAL;
            case GRAY:
                return ColorTransform.GRAY;
            default:
                return null;
        }
    }

}
