package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ScriptEngineFactoryTest {

    @Before
    public void setUp() {
        Application.setConfiguration(new BaseConfiguration());
    }

    @Test
    public void testGetScriptEngine() throws Exception {
        ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        assertNotNull(engine);
    }


}
