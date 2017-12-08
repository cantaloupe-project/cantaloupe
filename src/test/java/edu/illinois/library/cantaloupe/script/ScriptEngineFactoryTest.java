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
                TestUtil.getFixture("delegates.rb").toString());
    }

    @Test(expected = DelegateScriptDisabledException.class)
    public void testGetScriptEngineWithDelegateScriptDisabled()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, false);

        ScriptEngineFactory.getScriptEngine();
    }

    @Test
    public void testGetScriptEngineWithPresentValidScript() throws Exception {
        ScriptEngine engine = ScriptEngineFactory.getScriptEngine();
        assertNotNull(engine);
    }

    @Test(expected = ScriptException.class)
    public void testGetScriptEngineWithPresentInvalidScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getImage("txt").toString());

        ScriptEngineFactory.getScriptEngine();
    }

    @Test(expected = FileNotFoundException.class)
    public void testGetScriptEngineWithNoScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME, "");

        ScriptEngineFactory.getScriptEngine();
    }

    @Test(expected = FileNotFoundException.class)
    public void testGetScriptEngineWithBogusScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                "/bla/bla/blaasdfasdfasfd");

        ScriptEngineFactory.getScriptEngine();
    }

}
