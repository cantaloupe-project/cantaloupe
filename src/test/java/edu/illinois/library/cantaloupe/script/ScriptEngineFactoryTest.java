package edu.illinois.library.cantaloupe.script;

import org.junit.Test;

import static org.junit.Assert.*;

public class ScriptEngineFactoryTest {

    @Test
    public void testGetScriptEngine() {
        // with valid script engine
        assertNotNull(ScriptEngineFactory.getScriptEngine("jruby"));

        // with invalid script engine
        assertNull(ScriptEngineFactory.getScriptEngine("bogus"));
    }

}
