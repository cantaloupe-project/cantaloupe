package edu.illinois.library.cantaloupe.script;

import org.junit.Before;
import org.junit.Test;

import javax.script.ScriptException;
import java.util.Map;

import static org.junit.Assert.*;

public class RubyScriptEngineTest {

    RubyScriptEngine instance;

    @Before
    public void setUp() {
        instance = new RubyScriptEngine();
    }

    @Test
    public void testInvoke() throws Exception {
        final String code = "module Cantaloupe\n" +
                "SOMETHING = 1\n" +
                "def self.func(arg)\n" +
                "arg\n" +
                "end\n" +
                "end";
        instance.load(code);
        String function = "Cantaloupe::func";

        for (int i = 0; i < 3; i++) {
            String[] args = { String.valueOf(i) };
            String result = (String) instance.invoke(function, args);
            assertEquals(String.valueOf(i), result);
        }
    }

    @Test
    public void testInvokeWithUnexpectedReturnType() throws Exception {
        final String code = "module Cantaloupe\n" +
                "def self.func\n" +
                "[]\n" +
                "end\n" +
                "end";
        instance.load(code);
        String function = "Cantaloupe::func";

        try {
            Map result = (Map) instance.invoke(function);
            fail("Shouldn't be able to cast an array to a map");
        } catch (ClassCastException e) {
            // pass
        }
    }

    @Test
    public void testMethodExists() throws Exception {
        final String code = "module Cantaloupe\n" +
                "def self.func(arg)\n" +
                "end\n" +
                "def self.func2\n" +
                "end\n" +
                "end";
        instance.load(code);
        assertTrue(instance.methodExists("func"));
        assertFalse(instance.methodExists("bogus"));
    }

}
