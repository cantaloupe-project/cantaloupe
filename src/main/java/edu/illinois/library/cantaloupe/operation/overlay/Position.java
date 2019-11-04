package edu.illinois.library.cantaloupe.operation.overlay;

public enum Position {

    TOP_LEFT("NW"),
    TOP_CENTER("N"),
    TOP_RIGHT("NE"),
    LEFT_CENTER("W"),
    CENTER("C"),
    RIGHT_CENTER("E"),
    BOTTOM_LEFT("SW"),
    BOTTOM_CENTER("S"),
    BOTTOM_RIGHT("SE"),
    REPEAT("REPEAT"),
    SCALED("SCALED");

    private final String shortName;

    /**
     * @param positionStr Position string such as "top left," "left top",
     *                    "center", etc.
     * @return Position corresponding to the given string, or null if not
     *         found.
     */
    public static Position fromString(final String positionStr) {
        final String normalizedString =
                positionStr.replaceAll(" ", "_").trim().toLowerCase();
        final String[] normalizedParts = normalizedString.split("_");

        // check for an exact match
        for (Position pos : Position.values()) {
            if (normalizedString.equals(pos.name().toLowerCase())) {
                return pos;
            }
        }

        // check for a word match
        for (Position pos : Position.values()) {
            for (int i = 0; i < normalizedParts.length; i++) {
                if (!pos.name().toLowerCase().contains(normalizedParts[i])) {
                    break;
                }
                if (i == normalizedParts.length - 1) {
                    return pos;
                }
            }
        }
        return null;
    }

    Position(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return this.shortName;
    }

}
