package edu.illinois.library.cantaloupe.image;

public enum Filter implements Operation {

    BITONAL, DEFAULT, GRAY;

    public boolean isNoOp() {
        return (this == DEFAULT);
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
