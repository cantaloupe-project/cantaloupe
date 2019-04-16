package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class StringUtilsTest extends BaseTest {

    @Test
    public void testDecodeSlashes() {
        Configuration.getInstance().setProperty(Key.SLASH_SUBSTITUTE, "$$");
        assertEquals("cats", StringUtils.decodeSlashes("cats"));
        assertEquals("ca/ts", StringUtils.decodeSlashes("ca$$ts"));
    }

    @Test
    public void testEscapeHTML() {
        String html = "the quick brown <script type=\"text/javascript\">alert('hi');</script> fox";
        String expected = "the quick brown &#60;script type=&#34;text/javascript&#34;&#62;alert('hi');&#60;/script&#62; fox";
        assertEquals(expected, StringUtils.escapeHTML(html));
    }

    @Test
    public void testMd5() {
        assertEquals("0832c1202da8d382318e329a7c133ea0",
                StringUtils.md5("cats"));
    }

    @Test
    public void testMD5() {
        assertEquals("0832c1202da8d382318e329a7c133ea0",
                StringUtils.md5("cats"));
    }

    @Test
    public void testRemoveTrailingZeroes() {
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
    public void testSanitizeWithStrings() {
        assertEquals("", StringUtils.sanitize("dirt", "dirt"));
        assertEquals("y", StringUtils.sanitize("dirty", "dirt"));
        assertEquals("dirty", StringUtils.sanitize("dir1ty", "1"));

        // test injection
        assertEquals("", StringUtils.sanitize("cacacatststs", "cats"));
        assertEquals("", StringUtils.sanitize("cadocadogstsgsts", "cats", "dogs"));
    }

    @Test
    public void testSanitizeWithPatterns() {
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
    public void testStripEndWithMatch() {
        String str = "ababab";
        String toStrip = "ab";
        assertEquals("abab", StringUtils.stripEnd(str, toStrip));
    }

    @Test
    public void testStripEndWithoutMatch() {
        String str = "ababab";
        String toStrip = "c";
        assertSame(str, StringUtils.stripEnd(str, toStrip));

        toStrip = "longer than str";
        assertSame(str, StringUtils.stripEnd(str, toStrip));
    }

    @Test
    public void testStripStartWithMatch() {
        String str = "abcdefg";
        String toStrip = "ab";
        assertEquals("cdefg", StringUtils.stripStart(str, toStrip));
    }

    @Test
    public void testStripStartWithoutMatch() {
        String str = "ababab";
        String toStrip = "c";
        assertSame(str, StringUtils.stripStart(str, toStrip));

        toStrip = "longer than str";
        assertSame(str, StringUtils.stripStart(str, toStrip));
    }

    @Test(expected = NumberFormatException.class)
    public void testToBooleanWithUnrecognizedValue() {
        StringUtils.toBoolean("cats");
    }

    @Test
    public void testToBooleanWithRecognizedValue() {
        assertFalse(StringUtils.toBoolean("0"));
        assertFalse(StringUtils.toBoolean("false"));
        assertTrue(StringUtils.toBoolean("1"));
        assertTrue(StringUtils.toBoolean("true"));
    }

    @Test(expected = NumberFormatException.class)
    public void testToByteSizeWithIllegalArgument() {
        StringUtils.toByteSize("cats");
    }

    @Test
    public void testToByteSizeWithNumber() {
        assertEquals(254254254, StringUtils.toByteSize("254254254"));
        assertEquals(255, StringUtils.toByteSize("254.9"));
    }

    @Test
    public void testToByteSizeWithKB() {
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
    public void testToByteSizeWithMB() {
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
    public void testToByteSizeWithGB() {
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
    public void testToByteSizeWithTB() {
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
    public void testToByteSizeWithPB() {
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
    public void testTrimXMPWithTrimmableXMP() {
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
    public void testTrimXMPWithNonTrimmableXMP() {
        String xmp = "<rdf:RDF foxes=\"bugs\">" +
                "</rdf:RDF>";
        String result = StringUtils.trimXMP(xmp);
        assertSame(xmp, result);
    }

    @Test(expected = NullPointerException.class)
    public void testTrimXMPWithNullArgument() {
        StringUtils.trimXMP(null);
    }

}
