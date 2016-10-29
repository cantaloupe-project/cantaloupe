package edu.illinois.library.cantaloupe.image.watermark;

import java.awt.Color;

abstract class ColorUtil {

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
