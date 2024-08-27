package edu.illinois.library.cantaloupe.delegate;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DelegateProxyServiceTest extends BaseTest {

    private DelegateProxyService instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty(DelegateProxyService.DELEGATE_SCRIPT_VM_ARGUMENT,
                TestUtil.getFixture("delegates.rb").toString());

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.clearProperty(Key.DELEGATE_SCRIPT_PATHNAME);

        DelegateProxyService.clearInstance();

        instance = DelegateProxyService.getInstance();
    }

    /* getJavaDelegate() */

    @Test
    void getJavaDelegate() {
        // TODO: This is hard to test as we don't have a JavaDelegate on the classpath.
    }

    /* isDelegateAvailable() */

    @Test
    void isDelegateAvailableWithJavaDelegateAvailable() {
        // This is hard to test as we don't have a JavaDelegate on the
        // classpath.
    }

    @Test
    void isDelegateAvailableWithNoJavaDelegateAndScriptEnabled() {
        assertTrue(DelegateProxyService.isDelegateAvailable());
    }

    @Test
    void isDelegateAvailableWithNoJavaDelegateAndScriptDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, false);
        assertFalse(DelegateProxyService.isDelegateAvailable());
    }

    /* isScriptEnabled() */

    @Test
    void isScriptEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, false);
        assertFalse(DelegateProxyService.isScriptEnabled());

        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        assertTrue(DelegateProxyService.isScriptEnabled());
    }

    /* getScriptFile() */

    @Test
    void getScriptFileWithValidScriptInVMArgumentAndConfiguration()
            throws Exception {
        Configuration.getInstance().setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        Path actual = DelegateProxyService.getScriptFile();
        assertTrue(Files.readString(actual).contains("CustomDelegate"));
    }

    @Test
    void getScriptFileWithValidScriptInVMArgumentAndInvalidScriptInConfiguration()
            throws Exception {
        Configuration.getInstance().setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getImage("txt"));

        Path actual = DelegateProxyService.getScriptFile();
        assertTrue(Files.readString(actual).contains("CustomDelegate"));
    }

    @Test
    void getScriptFileWithValidScriptInVMArgumentAndMissingScriptInConfiguration()
            throws Exception {
        Configuration.getInstance().setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                "/bogus/bogus/bogus");

        Path actual = DelegateProxyService.getScriptFile();
        assertTrue(Files.readString(actual).contains("CustomDelegate"));
    }

    @Test
    void getScriptFileWithInvalidScriptInVMArgumentAndValidScriptInConfiguration()
            throws Exception {
        System.setProperty(DelegateProxyService.DELEGATE_SCRIPT_VM_ARGUMENT,
                TestUtil.getImage("txt").toString());
        Configuration.getInstance().setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        Path actual = DelegateProxyService.getScriptFile();
        assertEquals("some text", Files.readString(actual));
    }

    @Test
    void getScriptFileWithInvalidScriptInVMArgumentAndConfiguration()
            throws Exception {
        System.setProperty(DelegateProxyService.DELEGATE_SCRIPT_VM_ARGUMENT,
                TestUtil.getImage("txt").toString());
        Configuration.getInstance().setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getImage("txt").toString());

        Path actual = DelegateProxyService.getScriptFile();
        assertEquals("some text", Files.readString(actual));
    }

    @Test
    void getScriptFileWithInvalidScriptInVMArgumentAndMissingScriptInConfiguration()
            throws Exception {
        final Path invalidScript = TestUtil.getImage("txt");
        System.setProperty(DelegateProxyService.DELEGATE_SCRIPT_VM_ARGUMENT,
                invalidScript.toString());
        Configuration.getInstance().setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                "/bogus/bogus/bogus");

        Path actual = DelegateProxyService.getScriptFile();
        assertEquals("some text", Files.readString(actual));
    }

    @Test
    void getScriptFileWithMissingScriptInVMArgumentAndValidScriptConfiguration() {
        System.setProperty(DelegateProxyService.DELEGATE_SCRIPT_VM_ARGUMENT,
                "/bogus/bogus/bogus");
        Configuration.getInstance().setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        assertThrows(NoSuchFileException.class,
                DelegateProxyService::getScriptFile);
    }

    @Test
    void getScriptFileWithMissingScriptInVMArgumentAndInvalidScriptInConfiguration() {
        System.setProperty(DelegateProxyService.DELEGATE_SCRIPT_VM_ARGUMENT,
                "/bogus/bogus/bogus");
        Configuration.getInstance().setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("txt").toString());

        assertThrows(NoSuchFileException.class,
                DelegateProxyService::getScriptFile);
    }

    @Test
    void getScriptFileWithMissingScriptInVMArgumentAndConfiguration() {
        System.setProperty(DelegateProxyService.DELEGATE_SCRIPT_VM_ARGUMENT,
                "/bogus/bogus/bogus");
        Configuration.getInstance().setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                "/bogus/bogus/bogus");

        assertThrows(NoSuchFileException.class,
                DelegateProxyService::getScriptFile);
    }

    /* newDelegateProxy() */

    @Test
    void newDelegateProxyWithJavaDelegateAvailable() {
        // This is hard to test as we don't have a JavaDelegate on the
        // classpath.
    }

    @Test
    void newDelegateProxyWithDelegateScriptEnabled() throws Exception {
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