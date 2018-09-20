package edu.illinois.library.cantaloupe.status;

/**
 * <p>Application health, generally obtained from the {@link HealthChecker}.</p>
 *
 * <p>This class is thread-safe.</p>
 */
public final class Health {

    public enum Color {
        GREEN, YELLOW, RED
    }

    private Color color = Color.GREEN;
    private String message;

    public synchronized Color getColor() {
        return color;
    }

    public synchronized String getMessage() {
        return message;
    }

    /**
     * For Jackson JSON serialization.
     */
    public Color[] getPossibleColors() {
        return Color.values();
    }

    synchronized void setMessage(String message) {
        this.message = message;
    }

    /**
     * Sets the minimum color to the given value. Subsequent calls cannot set
     * the color to any lower value.
     */
    synchronized void setMinColor(Color minColor) {
        if (minColor.ordinal() > color.ordinal()) {
            this.color = minColor;
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getColor());
        if (getMessage() != null) {
            builder.append(": ");
            builder.append(getMessage());
        }
        return builder.toString();
    }

}
