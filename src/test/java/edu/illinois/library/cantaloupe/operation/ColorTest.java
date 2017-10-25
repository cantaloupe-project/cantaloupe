package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class ColorTest extends BaseTest {

    /* fromString(String) */

    @Test
    public void testFromStringWithNull() {
        assertNull(Color.fromString(null));
    }

    @Test
    public void testFromStringWithCSSLevel1ColorName() {
        assertEquals(new Color(0, 0, 0), Color.fromString("black"));
        assertEquals(new Color(192, 192, 192), Color.fromString("silver"));
        assertEquals(new Color(128, 128, 128), Color.fromString("gray"));
        assertEquals(new Color(255, 255, 255), Color.fromString("white"));
        assertEquals(new Color(128, 0, 0), Color.fromString("maroon"));
        assertEquals(new Color(255, 0, 0), Color.fromString("red"));
        assertEquals(new Color(128, 0, 128), Color.fromString("purple"));
        assertEquals(new Color(255, 0, 255), Color.fromString("fuchsia"));
        assertEquals(new Color(0, 128, 0), Color.fromString("green"));
        assertEquals(new Color(0, 255, 0), Color.fromString("lime"));
        assertEquals(new Color(128, 128, 0), Color.fromString("olive"));
        assertEquals(new Color(255, 255, 0), Color.fromString("yellow"));
        assertEquals(new Color(0, 0, 128), Color.fromString("navy"));
        assertEquals(new Color(0, 0, 255), Color.fromString("blue"));
        assertEquals(new Color(0, 128, 128), Color.fromString("teal"));
        assertEquals(new Color(0, 255, 255), Color.fromString("aqua"));
    }

    @Test
    public void testFromStringWithCSSLevel2ColorName() {
        assertEquals(new Color(255, 165, 0), Color.fromString("orange"));
    }

    @Test
    public void testFromStringWithCSSLevel3ColorName() {
        assertEquals(new Color(240, 248, 255), Color.fromString("aliceblue"));
        assertEquals(new Color(250, 235, 215), Color.fromString("antiquewhite"));
        assertEquals(new Color(127, 255, 212), Color.fromString("aquamarine"));
        assertEquals(new Color(240, 255, 255), Color.fromString("azure"));
        assertEquals(new Color(245, 245, 220), Color.fromString("beige"));
        assertEquals(new Color(255, 228, 196), Color.fromString("bisque"));
        assertEquals(new Color(255, 235, 205), Color.fromString("blanchedalmond"));
        assertEquals(new Color(138, 43, 226), Color.fromString("blueviolet"));
        assertEquals(new Color(165, 42, 42), Color.fromString("brown"));
        assertEquals(new Color(222, 184, 135), Color.fromString("burlywood"));
        assertEquals(new Color(95, 158, 160), Color.fromString("cadetblue"));
        assertEquals(new Color(127, 255, 0), Color.fromString("chartreuse"));
        assertEquals(new Color(210, 105, 30), Color.fromString("chocolate"));
        assertEquals(new Color(255, 127, 80), Color.fromString("coral"));
        assertEquals(new Color(100, 149, 237), Color.fromString("cornflowerblue"));
        assertEquals(new Color(255, 248, 220), Color.fromString("cornsilk"));
        assertEquals(new Color(220, 20, 60), Color.fromString("crimson"));
        assertEquals(new Color(0, 0, 139), Color.fromString("darkblue"));
        assertEquals(new Color(0, 139, 139), Color.fromString("darkcyan"));
        assertEquals(new Color(184, 134, 11), Color.fromString("darkgoldenrod"));
        assertEquals(new Color(169, 169, 169), Color.fromString("darkgray"));
        assertEquals(new Color(0, 100, 0), Color.fromString("darkgreen"));
        assertEquals(new Color(169, 169, 169), Color.fromString("darkgrey"));
        assertEquals(new Color(169, 183, 107), Color.fromString("darkkhaki"));
        assertEquals(new Color(139, 0, 139), Color.fromString("darkmagenta"));
        assertEquals(new Color(85, 107, 47), Color.fromString("darkolivegreen"));
        assertEquals(new Color(255, 140, 0), Color.fromString("darkorange"));
        assertEquals(new Color(153, 50, 204), Color.fromString("darkorchid"));
        assertEquals(new Color(139, 0, 0), Color.fromString("darkred"));
        assertEquals(new Color(233, 150, 122), Color.fromString("darksalmon"));
        assertEquals(new Color(143, 188, 143), Color.fromString("darkseagreen"));
        assertEquals(new Color(72, 61, 139), Color.fromString("darkslateblue"));
        assertEquals(new Color(47, 79, 79), Color.fromString("darkslategray"));
        assertEquals(new Color(47, 79, 79), Color.fromString("darkslategrey"));
        assertEquals(new Color(0, 206, 209), Color.fromString("darkturquoise"));
        assertEquals(new Color(148, 0, 211), Color.fromString("darkviolet"));
        assertEquals(new Color(255, 20, 147), Color.fromString("deeppink"));
        assertEquals(new Color(0, 191, 255), Color.fromString("deepskyblue"));
        assertEquals(new Color(105, 105, 105), Color.fromString("dimgray"));
        assertEquals(new Color(105, 105, 105), Color.fromString("dimgrey"));
        assertEquals(new Color(30, 144, 255), Color.fromString("dodgerblue"));
        assertEquals(new Color(178, 34, 34), Color.fromString("firebrick"));
        assertEquals(new Color(255, 250, 240), Color.fromString("floralwhite"));
        assertEquals(new Color(34, 139, 34), Color.fromString("forestgreen"));
        assertEquals(new Color(220, 220, 220), Color.fromString("gainsboro"));
        assertEquals(new Color(248, 248, 255), Color.fromString("ghostwhite"));
        assertEquals(new Color(255, 215, 0), Color.fromString("gold"));
        assertEquals(new Color(218, 165, 32), Color.fromString("goldenrod"));
        assertEquals(new Color(173, 255, 47), Color.fromString("greenyellow"));
        assertEquals(new Color(128, 128, 128), Color.fromString("grey"));
        assertEquals(new Color(240, 255, 240), Color.fromString("honeydew"));
        assertEquals(new Color(255, 105, 180), Color.fromString("hotpink"));
        assertEquals(new Color(205, 92, 92), Color.fromString("indianred"));
        assertEquals(new Color(75, 0, 130), Color.fromString("indigo"));
        assertEquals(new Color(255, 255, 240), Color.fromString("ivory"));
        assertEquals(new Color(240, 230, 140), Color.fromString("khaki"));
        assertEquals(new Color(230, 230, 250), Color.fromString("lavender"));
        assertEquals(new Color(255, 240, 245), Color.fromString("lavenderblush"));
        assertEquals(new Color(124, 252, 0), Color.fromString("lawngreen"));
        assertEquals(new Color(255, 250, 205), Color.fromString("lemonchiffon"));
        assertEquals(new Color(173, 216, 230), Color.fromString("lightblue"));
        assertEquals(new Color(240, 128, 128), Color.fromString("lightcoral"));
        assertEquals(new Color(224, 255, 255), Color.fromString("lightcyan"));
        assertEquals(new Color(250, 250, 210), Color.fromString("lightgoldenrodyellow"));
        assertEquals(new Color(211, 211, 211), Color.fromString("lightgray"));
        assertEquals(new Color(144, 238, 144), Color.fromString("lightgreen"));
        assertEquals(new Color(211, 211, 211), Color.fromString("lightgrey"));
        assertEquals(new Color(255, 182, 193), Color.fromString("lightpink"));
        assertEquals(new Color(255, 160, 122), Color.fromString("lightsalmon"));
        assertEquals(new Color(32, 178, 170), Color.fromString("lightseagreen"));
        assertEquals(new Color(135, 206, 250), Color.fromString("lightskyblue"));
        assertEquals(new Color(119, 136, 153), Color.fromString("lightslategray"));
        assertEquals(new Color(119, 136, 153), Color.fromString("lightslategrey"));
        assertEquals(new Color(176, 196, 222), Color.fromString("lightsteelblue"));
        assertEquals(new Color(255, 255, 224), Color.fromString("lightyellow"));
        assertEquals(new Color(50, 205, 50), Color.fromString("limegreen"));
        assertEquals(new Color(250, 240, 230), Color.fromString("linen"));
        assertEquals(new Color(102, 205, 170), Color.fromString("mediumaquamarine"));
        assertEquals(new Color(0, 0, 205), Color.fromString("mediumblue"));
        assertEquals(new Color(186, 85, 211), Color.fromString("mediumorchid"));
        assertEquals(new Color(147, 112, 219), Color.fromString("mediumpurple"));
        assertEquals(new Color(60, 179, 113), Color.fromString("mediumseagreen"));
        assertEquals(new Color(123, 104, 238), Color.fromString("mediumslateblue"));
        assertEquals(new Color(0, 250, 154), Color.fromString("mediumspringgreen"));
        assertEquals(new Color(72, 209, 204), Color.fromString("mediumturquoise"));
        assertEquals(new Color(199, 21, 133), Color.fromString("mediumvioletred"));
        assertEquals(new Color(25, 25, 112), Color.fromString("midnightblue"));
        assertEquals(new Color(245, 255, 250), Color.fromString("mintcream"));
        assertEquals(new Color(255, 228, 225), Color.fromString("mistyrose"));
        assertEquals(new Color(255, 228, 181), Color.fromString("moccasin"));
        assertEquals(new Color(255, 222, 173), Color.fromString("navajowhite"));
        assertEquals(new Color(253, 245, 230), Color.fromString("oldlace"));
        assertEquals(new Color(107, 142, 35), Color.fromString("olivedrab"));
        assertEquals(new Color(255, 69, 0), Color.fromString("orangered"));
        assertEquals(new Color(218, 112, 214), Color.fromString("orchid"));
        assertEquals(new Color(238, 232, 170), Color.fromString("palegoldenrod"));
        assertEquals(new Color(152, 251, 152), Color.fromString("palegreen"));
        assertEquals(new Color(175, 238, 238), Color.fromString("paleturquoise"));
        assertEquals(new Color(219, 112, 147), Color.fromString("palevioletred"));
        assertEquals(new Color(255, 239, 213), Color.fromString("papayawhip"));
        assertEquals(new Color(255, 218, 185), Color.fromString("peachpuff"));
        assertEquals(new Color(205, 133, 63), Color.fromString("peru"));
        assertEquals(new Color(255, 192, 203), Color.fromString("pink"));
        assertEquals(new Color(221, 160, 221), Color.fromString("plum"));
        assertEquals(new Color(176, 224, 230), Color.fromString("powderblue"));
        assertEquals(new Color(188, 143, 143), Color.fromString("rosybrown"));
        assertEquals(new Color(65, 105, 225), Color.fromString("royalblue"));
        assertEquals(new Color(139, 69, 19), Color.fromString("saddlebrown"));
        assertEquals(new Color(250, 128, 114), Color.fromString("salmon"));
        assertEquals(new Color(244, 164, 96), Color.fromString("sandybrown"));
        assertEquals(new Color(46, 139, 87), Color.fromString("seagreen"));
        assertEquals(new Color(255, 245, 238), Color.fromString("seashell"));
        assertEquals(new Color(160, 82, 45), Color.fromString("sienna"));
        assertEquals(new Color(135, 206, 235), Color.fromString("skyblue"));
        assertEquals(new Color(106, 90, 205), Color.fromString("slateblue"));
        assertEquals(new Color(112, 128, 144), Color.fromString("slategray"));
        assertEquals(new Color(112, 128, 144), Color.fromString("slategrey"));
        assertEquals(new Color(255, 250, 250), Color.fromString("snow"));
        assertEquals(new Color(0, 255, 127), Color.fromString("springgreen"));
        assertEquals(new Color(70, 130, 180), Color.fromString("steelblue"));
        assertEquals(new Color(210, 180, 140), Color.fromString("tan"));
        assertEquals(new Color(216, 191, 216), Color.fromString("thistle"));
        assertEquals(new Color(255, 99, 71), Color.fromString("tomato"));
        assertEquals(new Color(64, 224, 208), Color.fromString("turquoise"));
        assertEquals(new Color(238, 130, 238), Color.fromString("violet"));
        assertEquals(new Color(245, 222, 179), Color.fromString("wheat"));
        assertEquals(new Color(245, 245, 245), Color.fromString("whitesmoke"));
        assertEquals(new Color(154, 205, 50), Color.fromString("yellowgreen"));
    }

    @Test
    public void testFromStringWithCSSLevel4ColorName() {
        assertEquals(new Color(102, 51, 153), Color.fromString("rebeccapurple"));
    }

    @Test
    public void testFromStringWithInvalidColorName() {
        try {
            Color.fromString("bogus");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRGBHex() {
        // valid
        assertEquals(new Color(192, 192, 192), Color.fromString("#ccc"));

        // invalid
        try {
            Color.fromString("#ggg");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRGBAHex() {
        // valid
        assertEquals(new Color(192, 192, 192, 192), Color.fromString("#cccc"));

        // invalid
        try {
            Color.fromString("#gggg");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRRGGBBHex() {
        // valid
        assertEquals(new Color(12, 23, 34), Color.fromString("#0c1722"));

        // invalid
        try {
            Color.fromString("#fgfgfg");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRRGGBBAAHex() {
        // valid
        assertEquals(new Color(12, 23, 34, 45), Color.fromString("#0c17222d"));

        // invalid
        try {
            Color.fromString("#fgfgfgfg");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRGBNotation() {
        // valid
        assertEquals(new Color(12, 23, 34),
                Color.fromString("rgb(12, 23, 34)"));

        // invalid
        try {
            Color.fromString("rgb(280, 280, 280)");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testFromStringWithRGBANotation() {
        // valid
        assertEquals(new Color(12, 23, 34, 45),
                Color.fromString("rgba(12, 23, 34, 45)"));

        // invalid
        try {
            Color.fromString("rgb(280, 280, 280, 280)");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testEquals() {
        Color color = new Color(1, 2, 3, 4);
        assertTrue(color.equals(new Color(1, 2, 3, 4)));
        assertFalse(color.equals(new Color(1, 2, 3)));
        assertFalse(color.equals(new Color(2, 3, 4, 5)));

        color = new Color(1, 2, 3, 255);
        assertTrue(color.equals(new Color(1, 2, 3)));
        assertFalse(color.equals(new Color(1, 2, 3, 4)));
    }

    @Test
    public void testGetRGBHex() {
        assertEquals("#C01722", new Color(12, 23, 34, 45).toRGBHex());
    }

    @Test
    public void testGetRGBAHex() {
        assertEquals("#C017222D", new Color(12, 23, 34, 45).toRGBAHex());
    }

    @Test
    public void testToColor() {
        assertEquals(new java.awt.Color(1, 2, 3, 4),
                new Color(1, 2, 3, 4).toColor());
    }

}
