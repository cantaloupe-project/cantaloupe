package edu.illinois.library.cantaloupe.image;

public enum Filter implements Operation {

    BITONAL, NONE, GRAY;

    public boolean isNoOp() {
        return (this == NONE);
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
