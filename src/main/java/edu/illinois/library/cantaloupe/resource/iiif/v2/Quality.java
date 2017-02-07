package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.ColorTransform;

public enum Quality {

    BITONAL, COLOR, DEFAULT, GRAY;

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
