package edu.illinois.library.cantaloupe.script;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RubyScriptEngineTest {

    RubyScriptEngine instance;

    @Before
    public void setUp() {
        instance = new RubyScriptEngine();
    }

    @Test
    public void testLoadAndInvoke() throws Exception {
        final String code = "SOMETHING = 1\ndef func(arg)\narg\nend";
        instance.load(code);
        String function = "func";

        for (int i = 0; i < 3; i++) {
            String[] args = { String.valueOf(i) };
            String result = (String) instance.invoke(function, args);
            assertEquals(String.valueOf(i), result);
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
