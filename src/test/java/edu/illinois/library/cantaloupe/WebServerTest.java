package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WebServerTest extends BaseTest {

    private WebServer instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new WebServer();
    }

    @Test
    public void testGetHttpHost() {
        // default
        assertEquals("0.0.0.0", instance.getHTTPHost());
        // explicitly set
        instance.setHTTPHost("127.0.0.1");
        assertEquals("127.0.0.1", instance.getHTTPHost());
    }

    @Test
    public void testGetHttpPort() {
        // default
        assertEquals(8182, instance.getHTTPPort());
        // explicitly set
        instance.setHTTPPort(5000);
        assertEquals(5000, instance.getHTTPPort());
    }

    @Test
    public void testGetHttpsHost() {
        // default
        assertEquals("0.0.0.0", instance.getHTTPSHost());
        // explicitly set
        instance.setHTTPSHost("127.0.0.1");
        assertEquals("127.0.0.1", instance.getHTTPSHost());
    }

    @Test
    public void testGetHttpsKeyPassword() {
        // default
        assertNull(instance.getHTTPSKeyPassword());
        // explicitly set
        instance.setHTTPSKeyPassword("cats");
        assertEquals("cats", instance.getHTTPSKeyPassword());
    }

    @Test
    public void testGetHttpsKeyStorePassword() {
        // default
        assertNull(instance.getHTTPSKeyStorePassword());
        // explicitly set
        instance.setHTTPSKeyStorePassword("cats");
        assertEquals("cats", instance.getHTTPSKeyStorePassword());
    }

    @Test
    public void testGetHttpsKeyStorePath() {
        // default
        assertNull(instance.getHTTPSKeyStorePath());
        // explicitly set
        instance.setHTTPSKeyStorePath("/cats");
        assertEquals("/cats", instance.getHTTPSKeyStorePath());
    }

    @Test
    public void testGetHttpsKeyStoreType() {
        // default
        assertNull(instance.getHTTPSKeyStoreType());
        // explicitly set
        instance.setHTTPSKeyStoreType("cats");
        assertEquals("cats", instance.getHTTPSKeyStoreType());
    }

    @Test
    public void testGetHttpsPort() {
        // default
        assertEquals(8183, instance.getHTTPSPort());
        // explicitly set
        instance.setHTTPSPort(5000);
        assertEquals(5000, instance.getHTTPSPort());
    }

    @Test
    public void testIsHttpEnabled() {
        // default
        assertFalse(instance.isHTTPEnabled());
        // explicitly set
        instance.setHTTPEnabled(true);
        assertTrue(instance.isHTTPEnabled());
    }

    @Test
    public void testIsHttpsEnabled() {
        // default
        assertFalse(instance.isHTTPSEnabled());
        // explicitly set
        instance.setHTTPSEnabled(true);
        assertTrue(instance.isHTTPSEnabled());
    }

    @Test
    public void testIsStarted() throws Exception {
        assertFalse(instance.isStarted());
        assertTrue(instance.isStopped());
        instance.start();
        assertTrue(instance.isStarted());
        assertFalse(instance.isStopped());
    }

    @Test
    public void testIsStopped() throws Exception {
        testIsStarted();
    }

}
