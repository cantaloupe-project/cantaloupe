package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

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
        assertFalse(engine.methodExists("get_iiif2_service"));

        File script = TestUtil.getFixture("delegate.rb");
        Application.getConfiguration().setProperty("delegate_script",
                script.getAbsolutePath());

        engine = ScriptEngineFactory.getScriptEngine();
        assertNotNull(engine);
        assertTrue(engine.methodExists("get_iiif2_service"));
    }


}
