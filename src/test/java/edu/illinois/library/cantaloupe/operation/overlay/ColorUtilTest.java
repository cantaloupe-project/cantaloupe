package edu.illinois.library.cantaloupe.operation.overlay;

import edu.illinois.library.cantaloupe.operation.ColorUtil;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

/**
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/color">color</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/color_value">color</a>
 */
public class ColorUtilTest extends BaseTest {

    /* fromString(String) */

    @Test
    public void testFromStringWithCSSLevel1ColorName() {
        assertEquals(new Color(0, 0, 0), ColorUtil.fromString("black"));
        assertEquals(new Color(192, 192, 192), ColorUtil.fromString("silver"));
        assertEquals(new Color(128, 128, 128), ColorUtil.fromString("gray"));
        assertEquals(new Color(255, 255, 255), ColorUtil.fromString("white"));
        assertEquals(new Color(128, 0, 0), ColorUtil.fromString("maroon"));
        assertEquals(new Color(255, 0, 0), ColorUtil.fromString("red"));
        assertEquals(new Color(128, 0, 128), ColorUtil.fromString("purple"));
        assertEquals(new Color(255, 0, 255), ColorUtil.fromString("fuchsia"));
        assertEquals(new Color(0, 128, 0), ColorUtil.fromString("green"));
        assertEquals(new Color(0, 255, 0), ColorUtil.fromString("lime"));
        assertEquals(new Color(128, 128, 0), ColorUtil.fromString("olive"));
        assertEquals(new Color(255, 255, 0), ColorUtil.fromString("yellow"));
        assertEquals(new Color(0, 0, 128), ColorUtil.fromString("navy"));
        assertEquals(new Color(0, 0, 255), ColorUtil.fromString("blue"));
        assertEquals(new Color(0, 128, 128), ColorUtil.fromString("teal"));
        assertEquals(new Color(0, 255, 255), ColorUtil.fromString("aqua"));
    }

    @Test
    public void testFromStringWithCSSLevel2ColorName() {
        assertEquals(new Color(255, 165, 0), ColorUtil.fromString("orange"));
    }

