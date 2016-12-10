package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.Color;

public enum Quality {

    BITONAL, COLOR, DEFAULT, GRAY;

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
