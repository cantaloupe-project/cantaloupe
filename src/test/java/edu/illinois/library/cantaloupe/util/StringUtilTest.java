package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class StringUtilTest extends BaseTest {

    @Test
    public void testEscapeHTML() {
        String html = "the quick brown <script type=\"text/javascript\">alert('hi');</script> fox";
        String expected = "the quick brown &#60;script type=&#34;text/javascript&#34;&#62;alert('hi');&#60;/script&#62; fox";
        assertEquals(expected, StringUtil.escapeHTML(html));
    }

    @Test
    public void testFilenameSafe() {
        assertEquals("0832c1202da8d382318e329a7c133ea0",
                StringUtil.filesystemSafe("cats"));
    }

    @Test
    public void testRemoveTrailingZeroes() {
        // with floats
        assertEquals("0", StringUtil.removeTrailingZeroes(0.0f));
        assertEquals("0.5", StringUtil.removeTrailingZeroes(0.5f));
        assertEquals("50", StringUtil.removeTrailingZeroes(50.0f));
        assertEquals("50.5", StringUtil.removeTrailingZeroes(50.5f));
        assertEquals("50.5", StringUtil.removeTrailingZeroes(50.50f));
        assertTrue(StringUtil.removeTrailingZeroes(50.5555555555555f).length() <= 13);

        // with doubles
        assertEquals("0", StringUtil.removeTrailingZeroes(0.0));
        assertEquals("0.5", StringUtil.removeTrailingZeroes(0.5));
        assertEquals("50", StringUtil.removeTrailingZeroes(50.0));
        assertEquals("50.5", StringUtil.removeTrailingZeroes(50.5));
        assertEquals("50.5", StringUtil.removeTrailingZeroes(50.50));
        assertTrue(StringUtil.removeTrailingZeroes(50.5555555555555).length() <= 13);
    }

    @Test
    public void testSanitizeWithStrings() {
        assertEquals("", StringUtil.sanitize("dirt", "dirt"));
        assertEquals("y", StringUtil.sanitize("dirty", "dirt"));
        assertEquals("dirty", StringUtil.sanitize("dir1ty", "1"));

        // test injection
        assertEquals("", StringUtil.sanitize("cacacatststs", "cats"));
        assertEquals("", StringUtil.sanitize("cadocadogstsgsts", "cats", "dogs"));
    }

    @Test
    public void testSanitizeWithPatterns() {
        assertEquals("", StringUtil.sanitize("dirt", Pattern.compile("dirt")));
        assertEquals("y", StringUtil.sanitize("dirty", Pattern.compile("dirt")));
        assertEquals("dirty", StringUtil.sanitize("dir1ty", Pattern.compile("1")));

        // test injection
        assertEquals("", StringUtil.sanitize("cacacatststs",
                Pattern.compile("cats")));
        assertEquals("", StringUtil.sanitize("cadocadogstsgsts",
                Pattern.compile("cats"), Pattern.compile("dogs")));
    }

    @Test
    public void testStripEndWithMatch() {
        String str = "ababab";
        String toStrip = "ab";
        assertEquals("abab", StringUtil.stripEnd(str, toStrip));
    }

    @Test
    public void testStripEndWithoutMatch() {
        String str = "ababab";
        String toStrip = "c";
        assertSame(str, StringUtil.stripEnd(str, toStrip));

        toStrip = "longer than str";
        assertSame(str, StringUtil.stripEnd(str, toStrip));
    }

}