    @Test
    public void testFromStringWithCSSLevel3ColorName() {
        assertEquals(new Color(240, 248, 255), ColorUtil.fromString("aliceblue"));
        assertEquals(new Color(250, 235, 215), ColorUtil.fromString("antiquewhite"));
        assertEquals(new Color(127, 255, 212), ColorUtil.fromString("aquamarine"));
        assertEquals(new Color(240, 255, 255), ColorUtil.fromString("azure"));
        assertEquals(new Color(245, 245, 220), ColorUtil.fromString("beige"));
        assertEquals(new Color(255, 228, 196), ColorUtil.fromString("bisque"));
        assertEquals(new Color(255, 235, 205), ColorUtil.fromString("blanchedalmond"));
        assertEquals(new Color(138, 43, 226), ColorUtil.fromString("blueviolet"));
        assertEquals(new Color(165, 42, 42), ColorUtil.fromString("brown"));
        assertEquals(new Color(222, 184, 135), ColorUtil.fromString("burlywood"));
        assertEquals(new Color(95, 158, 160), ColorUtil.fromString("cadetblue"));
        assertEquals(new Color(127, 255, 0), ColorUtil.fromString("chartreuse"));
        assertEquals(new Color(210, 105, 30), ColorUtil.fromString("chocolate"));
        assertEquals(new Color(255, 127, 80), ColorUtil.fromString("coral"));
        assertEquals(new Color(100, 149, 237), ColorUtil.fromString("cornflowerblue"));
        assertEquals(new Color(255, 248, 220), ColorUtil.fromString("cornsilk"));
        assertEquals(new Color(220, 20, 60), ColorUtil.fromString("crimson"));
        assertEquals(new Color(0, 0, 139), ColorUtil.fromString("darkblue"));
        assertEquals(new Color(0, 139, 139), ColorUtil.fromString("darkcyan"));
        assertEquals(new Color(184, 134, 11), ColorUtil.fromString("darkgoldenrod"));
        assertEquals(new Color(169, 169, 169), ColorUtil.fromString("darkgray"));
        assertEquals(new Color(0, 100, 0), ColorUtil.fromString("darkgreen"));
        assertEquals(new Color(169, 169, 169), ColorUtil.fromString("darkgrey"));
        assertEquals(new Color(169, 183, 107), ColorUtil.fromString("darkkhaki"));
        assertEquals(new Color(139, 0, 139), ColorUtil.fromString("darkmagenta"));
        assertEquals(new Color(85, 107, 47), ColorUtil.fromString("darkolivegreen"));
        assertEquals(new Color(255, 140, 0), ColorUtil.fromString("darkorange"));
        assertEquals(new Color(153, 50, 204), ColorUtil.fromString("darkorchid"));
        assertEquals(new Color(139, 0, 0), ColorUtil.fromString("darkred"));
        assertEquals(new Color(233, 150, 122), ColorUtil.fromString("darksalmon"));
        assertEquals(new Color(143, 188, 143), ColorUtil.fromString("darkseagreen"));
        assertEquals(new Color(72, 61, 139), ColorUtil.fromString("darkslateblue"));
        assertEquals(new Color(47, 79, 79), ColorUtil.fromString("darkslategray"));
        assertEquals(new Color(47, 79, 79), ColorUtil.fromString("darkslategrey"));
        assertEquals(new Color(0, 206, 209), ColorUtil.fromString("darkturquoise"));
        assertEquals(new Color(148, 0, 211), ColorUtil.fromString("darkviolet"));
        assertEquals(new Color(255, 20, 147), ColorUtil.fromString("deeppink"));
        assertEquals(new Color(0, 191, 255), ColorUtil.fromString("deepskyblue"));
        assertEquals(new Color(105, 105, 105), ColorUtil.fromString("dimgray"));
        assertEquals(new Color(105, 105, 105), ColorUtil.fromString("dimgrey"));
        assertEquals(new Color(30, 144, 255), ColorUtil.fromString("dodgerblue"));
        assertEquals(new Color(178, 34, 34), ColorUtil.fromString("firebrick"));
        assertEquals(new Color(255, 250, 240), ColorUtil.fromString("floralwhite"));
        assertEquals(new Color(34, 139, 34), ColorUtil.fromString("forestgreen"));
        assertEquals(new Color(220, 220, 220), ColorUtil.fromString("gainsboro"));
        assertEquals(new Color(248, 248, 255), ColorUtil.fromString("ghostwhite"));
        assertEquals(new Color(255, 215, 0), ColorUtil.fromString("gold"));
        assertEquals(new Color(218, 165, 32), ColorUtil.fromString("goldenrod"));
        assertEquals(new Color(173, 255, 47), ColorUtil.fromString("greenyellow"));
        assertEquals(new Color(128, 128, 128), ColorUtil.fromString("grey"));
        assertEquals(new Color(240, 255, 240), ColorUtil.fromString("honeydew"));
        assertEquals(new Color(255, 105, 180), ColorUtil.fromString("hotpink"));
        assertEquals(new Color(205, 92, 92), ColorUtil.fromString("indianred"));
        assertEquals(new Color(75, 0, 130), ColorUtil.fromString("indigo"));
        assertEquals(new Color(255, 255, 240), ColorUtil.fromString("ivory"));
        assertEquals(new Color(240, 230, 140), ColorUtil.fromString("khaki"));
        assertEquals(new Color(230, 230, 250), ColorUtil.fromString("lavender"));
        assertEquals(new Color(255, 240, 245), ColorUtil.fromString("lavenderblush"));
        assertEquals(new Color(124, 252, 0), ColorUtil.fromString("lawngreen"));
        assertEquals(new Color(255, 250, 205), ColorUtil.fromString("lemonchiffon"));
        assertEquals(new Color(173, 216, 230), ColorUtil.fromString("lightblue"));
        assertEquals(new Color(240, 128, 128), ColorUtil.fromString("lightcoral"));
        assertEquals(new Color(224, 255, 255), ColorUtil.fromString("lightcyan"));
        assertEquals(new Color(250, 250, 210), ColorUtil.fromString("lightgoldenrodyellow"));
        assertEquals(new Color(211, 211, 211), ColorUtil.fromString("lightgray"));
        assertEquals(new Color(144, 238, 144), ColorUtil.fromString("lightgreen"));
        assertEquals(new Color(211, 211, 211), ColorUtil.fromString("lightgrey"));
        assertEquals(new Color(255, 182, 193), ColorUtil.fromString("lightpink"));
        assertEquals(new Color(255, 160, 122), ColorUtil.fromString("lightsalmon"));
        assertEquals(new Color(32, 178, 170), ColorUtil.fromString("lightseagreen"));
        assertEquals(new Color(135, 206, 250), ColorUtil.fromString("lightskyblue"));
        assertEquals(new Color(119, 136, 153), ColorUtil.fromString("lightslategray"));
        assertEquals(new Color(119, 136, 153), ColorUtil.fromString("lightslategrey"));
        assertEquals(new Color(176, 196, 222), ColorUtil.fromString("lightsteelblue"));
        assertEquals(new Color(255, 255, 224), ColorUtil.fromString("lightyellow"));
        assertEquals(new Color(50, 205, 50), ColorUtil.fromString("limegreen"));
        assertEquals(new Color(250, 240, 230), ColorUtil.fromString("linen"));
        assertEquals(new Color(102, 205, 170), ColorUtil.fromString("mediumaquamarine"));
        assertEquals(new Color(0, 0, 205), ColorUtil.fromString("mediumblue"));
        assertEquals(new Color(186, 85, 211), ColorUtil.fromString("mediumorchid"));
        assertEquals(new Color(147, 112, 219), ColorUtil.fromString("mediumpurple"));
        assertEquals(new Color(60, 179, 113), ColorUtil.fromString("mediumseagreen"));
        assertEquals(new Color(123, 104, 238), ColorUtil.fromString("mediumslateblue"));
        assertEquals(new Color(0, 250, 154), ColorUtil.fromString("mediumspringgreen"));
        assertEquals(new Color(72, 209, 204), ColorUtil.fromString("mediumturquoise"));
        assertEquals(new Color(199, 21, 133), ColorUtil.fromString("mediumvioletred"));
        assertEquals(new Color(25, 25, 112), ColorUtil.fromString("midnightblue"));
        assertEquals(new Color(245, 255, 250), ColorUtil.fromString("mintcream"));
        assertEquals(new Color(255, 228, 225), ColorUtil.fromString("mistyrose"));
        assertEquals(new Color(255, 228, 181), ColorUtil.fromString("moccasin"));
        assertEquals(new Color(255, 222, 173), ColorUtil.fromString("navajowhite"));
        assertEquals(new Color(253, 245, 230), ColorUtil.fromString("oldlace"));
        assertEquals(new Color(107, 142, 35), ColorUtil.fromString("olivedrab"));
        assertEquals(new Color(255, 69, 0), ColorUtil.fromString("orangered"));
        assertEquals(new Color(218, 112, 214), ColorUtil.fromString("orchid"));
        assertEquals(new Color(238, 232, 170), ColorUtil.fromString("palegoldenrod"));
        assertEquals(new Color(152, 251, 152), ColorUtil.fromString("palegreen"));
        assertEquals(new Color(175, 238, 238), ColorUtil.fromString("paleturquoise"));
        assertEquals(new Color(219, 112, 147), ColorUtil.fromString("palevioletred"));
        assertEquals(new Color(255, 239, 213), ColorUtil.fromString("papayawhip"));
        assertEquals(new Color(255, 218, 185), ColorUtil.fromString("peachpuff"));
        assertEquals(new Color(205, 133, 63), ColorUtil.fromString("peru"));
        assertEquals(new Color(255, 192, 203), ColorUtil.fromString("pink"));
        assertEquals(new Color(221, 160, 221), ColorUtil.fromString("plum"));
        assertEquals(new Color(176, 224, 230), ColorUtil.fromString("powderblue"));
        assertEquals(new Color(188, 143, 143), ColorUtil.fromString("rosybrown"));
        assertEquals(new Color(65, 105, 225), ColorUtil.fromString("royalblue"));
        assertEquals(new Color(139, 69, 19), ColorUtil.fromString("saddlebrown"));
        assertEquals(new Color(250, 128, 114), ColorUtil.fromString("salmon"));
        assertEquals(new Color(244, 164, 96), ColorUtil.fromString("sandybrown"));
        assertEquals(new Color(46, 139, 87), ColorUtil.fromString("seagreen"));
        assertEquals(new Color(255, 245, 238), ColorUtil.fromString("seashell"));
        assertEquals(new Color(160, 82, 45), ColorUtil.fromString("sienna"));
        assertEquals(new Color(135, 206, 235), ColorUtil.fromString("skyblue"));
        assertEquals(new Color(106, 90, 205), ColorUtil.fromString("slateblue"));
        assertEquals(new Color(112, 128, 144), ColorUtil.fromString("slategray"));
        assertEquals(new Color(112, 128, 144), ColorUtil.fromString("slategrey"));
        assertEquals(new Color(255, 250, 250), ColorUtil.fromString("snow"));
        assertEquals(new Color(0, 255, 127), ColorUtil.fromString("springgreen"));
        assertEquals(new Color(70, 130, 180), ColorUtil.fromString("steelblue"));
        assertEquals(new Color(210, 180, 140), ColorUtil.fromString("tan"));
        assertEquals(new Color(216, 191, 216), ColorUtil.fromString("thistle"));
        assertEquals(new Color(255, 99, 71), ColorUtil.fromString("tomato"));
        assertEquals(new Color(64, 224, 208), ColorUtil.fromString("turquoise"));
        assertEquals(new Color(238, 130, 238), ColorUtil.fromString("violet"));
        assertEquals(new Color(245, 222, 179), ColorUtil.fromString("wheat"));
        assertEquals(new Color(245, 245, 245), ColorUtil.fromString("whitesmoke"));
        assertEquals(new Color(154, 205, 50), ColorUtil.fromString("yellowgreen"));
    }

