package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import javax.script.ScriptException;
import java.io.FileNotFoundException;

import static org.junit.Assert.*;

public class ScriptEngineFactoryTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ScriptEngineFactory.clearInstance();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
    }

    @Test
    public void testGetScriptEngineWithDelegateScriptDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, false);
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
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getImage("txt").getAbsolutePath());
        try {
            ScriptEngineFactory.getScriptEngine();
            fail("Expected exception");
        } catch (ScriptException e) {
            // pass
        }
    }

    @Test
    public void testGetScriptEngineWithNoScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME, "");
        try {
            ScriptEngineFactory.getScriptEngine();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetScriptEngineWithBogusScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                "/bla/bla/blaasdfasdfasfd");
        try {
            ScriptEngineFactory.getScriptEngine();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

}
