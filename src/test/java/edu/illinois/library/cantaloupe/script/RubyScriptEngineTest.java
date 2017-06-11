package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class RubyScriptEngineTest extends BaseTest {

    private RubyScriptEngine instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new RubyScriptEngine();
    }

    @Test
    public void testGetModuleName() {
        assertEquals(RubyScriptEngine.TOP_MODULE,
                instance.getModuleName("cats"));
        assertEquals(RubyScriptEngine.TOP_MODULE + "::cats",
                instance.getModuleName("cats::dogs"));
        assertEquals(RubyScriptEngine.TOP_MODULE + "::cats::dogs",
                instance.getModuleName("cats::dogs::fleas"));
    }

    @Test
    public void testGetUnqualifiedMethodName() {
        assertEquals("cats", instance.getUnqualifiedMethodName("cats"));
        assertEquals("dogs", instance.getUnqualifiedMethodName("cats::dogs"));
    }

    @Test
    public void testInvokeWithNoArgs() throws Exception {
        final String code = "module Cantaloupe\n" +
                "SOMETHING = 1\n" +
                "def self.func1\n" +
                "'cats'\n" +
                "end\n" +
                "end";
        instance.load(code);

        String result = (String) instance.invoke("func1");
        assertEquals("cats", result);
    }

    @Test
    public void testInvokeWithArgs() throws Exception {
        final String code = "module Cantaloupe\n" +
                "SOMETHING = 1\n" +
                "def self.func2(arg)\n" +
                "arg\n" +
                "end\n" +
                "end";
        instance.load(code);
        final String function = "func2";

        for (int i = 0; i < 3; i++) {
            String result = (String) instance.invoke(function, String.valueOf(i));
            assertEquals(String.valueOf(i), result);
        }
    }

    @Test
    public void testInvokeWithUnexpectedReturnType() throws Exception {
        final String code = "module Cantaloupe\n" +
                "def self.func3\n" +
                "[]\n" +
                "end\n" +
                "end";
        instance.load(code);
        String function = "func3";

        try {
            Map result = (Map) instance.invoke(function);
            fail("Shouldn't be able to cast an array to a map");
        } catch (ClassCastException e) {
            // pass
        }
    }

    @Test
    public void testInvokeWithCacheEnabled() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.DELEGATE_METHOD_INVOCATION_CACHE_ENABLED, true);

        final String code = "module Cantaloupe\n" +
                "SOMETHING = 1\n" +
                "def self.func2(arg)\n" +
                "arg\n" +
                "end\n" +
                "end";
        instance.load(code);
        final String function = "func2";

        for (int i = 0; i < 3; i++) {
            String result = (String) instance.invoke(function, String.valueOf(i));
            assertEquals(String.valueOf(i), result);
        }

        assertEquals(3, instance.getInvocationCache().size());
    }

}
