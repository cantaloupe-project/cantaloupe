package edu.illinois.library.cantaloupe.operation;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Eight-bits-per-component RGBA color.
 */
public final class Color {

    public static final Color BLACK = new Color(0, 0, 0);
    public static final Color BLUE = new Color(0, 0, 255);
    public static final Color GREEN = new Color(0, 128, 0);
    public static final Color ORANGE = new Color(255, 165, 0);
    public static final Color RED = new Color(255, 0, 0);
    public static final Color WHITE = new Color(255, 255, 255);

    private static final Map<String, int[]> CSS_COLORS = Map.ofEntries(
            // CSS Level 1 colors
            Map.entry("black", new int[] {0, 0, 0}),
            Map.entry("silver", new int[] {192, 192, 192}),
            Map.entry("gray", new int[] {128, 128, 128}),
            Map.entry("white", new int[] {255, 255, 255}),
            Map.entry("maroon", new int[] {128, 0, 0}),
            Map.entry("red", new int[] {255, 0, 0}),
            Map.entry("purple", new int[] {128, 0, 128}),
            Map.entry("fuchsia", new int[] {255, 0, 255}),
            Map.entry("green", new int[] {0, 128, 0}),
            Map.entry("lime", new int[] {0, 255, 0}),
            Map.entry("olive", new int[] {128, 128, 0}),
            Map.entry("yellow", new int[] {255, 255, 0}),
            Map.entry("navy", new int[] {0, 0, 128}),
            Map.entry("blue", new int[] {0, 0, 255}),
            Map.entry("teal", new int[] {0, 128, 128}),
            Map.entry("aqua", new int[] {0, 255, 255}),
            // CSS Level 2 colors
            Map.entry("orange", new int[] {255, 165, 0}),
            // CSS Level 3 colors
            Map.entry("aliceblue", new int[] {240, 248, 255}),
            Map.entry("antiquewhite", new int[] {250, 235, 215}),
            Map.entry("aquamarine", new int[] {127, 255, 212}),
            Map.entry("azure", new int[] {240, 255, 255}),
            Map.entry("beige", new int[] {245, 245, 220}),
            Map.entry("bisque", new int[] {255, 228, 196}),
            Map.entry("blanchedalmond", new int[] {255, 235, 205}),
            Map.entry("blueviolet", new int[] {138, 43, 226}),
            Map.entry("brown", new int[] {165, 42, 42}),
            Map.entry("burlywood", new int[] {222, 184, 135}),
            Map.entry("cadetblue", new int[] {95, 158, 160}),
            Map.entry("chartreuse", new int[] {127, 255, 0}),
            Map.entry("chocolate", new int[] {210, 105, 30}),
            Map.entry("coral", new int[] {255, 127, 80}),
            Map.entry("cornflowerblue", new int[] {100, 149, 237}),
            Map.entry("cornsilk", new int[] {255, 248, 220}),
            Map.entry("crimson", new int[] {220, 20, 60}),
            Map.entry("darkblue", new int[] {0, 0, 139}),
            Map.entry("darkcyan", new int[] {0, 139, 139}),
            Map.entry("darkgoldenrod", new int[] {184, 134, 11}),
            Map.entry("darkgray", new int[] {169, 169, 169}),
            Map.entry("darkgreen", new int[] {0, 100, 0}),
            Map.entry("darkgrey", new int[] {169, 169, 169}),
            Map.entry("darkkhaki", new int[] {169, 183, 107}),
            Map.entry("darkmagenta", new int[] {139, 0, 139}),
            Map.entry("darkolivegreen", new int[] {85, 107, 47}),
            Map.entry("darkorange", new int[] {255, 140, 0}),
            Map.entry("darkorchid", new int[] {153, 50, 204}),
            Map.entry("darkred", new int[] {139, 0, 0}),
            Map.entry("darksalmon", new int[] {233, 150, 122}),
            Map.entry("darkseagreen", new int[] {143, 188, 143}),
            Map.entry("darkslateblue", new int[] {72, 61, 139}),
            Map.entry("darkslategray", new int[] {47, 79, 79}),
            Map.entry("darkslategrey", new int[] {47, 79, 79}),
            Map.entry("darkturquoise", new int[] {0, 206, 209}),
            Map.entry("darkviolet", new int[] {148, 0, 211}),
            Map.entry("deeppink", new int[] {255, 20, 147}),
            Map.entry("deepskyblue", new int[] {0, 191, 255}),
            Map.entry("dimgray", new int[] {105, 105, 105}),
            Map.entry("dimgrey", new int[] {105, 105, 105}),
            Map.entry("dodgerblue", new int[] {30, 144, 255}),
            Map.entry("firebrick", new int[] {178, 34, 34}),
            Map.entry("floralwhite", new int[] {255, 250, 240}),
            Map.entry("forestgreen", new int[] {34, 139, 34}),
            Map.entry("gainsboro", new int[] {220, 220, 220}),
            Map.entry("ghostwhite", new int[] {248, 248, 255}),
            Map.entry("gold", new int[] {255, 215, 0}),
            Map.entry("goldenrod", new int[] {218, 165, 32}),
            Map.entry("greenyellow", new int[] {173, 255, 47}),
            Map.entry("grey", new int[] {128, 128, 128}),
            Map.entry("honeydew", new int[] {240, 255, 240}),
            Map.entry("hotpink", new int[] {255, 105, 180}),
            Map.entry("indianred", new int[] {205, 92, 92}),
            Map.entry("indigo", new int[] {75, 0, 130}),
            Map.entry("ivory", new int[] {255, 255, 240}),
            Map.entry("khaki", new int[] {240, 230, 140}),
            Map.entry("lavender", new int[] {230, 230, 250}),
            Map.entry("lavenderblush", new int[] {255, 240, 245}),
            Map.entry("lawngreen", new int[] {124, 252, 0}),
            Map.entry("lemonchiffon", new int[] {255, 250, 205}),
            Map.entry("lightblue", new int[] {173, 216, 230}),
            Map.entry("lightcoral", new int[] {240, 128, 128}),
            Map.entry("lightcyan", new int[] {224, 255, 255}),
            Map.entry("lightgoldenrodyellow", new int[] {250, 250, 210}),
            Map.entry("lightgray", new int[] {211, 211, 211}),
            Map.entry("lightgreen", new int[] {144, 238, 144}),
            Map.entry("lightgrey", new int[] {211, 211, 211}),
            Map.entry("lightpink", new int[] {255, 182, 193}),
            Map.entry("lightsalmon", new int[] {255, 160, 122}),
            Map.entry("lightseagreen", new int[] {32, 178, 170}),
            Map.entry("lightskyblue", new int[] {135, 206, 250}),
            Map.entry("lightslategray", new int[] {119, 136, 153}),
            Map.entry("lightslategrey", new int[] {119, 136, 153}),
            Map.entry("lightsteelblue", new int[] {176, 196, 222}),
            Map.entry("lightyellow", new int[] {255, 255, 224}),
            Map.entry("limegreen", new int[] {50, 205, 50}),
            Map.entry("linen", new int[] {250, 240, 230}),
            Map.entry("mediumaquamarine", new int[] {102, 205, 170}),
            Map.entry("mediumblue", new int[] {0, 0, 205}),
            Map.entry("mediumorchid", new int[] {186, 85, 211}),
            Map.entry("mediumpurple", new int[] {147, 112, 219}),
            Map.entry("mediumseagreen", new int[] {60, 179, 113}),
            Map.entry("mediumslateblue", new int[] {123, 104, 238}),
            Map.entry("mediumspringgreen", new int[] {0, 250, 154}),
            Map.entry("mediumturquoise", new int[] {72, 209, 204}),
            Map.entry("mediumvioletred", new int[] {199, 21, 133}),
            Map.entry("midnightblue", new int[] {25, 25, 112}),
            Map.entry("mintcream", new int[] {245, 255, 250}),
            Map.entry("mistyrose", new int[] {255, 228, 225}),
            Map.entry("moccasin", new int[] {255, 228, 181}),
            Map.entry("navajowhite", new int[] {255, 222, 173}),
            Map.entry("oldlace", new int[] {253, 245, 230}),
            Map.entry("olivedrab", new int[] {107, 142, 35}),
            Map.entry("orangered", new int[] {255, 69, 0}),
            Map.entry("orchid", new int[] {218, 112, 214}),
            Map.entry("palegoldenrod", new int[] {238, 232, 170}),
            Map.entry("palegreen", new int[] {152, 251, 152}),
            Map.entry("paleturquoise", new int[] {175, 238, 238}),
            Map.entry("palevioletred", new int[] {219, 112, 147}),
            Map.entry("papayawhip", new int[] {255, 239, 213}),
            Map.entry("peachpuff", new int[] {255, 218, 185}),
            Map.entry("peru", new int[] {205, 133, 63}),
            Map.entry("pink", new int[] {255, 192, 203}),
            Map.entry("plum", new int[] {221, 160, 221}),
            Map.entry("powderblue", new int[] {176, 224, 230}),
            Map.entry("rosybrown", new int[] {188, 143, 143}),
            Map.entry("royalblue", new int[] {65, 105, 225}),
            Map.entry("saddlebrown", new int[] {139, 69, 19}),
            Map.entry("salmon", new int[] {250, 128, 114}),
            Map.entry("sandybrown", new int[] {244, 164, 96}),
            Map.entry("seagreen", new int[] {46, 139, 87}),
            Map.entry("seashell", new int[] {255, 245, 238}),
            Map.entry("sienna", new int[] {160, 82, 45}),
            Map.entry("skyblue", new int[] {135, 206, 235}),
            Map.entry("slateblue", new int[] {106, 90, 205}),
            Map.entry("slategray", new int[] {112, 128, 144}),
            Map.entry("slategrey", new int[] {112, 128, 144}),
            Map.entry("snow", new int[] {255, 250, 250}),
            Map.entry("springgreen", new int[] {0, 255, 127}),
            Map.entry("steelblue", new int[] {70, 130, 180}),
            Map.entry("tan", new int[] {210, 180, 140}),
            Map.entry("thistle", new int[] {216, 191, 216}),
            Map.entry("tomato", new int[] {255, 99, 71}),
            Map.entry("turquoise", new int[] {64, 224, 208}),
            Map.entry("violet", new int[] {238, 130, 238}),
            Map.entry("wheat", new int[] {245, 222, 179}),
            Map.entry("whitesmoke", new int[] {245, 245, 245}),
            Map.entry("yellowgreen", new int[] {154, 205, 50}),
            // CSS Level 4 colors
            Map.entry("rebeccapurple", new int[] {102, 51, 153}));