    @Test
    public void testFromStringWithCSSLevel4ColorName() {
        assertEquals(new Color(102, 51, 153), ColorUtil.fromString("rebeccapurple"));
    }

    @Test
    public void testFromStringWithInvalidColorName() {
        try {
            ColorUtil.fromString("bogus");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRGBHex() {
        // valid
        assertEquals(new Color(192, 192, 192), ColorUtil.fromString("#ccc"));

        // invalid
        try {
            ColorUtil.fromString("#ggg");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRGBAHex() {
        // valid
        assertEquals(new Color(192, 192, 192, 192), ColorUtil.fromString("#cccc"));

        // invalid
        try {
            ColorUtil.fromString("#gggg");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRRGGBBHex() {
        // valid
        assertEquals(new Color(12, 23, 34), ColorUtil.fromString("#0c1722"));

        // invalid
        try {
            ColorUtil.fromString("#fgfgfg");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRRGGBBAAHex() {
        // valid
        assertEquals(new Color(12, 23, 34, 45), ColorUtil.fromString("#0c17222d"));

        // invalid
        try {
            ColorUtil.fromString("#fgfgfgfg");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRGBNotation() {
        // valid
        assertEquals(new Color(12, 23, 34),
                ColorUtil.fromString("rgb(12, 23, 34)"));

        // invalid
        try {
            ColorUtil.fromString("rgb(280, 280, 280)");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRGBANotation() {
        // valid
        assertEquals(new Color(12, 23, 34, 45),
                ColorUtil.fromString("rgba(12, 23, 34, 45)"));

        // invalid
        try {
            ColorUtil.fromString("rgb(280, 280, 280, 280)");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testGetRGBHex() {
        assertEquals("#C01722", ColorUtil.getRGBHex(new Color(12, 23, 34, 45)));
    }

    @Test
    public void testGetRGBAHex() {
        assertEquals("#C017222D", ColorUtil.getRGBAHex(new Color(12, 23, 34, 45)));
    }

}
