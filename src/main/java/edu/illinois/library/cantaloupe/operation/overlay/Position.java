package edu.illinois.library.cantaloupe.operation.overlay;

import org.apache.commons.lang3.StringUtils;

public enum Position {

    TOP_LEFT("NW"),
    TOP_CENTER("N"),
    TOP_RIGHT("NE"),
    LEFT_CENTER("W"),
    CENTER("C"),
    RIGHT_CENTER("E"),
    BOTTOM_LEFT("SW"),
    BOTTOM_CENTER("S"),
    BOTTOM_RIGHT("SE");

    private final String shortName;

    /**
     * @param positionStr Position string such as "top left," "left top",
     *                    "center", etc.
     * @return Position corresponding to the given string, or null if not
     *         found.
     */
    public static Position fromString(final String positionStr) {
        final String normalizedString = StringUtils.
                replace(positionStr, " ", "_").trim().toLowerCase();
        final String[] normalizedParts = StringUtils.
                split(normalizedString, "_");

        // check for an exact match
        for (Position pos : Position.values()) {
            if (normalizedString.equals(pos.name().toLowerCase())) {
                return pos;
            }
        }

        // check for a word match
        for (Position pos : Position.values()) {
            for (int i = 0; i < normalizedParts.length; i++) {
                if (!StringUtils.contains(pos.name().toLowerCase(),
                        normalizedParts[i])) {
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
