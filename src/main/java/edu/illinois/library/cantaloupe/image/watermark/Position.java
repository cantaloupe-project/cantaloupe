package edu.illinois.library.cantaloupe.image.watermark;

public enum Position {

    TOP_LEFT("NW"),
    TOP_RIGHT("NE"),
    BOTTOM_LEFT("SW"),
    BOTTOM_RIGHT("SE"),
    TOP_CENTER("N"),
    BOTTOM_CENTER("S"),
    LEFT_CENTER("W"),
    RIGHT_CENTER("E"),
    CENTER("C");

    private final String shortName;

    Position(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return this.shortName;
    }

}
