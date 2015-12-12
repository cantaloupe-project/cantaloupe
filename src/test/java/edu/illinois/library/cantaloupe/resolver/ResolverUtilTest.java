package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import org.junit.Test;

import javax.script.ScriptException;

public class ResolverUtilTest {

    public void testExecuteRubyFunction() {
        try {
            String code = "def func(arg)\narg\nend";
            String function = "func";
            String[] args = { "bla" };
            String result = ResolverUtil.executeRubyFunction(code, function,
                    args);
            assertEquals("bla", result);
        } catch (ScriptException e) {
            fail();
        }
    }

    @Test
    public void testExecuteRubyScript() {
        // valid script
        try {
            String code = "def func(arg)\narg\nend\nfunc('bla')";
            String result = ResolverUtil.executeRubyScript(code);
            assertEquals("bla", result);
        } catch (ScriptException e) {
            fail();
        }
        // syntax error
        try {
            String code = "bogus do(arg)\narg\nend\ncats('bla')";
            ResolverUtil.executeRubyScript(code);
            fail("Expected exception");
        } catch (ScriptException e) {
            // pass
        }
    }

    @Test
    public void testInferSourceFormat() {
        assertEquals(SourceFormat.BMP,
                ResolverUtil.inferSourceFormat(new Identifier("bla.bmp")));
        assertEquals(SourceFormat.GIF,
                ResolverUtil.inferSourceFormat(new Identifier("bla.gif")));
        assertEquals(SourceFormat.JP2,
                ResolverUtil.inferSourceFormat(new Identifier("bla.JP2")));
        assertEquals(SourceFormat.PDF,
                ResolverUtil.inferSourceFormat(new Identifier("bla.pdf")));
        assertEquals(SourceFormat.PNG,
                ResolverUtil.inferSourceFormat(new Identifier("bla.png")));
        assertEquals(SourceFormat.TIF,
                ResolverUtil.inferSourceFormat(new Identifier("bla.tif")));
        assertEquals(SourceFormat.UNKNOWN,
                ResolverUtil.inferSourceFormat(new Identifier("bla.bogus")));
        assertEquals(SourceFormat.UNKNOWN,
                ResolverUtil.inferSourceFormat(new Identifier("bla")));
    }

    @Test
    public void testReplacePathSeparators() {
        Identifier identifier = new Identifier("somethingCATSsomething");
        Identifier actual = ResolverUtil.replacePathSeparators(identifier, "CATS", "DOGS");
        assertEquals("somethingDOGSsomething", actual.toString());
        actual = ResolverUtil.replacePathSeparators(identifier, "BOGUS", "DOGS");
        assertEquals("somethingCATSsomething", actual.toString());
    }

}
