package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.operation.Color;

public enum Quality {

    BITONAL, COLOR, GRAY, NATIVE;

    public Color toFilter() {
        switch (this) {
            case BITONAL:
                return Color.BITONAL;
            case GRAY:
                return Color.GRAY;
            default:
                return null;
        }
    }

}
