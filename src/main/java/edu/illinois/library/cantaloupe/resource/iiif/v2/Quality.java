package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.ColorTransform;

public enum Quality {

    BITONAL("bitonal"), COLOR("color"), DEFAULT("default"), GRAY("gray");

    private String uriValue;

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
