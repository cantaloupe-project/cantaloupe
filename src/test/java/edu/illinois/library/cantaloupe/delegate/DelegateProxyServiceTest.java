package edu.illinois.library.cantaloupe.delegate;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class DelegateProxyServiceTest extends BaseTest {

    private DelegateProxyService instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        DelegateProxyService.clearInstance();

        instance = DelegateProxyService.getInstance();
    }

    /* getJavaDelegate() */

    @Test
    void testGetJavaDelegate() {
        // This is hard to test as we don't have a JavaDelegate on the
        // classpath.
    }

    /* isDelegateAvailable() */

    @Test
    void testIsDelegateAvailableWithJavaDelegateAvailable() {
        // This is hard to test as we don't have a JavaDelegate on the
        // classpath.
    }

    @Test
    void testIsDelegateAvailableWithNoJavaDelegateAndScriptEnabled() {
        assertTrue(DelegateProxyService.isDelegateAvailable());
    }

    @Test
    void testIsDelegateAvailableWithNoJavaDelegateAndScriptDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, false);
        assertFalse(DelegateProxyService.isDelegateAvailable());
    }

    /* isScriptEnabled() */

    @Test
    void testIsScriptEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, false);
        assertFalse(DelegateProxyService.isScriptEnabled());

        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        assertTrue(DelegateProxyService.isScriptEnabled());
    }

    /* getScriptFile() */

    @Test
    void testGetScriptFileWithPresentValidScript() throws Exception {
        Path file = DelegateProxyService.getScriptFile();
        assertNotNull(file);
    }

    @Test
    void testGetScriptFileWithPresentInvalidScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getImage("txt").toString());

        Path file = DelegateProxyService.getScriptFile();
        assertNotNull(file);
    }

    @Test
    void testGetScriptFileWithNoScript() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME, "");

        assertNull(DelegateProxyService.getScriptFile());
    }

    @Test
    void testGetScriptFileWithBogusScript() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                "/bla/bla/blaasdfasdfasfd");

        assertThrows(NoSuchFileException.class,
                DelegateProxyService::getScriptFile);
    }

    /* newDelegateProxy() */

    @Test
    void testNewDelegateProxyWithJavaDelegateAvailable() {
        // This is hard to test as we don't have a JavaDelegate on the
        // classpath.
    }

    @Test
    void testNewDelegateProxyWithDelegateScriptEnabled() throws Exception {
        RequestContext context = new RequestContext();
        DelegateProxy actual = instance.newDelegateProxy(context);
        assertNotNull(actual);
        assertNotNull(actual.getRequestContext());
    }

    @Test
    void newDelegateProxyWithDelegateScriptDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, false);

        RequestContext context = new RequestContext();
        assertThrows(UnavailableException.class,
                () -> instance.newDelegateProxy(context));
    }

}