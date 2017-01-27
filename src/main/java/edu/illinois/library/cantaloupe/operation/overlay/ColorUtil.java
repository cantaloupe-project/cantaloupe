package edu.illinois.library.cantaloupe.operation.overlay;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class ColorUtil {

    private static final Pattern rgbPattern =
            Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");

    static Color fromString(final String string) {
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
     * @return Hexadecimal value of the color in the sRGB color model.
     */
    static String getHex(Color color) {
        return "#" + toHex(color.getRed()) + toHex(color.getGreen()) +
                toHex(color.getBlue());
    }

    private static String toHex(int number) {
        StringBuilder builder = new StringBuilder(Integer.toHexString(number & 0xff));
        while (builder.length() < 2) {
            builder.append("0");
        }
        return builder.toString().toUpperCase();
    }

}
