package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Filter;

public enum Quality {

    BITONAL, COLOR, DEFAULT, GRAY;

    public Filter toFilter() {
        switch (this) {
            case BITONAL:
                return Filter.BITONAL;
            case GRAY:
                return Filter.GRAY;
            default:
                return null;
        }
    }

}
