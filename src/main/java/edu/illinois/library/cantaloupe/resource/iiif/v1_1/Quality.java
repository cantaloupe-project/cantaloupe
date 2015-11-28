package edu.illinois.library.cantaloupe.resource.iiif.v1_1;

import edu.illinois.library.cantaloupe.image.Filter;

public enum Quality {

    BITONAL, COLOR, GRAY, NATIVE;

    public Filter toFilter() {
        switch (this) {
            case BITONAL:
                return Filter.BITONAL;
            case GRAY:
                return Filter.GRAY;
            default:
                return Filter.DEFAULT;
        }
    }

}
