package edu.illinois.library.cantaloupe.operation;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ColorUtil {

    private static final Map<String, int[]> namedRGBColors = new HashMap<>();
    private static final Pattern rgbPattern =
            Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
    private static final Pattern rgbaPattern =
            Pattern.compile("rgba *\\( *([0-9]+), *([0-9]+), *([0-9]+), *([0-9]+) *\\)");

    static {
        // CSS Level 1 colors
        namedRGBColors.put("black", new int[] {0, 0, 0});
        namedRGBColors.put("silver", new int[] {192, 192, 192});
        namedRGBColors.put("gray", new int[] {128, 128, 128});
        namedRGBColors.put("white", new int[] {255, 255, 255});
        namedRGBColors.put("maroon", new int[] {128, 0, 0});
        namedRGBColors.put("red", new int[] {255, 0, 0});
        namedRGBColors.put("purple", new int[] {128, 0, 128});
        namedRGBColors.put("fuchsia", new int[] {255, 0, 255});
        namedRGBColors.put("green", new int[] {0, 128, 0});
        namedRGBColors.put("lime", new int[] {0, 255, 0});
        namedRGBColors.put("olive", new int[] {128, 128, 0});
        namedRGBColors.put("yellow", new int[] {255, 255, 0});
        namedRGBColors.put("navy", new int[] {0, 0, 128});
        namedRGBColors.put("blue", new int[] {0, 0, 255});
        namedRGBColors.put("teal", new int[] {0, 128, 128});
        namedRGBColors.put("aqua", new int[] {0, 255, 255});

        // CSS Level 2 colors
        namedRGBColors.put("orange", new int[] {255, 165, 0});

        // CSS Level 3 colors
        namedRGBColors.put("aliceblue", new int[] {240, 248, 255});
        namedRGBColors.put("antiquewhite", new int[] {250, 235, 215});
        namedRGBColors.put("aquamarine", new int[] {127, 255, 212});
        namedRGBColors.put("azure", new int[] {240, 255, 255});
        namedRGBColors.put("beige", new int[] {245, 245, 220});
        namedRGBColors.put("bisque", new int[] {255, 228, 196});
        namedRGBColors.put("blanchedalmond", new int[] {255, 235, 205});
        namedRGBColors.put("blueviolet", new int[] {138, 43, 226});
        namedRGBColors.put("brown", new int[] {165, 42, 42});
        namedRGBColors.put("burlywood", new int[] {222, 184, 135});
        namedRGBColors.put("cadetblue", new int[] {95, 158, 160});
        namedRGBColors.put("chartreuse", new int[] {127, 255, 0});
        namedRGBColors.put("chocolate", new int[] {210, 105, 30});
        namedRGBColors.put("coral", new int[] {255, 127, 80});
        namedRGBColors.put("cornflowerblue", new int[] {100, 149, 237});
        namedRGBColors.put("cornsilk", new int[] {255, 248, 220});
        namedRGBColors.put("crimson", new int[] {220, 20, 60});
        namedRGBColors.put("darkblue", new int[] {0, 0, 139});
        namedRGBColors.put("darkcyan", new int[] {0, 139, 139});
        namedRGBColors.put("darkgoldenrod", new int[] {184, 134, 11});
        namedRGBColors.put("darkgray", new int[] {169, 169, 169});
        namedRGBColors.put("darkgreen", new int[] {0, 100, 0});
        namedRGBColors.put("darkgrey", new int[] {169, 169, 169});
        namedRGBColors.put("darkkhaki", new int[] {169, 183, 107});
        namedRGBColors.put("darkmagenta", new int[] {139, 0, 139});
        namedRGBColors.put("darkolivegreen", new int[] {85, 107, 47});
        namedRGBColors.put("darkorange", new int[] {255, 140, 0});
        namedRGBColors.put("darkorchid", new int[] {153, 50, 204});
        namedRGBColors.put("darkred", new int[] {139, 0, 0});
        namedRGBColors.put("darksalmon", new int[] {233, 150, 122});
        namedRGBColors.put("darkseagreen", new int[] {143, 188, 143});
        namedRGBColors.put("darkslateblue", new int[] {72, 61, 139});
        namedRGBColors.put("darkslategray", new int[] {47, 79, 79});
        namedRGBColors.put("darkslategrey", new int[] {47, 79, 79});
        namedRGBColors.put("darkturquoise", new int[] {0, 206, 209});
        namedRGBColors.put("darkviolet", new int[] {148, 0, 211});
        namedRGBColors.put("deeppink", new int[] {255, 20, 147});
        namedRGBColors.put("deepskyblue", new int[] {0, 191, 255});
        namedRGBColors.put("dimgray", new int[] {105, 105, 105});
        namedRGBColors.put("dimgrey", new int[] {105, 105, 105});
        namedRGBColors.put("dodgerblue", new int[] {30, 144, 255});
        namedRGBColors.put("firebrick", new int[] {178, 34, 34});
        namedRGBColors.put("floralwhite", new int[] {255, 250, 240});
        namedRGBColors.put("forestgreen", new int[] {34, 139, 34});
        namedRGBColors.put("gainsboro", new int[] {220, 220, 220});
        namedRGBColors.put("ghostwhite", new int[] {248, 248, 255});
        namedRGBColors.put("gold", new int[] {255, 215, 0});
        namedRGBColors.put("goldenrod", new int[] {218, 165, 32});
        namedRGBColors.put("greenyellow", new int[] {173, 255, 47});
        namedRGBColors.put("grey", new int[] {128, 128, 128});
        namedRGBColors.put("honeydew", new int[] {240, 255, 240});
        namedRGBColors.put("hotpink", new int[] {255, 105, 180});
        namedRGBColors.put("indianred", new int[] {205, 92, 92});
        namedRGBColors.put("indigo", new int[] {75, 0, 130});
        namedRGBColors.put("ivory", new int[] {255, 255, 240});
        namedRGBColors.put("khaki", new int[] {240, 230, 140});
        namedRGBColors.put("lavender", new int[] {230, 230, 250});
        namedRGBColors.put("lavenderblush", new int[] {255, 240, 245});
        namedRGBColors.put("lawngreen", new int[] {124, 252, 0});
        namedRGBColors.put("lemonchiffon", new int[] {255, 250, 205});
        namedRGBColors.put("lightblue", new int[] {173, 216, 230});
        namedRGBColors.put("lightcoral", new int[] {240, 128, 128});
        namedRGBColors.put("lightcyan", new int[] {224, 255, 255});
        namedRGBColors.put("lightgoldenrodyellow", new int[] {250, 250, 210});
        namedRGBColors.put("lightgray", new int[] {211, 211, 211});
        namedRGBColors.put("lightgreen", new int[] {144, 238, 144});
        namedRGBColors.put("lightgrey", new int[] {211, 211, 211});
        namedRGBColors.put("lightpink", new int[] {255, 182, 193});
        namedRGBColors.put("lightsalmon", new int[] {255, 160, 122});
        namedRGBColors.put("lightseagreen", new int[] {32, 178, 170});
        namedRGBColors.put("lightskyblue", new int[] {135, 206, 250});
        namedRGBColors.put("lightslategray", new int[] {119, 136, 153});
        namedRGBColors.put("lightslategrey", new int[] {119, 136, 153});
        namedRGBColors.put("lightsteelblue", new int[] {176, 196, 222});
        namedRGBColors.put("lightyellow", new int[] {255, 255, 224});
        namedRGBColors.put("limegreen", new int[] {50, 205, 50});
        namedRGBColors.put("linen", new int[] {250, 240, 230});
        namedRGBColors.put("mediumaquamarine", new int[] {102, 205, 170});
        namedRGBColors.put("mediumblue", new int[] {0, 0, 205});
        namedRGBColors.put("mediumorchid", new int[] {186, 85, 211});
        namedRGBColors.put("mediumpurple", new int[] {147, 112, 219});
        namedRGBColors.put("mediumseagreen", new int[] {60, 179, 113});
        namedRGBColors.put("mediumslateblue", new int[] {123, 104, 238});
        namedRGBColors.put("mediumspringgreen", new int[] {0, 250, 154});
        namedRGBColors.put("mediumturquoise", new int[] {72, 209, 204});
        namedRGBColors.put("mediumvioletred", new int[] {199, 21, 133});
        namedRGBColors.put("midnightblue", new int[] {25, 25, 112});
        namedRGBColors.put("mintcream", new int[] {245, 255, 250});
        namedRGBColors.put("mistyrose", new int[] {255, 228, 225});
        namedRGBColors.put("moccasin", new int[] {255, 228, 181});
        namedRGBColors.put("navajowhite", new int[] {255, 222, 173});
        namedRGBColors.put("oldlace", new int[] {253, 245, 230});
        namedRGBColors.put("olivedrab", new int[] {107, 142, 35});
        namedRGBColors.put("orangered", new int[] {255, 69, 0});
        namedRGBColors.put("orchid", new int[] {218, 112, 214});
        namedRGBColors.put("palegoldenrod", new int[] {238, 232, 170});
        namedRGBColors.put("palegreen", new int[] {152, 251, 152});
        namedRGBColors.put("paleturquoise", new int[] {175, 238, 238});
        namedRGBColors.put("palevioletred", new int[] {219, 112, 147});
        namedRGBColors.put("papayawhip", new int[] {255, 239, 213});
        namedRGBColors.put("peachpuff", new int[] {255, 218, 185});
        namedRGBColors.put("peru", new int[] {205, 133, 63});
        namedRGBColors.put("pink", new int[] {255, 192, 203});
        namedRGBColors.put("plum", new int[] {221, 160, 221});
        namedRGBColors.put("powderblue", new int[] {176, 224, 230});
        namedRGBColors.put("rosybrown", new int[] {188, 143, 143});
        namedRGBColors.put("royalblue", new int[] {65, 105, 225});
        namedRGBColors.put("saddlebrown", new int[] {139, 69, 19});
        namedRGBColors.put("salmon", new int[] {250, 128, 114});
        namedRGBColors.put("sandybrown", new int[] {244, 164, 96});
        namedRGBColors.put("seagreen", new int[] {46, 139, 87});
        namedRGBColors.put("seashell", new int[] {255, 245, 238});
        namedRGBColors.put("sienna", new int[] {160, 82, 45});
        namedRGBColors.put("skyblue", new int[] {135, 206, 235});
        namedRGBColors.put("slateblue", new int[] {106, 90, 205});
        namedRGBColors.put("slategray", new int[] {112, 128, 144});
        namedRGBColors.put("slategrey", new int[] {112, 128, 144});
        namedRGBColors.put("snow", new int[] {255, 250, 250});
        namedRGBColors.put("springgreen", new int[] {0, 255, 127});
        namedRGBColors.put("steelblue", new int[] {70, 130, 180});
        namedRGBColors.put("tan", new int[] {210, 180, 140});
        namedRGBColors.put("thistle", new int[] {216, 191, 216});
        namedRGBColors.put("tomato", new int[] {255, 99, 71});
        namedRGBColors.put("turquoise", new int[] {64, 224, 208});
        namedRGBColors.put("violet", new int[] {238, 130, 238});
        namedRGBColors.put("wheat", new int[] {245, 222, 179});
        namedRGBColors.put("whitesmoke", new int[] {245, 245, 245});
        namedRGBColors.put("yellowgreen", new int[] {154, 205, 50});

        // CSS Level 4 colors
        namedRGBColors.put("rebeccapurple", new int[] {102, 51, 153});
    }

    public static Color fromString(final String string) {
        // Check for hexadecimal.
        if ("#".equals(string.substring(0, 1))) {
            int r = 0, g = 0, b = 0, a = 255;
            switch (string.length()) {
                case 4: // #rgb
                    r = Integer.parseInt(string.substring(1, 2), 16) * 16;
                    g = Integer.parseInt(string.substring(2, 3), 16) * 16;
                    b = Integer.parseInt(string.substring(3, 4), 16) * 16;
                    break;
                case 5: // #rgba
                    r = Integer.parseInt(string.substring(1, 2), 16) * 16;
                    g = Integer.parseInt(string.substring(2, 3), 16) * 16;
                    b = Integer.parseInt(string.substring(3, 4), 16) * 16;
                    a = Integer.parseInt(string.substring(4, 5), 16) * 16;
                    break;
                case 7: // #rrggbb
                    r = Integer.parseInt(string.substring(1, 3), 16);
                    g = Integer.parseInt(string.substring(3, 5), 16);
                    b = Integer.parseInt(string.substring(5, 7), 16);
                    break;
                case 9: // #rrggbbaa
                    r = Integer.parseInt(string.substring(1, 3), 16);
                    g = Integer.parseInt(string.substring(3, 5), 16);
                    b = Integer.parseInt(string.substring(5, 7), 16);
                    a = Integer.parseInt(string.substring(7, 9), 16);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Illegal color value: " + string);
            }
            return new Color(r, g, b, a);
        }

        // Check for a color name.
        if (string.matches("[a-z]+")) {
            final int[] rgb = namedRGBColors.get(string);
            if (rgb != null) {
                return new Color(rgb[0], rgb[1], rgb[2]);
            } else {
                throw new IllegalArgumentException(
                        "Unrecognized color name: " + string);
            }
        }

        // Check for rgb(r, g, b).
        Matcher m = rgbPattern.matcher(string.replace(" ", ""));
        if (m.matches()) {
            return new Color(Integer.valueOf(m.group(1)),
                    Integer.valueOf(m.group(2)),
                    Integer.valueOf(m.group(3)));
        }

        // Check for rgba(r, g, b, a).
        m = rgbaPattern.matcher(string.replace(" ", ""));
        if (m.matches()) {
            return new Color(Integer.valueOf(m.group(1)),
                    Integer.valueOf(m.group(2)),
                    Integer.valueOf(m.group(3)),
                    Integer.valueOf(m.group(4)));
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

    private static String toHex(int number) {
        StringBuilder builder = new StringBuilder(Integer.toHexString(number & 0xff));
        while (builder.length() < 2) {
            builder.append("0");
        }
        return builder.toString().toUpperCase();
    }

}
