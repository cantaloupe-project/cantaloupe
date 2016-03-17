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
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        Application.setConfiguration(config);
    }

    @Test
    public void testGetScriptEngineWithDelegateScriptDisabled() throws Exception {
        Configuration config = Application.getConfiguration();
        config.setProperty(
                ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY, false);
        try {
            ScriptEngineFactory.getScriptEngine();
            fail("Expected exception");
        } catch (DelegateScriptDisabledException e) {
            // pass
        }
    }

    @Test
    public void testGetScriptEngineWithPresentValidScript() throws Exception {
        ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        assertNotNull(engine);
    }

    @Test
    public void testGetScriptEngineWithPresentInvalidScript() throws Exception {
        final Configuration config = Application.getConfiguration();
        config.setProperty(
                ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getImage("txt").getAbsolutePath());
        try {
            ScriptEngineFactory.getScriptEngine();
            fail("Expected exception");
        } catch (ScriptException e) {
            // pass
        }
    }

    @Test
    public void testGetScriptEngineWithMissingScript() throws Exception {
        Configuration config = Application.getConfiguration();
        // bogus script
        config.setProperty(
                ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                "/bla/bla/blaasdfasdfasfd");
        try {
            ScriptEngineFactory.getScriptEngine();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }

        // empty value
        config.setProperty(
                ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY, "");
        try {
            ScriptEngineFactory.getScriptEngine();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

}
