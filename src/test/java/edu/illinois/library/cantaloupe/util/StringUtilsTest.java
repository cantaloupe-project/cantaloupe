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
    public void testFilenameSafe() {
        assertEquals("0832c1202da8d382318e329a7c133ea0",
                StringUtils.filesystemSafe("cats"));
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
