package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest extends BaseTest {

    @Test
    void testDecodeSlashes() {
        Configuration.getInstance().setProperty(Key.SLASH_SUBSTITUTE, "$$");
        assertEquals("cats", StringUtils.decodeSlashes("cats"));
        assertEquals("ca/ts", StringUtils.decodeSlashes("ca$$ts"));
    }

    @Test
    void testEscapeHTML() {
        String html = "the quick brown <script type=\"text/javascript\">alert('hi');</script> fox";
        String expected = "the quick brown &#60;script type=&#34;text/javascript&#34;&#62;alert('hi');&#60;/script&#62; fox";
        assertEquals(expected, StringUtils.escapeHTML(html));
    }

    @Test
    void testMD5() {
        assertEquals("0832c1202da8d382318e329a7c133ea0",
                StringUtils.md5("cats"));
    }

    @Test
    void testRemoveTrailingZeroes() {
        // with floats
        assertEquals("0", StringUtils.removeTrailingZeroes(0.0f));
        assertEquals("0.5", StringUtils.removeTrailingZeroes(0.5f));
        assertEquals("50", StringUtils.removeTrailingZeroes(50.0f));
        assertEquals("50.5", StringUtils.removeTrailingZeroes(50.5f));
        assertEquals("50.5", StringUtils.removeTrailingZeroes(50.50f));
        assertTrue(StringUtils.removeTrailingZeroes(50.5555555555555f).length() <= 13);

        // with doubles
        assertEquals("0", StringUtils.removeTrailingZeroes(0.0));
        assertEquals("0.5", StringUtils.removeTrailingZeroes(0.5));
        assertEquals("50", StringUtils.removeTrailingZeroes(50.0));
        assertEquals("50.5", StringUtils.removeTrailingZeroes(50.5));
        assertEquals("50.5", StringUtils.removeTrailingZeroes(50.50));
        assertTrue(StringUtils.removeTrailingZeroes(50.5555555555555).length() <= 13);
    }

    @Test
    void testReverse() {
        assertEquals("321stac", StringUtils.reverse("cats123"));
    }

    @Test
    void testSanitize1() {
        assertEquals("", StringUtils.sanitize("dirt", "dirt", "dirt"));
        assertEquals("y", StringUtils.sanitize("dirty", "dirt", "dirt"));
        assertEquals("dirty", StringUtils.sanitize("dir1ty", "1", "1"));

        // test injection
        assertEquals("", StringUtils.sanitize("cacacatststs", "cats", "cats"));
        assertEquals("", StringUtils.sanitize("cadocadogstsgsts", "cats", "dogs", "foxes"));
    }

    @Test
    void testSanitize2() {
        assertEquals("", StringUtils.sanitize("dirt", Pattern.compile("dirt")));
        assertEquals("y", StringUtils.sanitize("dirty", Pattern.compile("dirt")));
        assertEquals("dirty", StringUtils.sanitize("dir1ty", Pattern.compile("1")));

        // test injection
        assertEquals("", StringUtils.sanitize("cacacatststs",
                Pattern.compile("cats")));
        assertEquals("", StringUtils.sanitize("cadocadogstsgsts",
                Pattern.compile("cats"), Pattern.compile("dogs")));
    }

    @Test
    void testStripEndWithMatch() {
        String str = "ababab";
        String toStrip = "ab";
        assertEquals("abab", StringUtils.stripEnd(str, toStrip));
    }

    @Test
    void testStripEndWithoutMatch() {
        String str = "ababab";
        String toStrip = "c";
        assertSame(str, StringUtils.stripEnd(str, toStrip));

        toStrip = "longer than str";
        assertSame(str, StringUtils.stripEnd(str, toStrip));
    }

    @Test
    void testStripStartWithMatch() {
        String str = "abcdefg";
        String toStrip = "ab";
        assertEquals("cdefg", StringUtils.stripStart(str, toStrip));
    }

    @Test
    void testStripStartWithoutMatch() {
        String str = "ababab";
        String toStrip = "c";
        assertSame(str, StringUtils.stripStart(str, toStrip));

        toStrip = "longer than str";
        assertSame(str, StringUtils.stripStart(str, toStrip));
    }

    @Test
    void testToBooleanWithNullValue() {
        assertThrows(NumberFormatException.class,
                () -> StringUtils.toBoolean(null));
    }

    @Test
    void testToBooleanWithUnrecognizedValue() {
        assertThrows(NumberFormatException.class,
                () -> StringUtils.toBoolean("cats"));
    }

    @Test
    void testToBooleanWithRecognizedValue() {
        assertFalse(StringUtils.toBoolean("0"));
        assertFalse(StringUtils.toBoolean("false"));
        assertTrue(StringUtils.toBoolean("1"));
        assertTrue(StringUtils.toBoolean("true"));
    }

    @Test
    void testToByteSizeWithIllegalArgument() {
        assertThrows(NumberFormatException.class,
                () -> StringUtils.toByteSize("cats"));
    }

    @Test
    void testToByteSizeWithNumber() {
        assertEquals(254254254, StringUtils.toByteSize("254254254"));
        assertEquals(255, StringUtils.toByteSize("254.9"));
        assertEquals(-255, StringUtils.toByteSize("-254.9"));
    }

    @Test
    void testToByteSizeWithKB() {
        long expected = 25 * 1024;
        assertEquals(expected, StringUtils.toByteSize("25K"));
        assertEquals(expected, StringUtils.toByteSize("25KB"));
        assertEquals(expected, StringUtils.toByteSize("25k"));
        assertEquals(expected, StringUtils.toByteSize("25kb"));
        assertEquals(expected, StringUtils.toByteSize("25 K"));
        assertEquals(expected, StringUtils.toByteSize("25 KB"));
        assertEquals(expected, StringUtils.toByteSize("25 k"));
        assertEquals(expected, StringUtils.toByteSize("25 kb"));
    }

    @Test
    void testToByteSizeWithMB() {
        long expected = 25 * (long) Math.pow(1024, 2);
        assertEquals(expected, StringUtils.toByteSize("25M"));
        assertEquals(expected, StringUtils.toByteSize("25MB"));
        assertEquals(expected, StringUtils.toByteSize("25m"));
        assertEquals(expected, StringUtils.toByteSize("25mb"));
        assertEquals(expected, StringUtils.toByteSize("25 M"));
        assertEquals(expected, StringUtils.toByteSize("25 MB"));
        assertEquals(expected, StringUtils.toByteSize("25 m"));
        assertEquals(expected, StringUtils.toByteSize("25 mb"));
    }

    @Test
    void testToByteSizeWithGB() {
        long expected = 25 * (long) Math.pow(1024, 3);
        assertEquals(expected, StringUtils.toByteSize("25G"));
        assertEquals(expected, StringUtils.toByteSize("25GB"));
        assertEquals(expected, StringUtils.toByteSize("25g"));
        assertEquals(expected, StringUtils.toByteSize("25gb"));
        assertEquals(expected, StringUtils.toByteSize("25 G"));
        assertEquals(expected, StringUtils.toByteSize("25 GB"));
        assertEquals(expected, StringUtils.toByteSize("25 g"));
        assertEquals(expected, StringUtils.toByteSize("25 gb"));
    }

    @Test
    void testToByteSizeWithTB() {
        long expected = 25 * (long) Math.pow(1024, 4);
        assertEquals(expected, StringUtils.toByteSize("25T"));
        assertEquals(expected, StringUtils.toByteSize("25TB"));
        assertEquals(expected, StringUtils.toByteSize("25t"));
        assertEquals(expected, StringUtils.toByteSize("25tb"));
        assertEquals(expected, StringUtils.toByteSize("25 T"));
        assertEquals(expected, StringUtils.toByteSize("25 TB"));
        assertEquals(expected, StringUtils.toByteSize("25 t"));
        assertEquals(expected, StringUtils.toByteSize("25 tb"));
    }

    @Test
    void testToByteSizeWithPB() {
        long expected = 25 * (long) Math.pow(1024, 5);
        assertEquals(expected, StringUtils.toByteSize("25P"));
        assertEquals(expected, StringUtils.toByteSize("25PB"));
        assertEquals(expected, StringUtils.toByteSize("25p"));
        assertEquals(expected, StringUtils.toByteSize("25pb"));
        assertEquals(expected, StringUtils.toByteSize("25 P"));
        assertEquals(expected, StringUtils.toByteSize("25 PB"));
        assertEquals(expected, StringUtils.toByteSize("25 p"));
        assertEquals(expected, StringUtils.toByteSize("25 pb"));
    }

    @Test
    void testTrimXMPWithTrimmableXMP() {
        String xmp = "<?xpacket id=\"cats\"?>" +
                "<x:xmpmeta bla=\"dogs\">" +
                "<rdf:RDF foxes=\"bugs\">" +
                "</rdf:RDF>" +
                "</x:xmpmeta>";
        String result = StringUtils.trimXMP(xmp);
        assertTrue(result.startsWith("<rdf:RDF"));
        assertTrue(result.endsWith("</rdf:RDF>"));
    }

    @Test
    void testTrimXMPWithNonTrimmableXMP() {
        String xmp = "<rdf:RDF foxes=\"bugs\">" +
                "</rdf:RDF>";
        String result = StringUtils.trimXMP(xmp);
        assertSame(xmp, result);
    }

    @Test
    void testTrimXMPWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> StringUtils.trimXMP(null));
    }

    @Test
    void testWrap() {
        String str = "This is a very very very very very very very very long line.";
        final int maxWidth        = 200;
        final BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d      = image.createGraphics();

        final Map<TextAttribute, Object> attributes = Map.of(
                TextAttribute.FAMILY, "Helvetica",
                TextAttribute.SIZE, 18,
                TextAttribute.WEIGHT, 1,
                TextAttribute.TRACKING, 0);
        final Font font = Font.getFont(attributes);
        g2d.setFont(font);
        final FontMetrics fm = g2d.getFontMetrics();

        List<String> lines = StringUtils.wrap(str, fm, maxWidth);
        assertTrue(lines.size() > 2 && lines.size() < 6);
        assertTrue(lines.get(0).length() > 10 && lines.get(0).length() < 30);
        assertTrue(lines.get(1).length() > 10 && lines.get(1).length() < 30);
        assertTrue(lines.get(2).length() > 5 && lines.get(2).length() < 30);
    }

}
