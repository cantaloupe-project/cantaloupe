package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.operation.ColorTransform;

public enum Quality {

    BITONAL, COLOR, GRAY, NATIVE;

    public ColorTransform toFilter() {
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
