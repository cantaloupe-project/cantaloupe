package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.script.ScriptException;
import java.io.FileNotFoundException;

import static org.junit.Assert.*;

public class ScriptEngineFactoryTest {

    @Before
    public void setUp() throws Exception {
        Configuration config = new BaseConfiguration();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        Application.setConfiguration(config);
    }

    @Test
    public void testGetScriptEngine() throws Exception {
        // test with delegate script config key set to a present, valid script
        ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        assertNotNull(engine);

        // test with delegate script config key set to a missing script
        Configuration config = Application.getConfiguration();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_CONFIG_KEY,
                "/bla/bla/blaasdfasdfasfd");
        try {
            ScriptEngineFactory.getScriptEngine();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }

        // test with present, invalid delegate script
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_CONFIG_KEY,
                TestUtil.getImage("txt").getAbsolutePath());
        try {
            ScriptEngineFactory.getScriptEngine();
            fail("Expected exception");
        } catch (ScriptException e) {
            // pass
        }
    }


}
