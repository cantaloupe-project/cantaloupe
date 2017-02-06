package edu.illinois.library.cantaloupe.operation;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ColorUtil {

    private static final Pattern rgbPattern =
            Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");

    public static Color fromString(final String string) {
        // Check for #rgb, #rrggbb, etc.
        try {
            String str = string.replace("#", "");
            return new Color(Integer.parseInt(str, 16));
        } catch (NumberFormatException e) {
            // Fine
        }

        // Check for rgb(r, g, b)
        Matcher m = rgbPattern.matcher(string.replace(" ", ""));
        if (m.matches()) {
            return new Color(Integer.valueOf(m.group(1)),
                    Integer.valueOf(m.group(2)),
                    Integer.valueOf(m.group(3)));
        }

        // Check for java.awt.Color constants
        try {
            Field field = Class.forName("java.awt.Color").getField(string);
            return (Color) field.get(null);
        } catch (Exception e) {
            // Fine
        }

        throw new IllegalArgumentException("Unrecognized color string: " + string);
    }

    /**
     * @return Hexadecimal value of the color.
     */
    public static String getHex(Color color) {
        return "#" + toHex(color.getRed()) + toHex(color.getGreen()) +
                toHex(color.getBlue());
    }

    /**
     * @return rgba(r,g,b,a) value of the color.
     */
    public static String getRGBA(Color color) {
        return String.format("rgba(%d, %d, %d, %d)", color.getRed(),
                color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private static String toHex(int number) {
        StringBuilder builder = new StringBuilder(Integer.toHexString(number & 0xff));
        while (builder.length() < 2) {
            builder.append("0");
        }
        return builder.toString().toUpperCase();
    }

}
