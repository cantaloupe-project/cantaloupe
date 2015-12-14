package edu.illinois.library.cantaloupe.script;

import org.junit.Test;

import static org.junit.Assert.*;

public class RubyScriptEngineTest {

    RubyScriptEngine instance = new RubyScriptEngine();

    @Test
    public void testLoadAndInvoke() throws Exception {
        final String code = "SOMETHING = 1\ndef func(arg)\narg\nend";
        instance.load(code);
        String function = "func";

        for (int i = 0; i < 3; i++) {
            String[] args = { String.valueOf(i) };
            String result = instance.invoke(function, args);
            assertEquals(String.valueOf(i), result);
        }
    }

}
