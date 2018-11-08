package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.operation.ColorTransform;

public enum Quality {

    BITONAL, COLOR, GREY, NATIVE;

    public ColorTransform toColorTransform() {
        switch (this) {
            case BITONAL:
                return ColorTransform.BITONAL;
            case GREY:
                return ColorTransform.GRAY;
            default:
                return null;
        }
    }

}
