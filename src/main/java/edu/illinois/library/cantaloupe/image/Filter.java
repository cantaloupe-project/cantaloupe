package edu.illinois.library.cantaloupe.image;

import java.awt.Dimension;

public enum Filter implements Operation {

    BITONAL, NONE, GRAY;

    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    public boolean isNoOp() {
        return (this == NONE);
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
