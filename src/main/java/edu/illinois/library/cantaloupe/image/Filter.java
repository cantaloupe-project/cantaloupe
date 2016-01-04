package edu.illinois.library.cantaloupe.image;

import java.awt.Dimension;

public enum Filter implements Operation {

    BITONAL, GRAY;

    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    public boolean isNoOp() {
        return false;
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
