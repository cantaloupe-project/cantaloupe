package edu.illinois.library.cantaloupe.image;

public enum Quality implements Operation {

    BITONAL, COLOR, DEFAULT, GRAY;

    public boolean isNoOp() {
        return (this == DEFAULT || this == COLOR);
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