    private static final Pattern CSS_RGB_PATTERN =
            Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
    private static final Pattern CSS_RGBA_PATTERN =
            Pattern.compile("rgba *\\( *([0-9]+), *([0-9]+), *([0-9]+), *([0-9]+) *\\)");

    private int rgba;

    /**
     * Alternative to {@link java.awt.Color#decode(String)} that supports
     * RGB(A) and named color CSS syntax.
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/color">color</a>
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/color_value">color</a>
     *
     * @param string Hexadecimal, named, or functional color.
     * @return       New instance corresponding to the given string.
     * @throws IllegalArgumentException
     */
    public static Color fromString(final String string) {
        // Check for hexadecimal.
        if ("#".equals(string.substring(0, 1))) {
            int r, g, b, a = 255;
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
            final int[] rgb = CSS_COLORS.get(string);
            if (rgb != null) {
                return new Color(rgb[0], rgb[1], rgb[2]);
            } else {
                throw new IllegalArgumentException(
                        "Unrecognized color name: " + string);
            }
        }

        // Check for rgb(r, g, b).
        Matcher m = CSS_RGB_PATTERN.matcher(string.replace(" ", ""));
        if (m.matches()) {
            return new Color(Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)));
        }

        // Check for rgba(r, g, b, a).
        m = CSS_RGBA_PATTERN.matcher(string.replace(" ", ""));
        if (m.matches()) {
            return new Color(Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(4)));
        }

        throw new IllegalArgumentException("Unrecognized color string: " + string);
    }

    private static String toHex(int number) {
        StringBuilder builder = new StringBuilder(Integer.toHexString(number & 0xff));
        while (builder.length() < 2) {
            builder.append("0");
        }
        return builder.toString().toUpperCase();
    }

    private static void validateComponent(int component) {
        if (component < 0 || component > 255) {
            throw new IllegalArgumentException(
                    "Component must be in the range (0-255).");
        }
    }

    public Color(int rgba) {
        this.rgba = rgba;
    }

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(int r, int g, int b, int a) {
        validateComponent(r);
        validateComponent(g);
        validateComponent(b);
        validateComponent(a);
        this.rgba = ((r & 0xff) << 24) |
                ((g & 0xff) << 16) |
                ((b & 0xff) << 8) |
                (a & 0xff);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Color) {
            final Color color = (Color) obj;
            return color.getRGBA() == getRGBA();
        }
        return super.equals(obj);
    }

    /**
     * @return Red value in the range (0-255).
     */
    public int getRed() {
        return (rgba >> 24) & 0xff;
    }

    /**
     * @return Green value in the range (0-255).
     */
    public int getGreen() {
        return (rgba >> 16) & 0xff;
    }

    /**
     * @return Blue value in the range (0-255).
     */
    public int getBlue() {
        return (rgba >> 8) & 0xff;
    }

    /**
     * @return Alpha value in the range (0-255).
     */
    public int getAlpha() {
        return rgba & 0xff;
    }

    /**
     * @return RGBA integer.
     */
    public int getRGBA() {
        return rgba;
    }

    @Override
    public int hashCode() {
        return toRGBAHex().hashCode();
    }

    public java.awt.Color toColor() {
        return new java.awt.Color(getRed(), getGreen(), getBlue(), getAlpha());
    }

    /**
     * @return String RGB hexadecimal value of the instance.
     */
    public String toRGBHex() {
        return "#" + toHex(getRed()) + toHex(getGreen()) + toHex(getBlue());
    }

    /**
     * @return String RGBA hexadecimal value of the color.
     */
    public String toRGBAHex() {
        return "#" + toHex(getRed()) + toHex(getGreen()) + toHex(getBlue()) +
                toHex(getAlpha());
    }

}
