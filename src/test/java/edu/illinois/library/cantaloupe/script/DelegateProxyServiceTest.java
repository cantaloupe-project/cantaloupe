package edu.illinois.library.cantaloupe.script;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class DelegateProxyServiceTest extends BaseTest {

    private DelegateProxyService instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        DelegateProxyService.clearInstance();

        instance = DelegateProxyService.getInstance();
    }

    /* getScriptFile() */

    @Test
    public void testGetScriptFileWithPresentValidScript() throws Exception {
        Path file = DelegateProxyService.getScriptFile();
        assertNotNull(file);
    }

    @Test
    public void testGetScriptFileWithPresentInvalidScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getImage("txt").toString());

        Path file = DelegateProxyService.getScriptFile();
        assertNotNull(file);
    }

    @Test
    public void testGetScriptFileWithNoScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME, "");

        assertNull(DelegateProxyService.getScriptFile());
    }

    @Test(expected = NoSuchFileException.class)
    public void testGetScriptFileWithBogusScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                "/bla/bla/blaasdfasdfasfd");

        DelegateProxyService.getScriptFile();
    }

    /* isEnabled() */

    @Test
    public void testIsEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, false);
        assertFalse(DelegateProxyService.isEnabled());

        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        assertTrue(DelegateProxyService.isEnabled());
    }

    /* newDelegateProxy() */

    @Test
    public void testNewDelegateProxy() throws Exception {
        RequestContext context = new RequestContext();
        assertNotNull(instance.newDelegateProxy(context));
    }

    @Test(expected = DisabledException.class)
    public void newDelegateProxyWithDelegateScriptDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, false);

        RequestContext context = new RequestContext();
        instance.newDelegateProxy(context);
    }

}