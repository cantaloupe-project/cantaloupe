package edu.illinois.library.cantaloupe;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WebServerTest {

    private WebServer instance;

    @Before
    public void setUp() {
        Configuration config = new BaseConfiguration();
        Application.setConfiguration(config);

        instance = new WebServer();
    }

    @Test
    public void testGetHttpPort() {
        // default
        assertEquals(8182, instance.getHttpPort());
        // explicitly set
        instance.setHttpPort(5000);
        assertEquals(5000, instance.getHttpPort());
    }

    @Test
    public void testGetHttpsKeyPassword() {
        // default
        assertNull(instance.getHttpsKeyPassword());
        // explicitly set
        instance.setHttpsKeyPassword("cats");
        assertEquals("cats", instance.getHttpsKeyPassword());
    }

    @Test
    public void testGetHttpsKeyStorePassword() {
        // default
        assertNull(instance.getHttpsKeyStorePassword());
        // explicitly set
        instance.setHttpsKeyStorePassword("cats");
        assertEquals("cats", instance.getHttpsKeyStorePassword());
    }

    @Test
    public void testGetHttpsKeyStorePath() {
        // default
        assertNull(instance.getHttpsKeyStorePath());
        // explicitly set
        instance.setHttpsKeyStorePath("/cats");
        assertEquals("/cats", instance.getHttpsKeyStorePath());
    }

    @Test
    public void testGetHttpsKeyStoreType() {
        // default
        assertNull(instance.getHttpsKeyStoreType());
        // explicitly set
        instance.setHttpsKeyStoreType("cats");
        assertEquals("cats", instance.getHttpsKeyStoreType());
    }

    @Test
    public void testGetHttpsPort() {
        // default
        assertEquals(8183, instance.getHttpsPort());
        // explicitly set
        instance.setHttpsPort(5000);
        assertEquals(5000, instance.getHttpsPort());
    }

    @Test
    public void testIsHttpEnabled() {
        // default
        assertFalse(instance.isHttpEnabled());
        // explicitly set
        instance.setHttpEnabled(true);
        assertTrue(instance.isHttpEnabled());
    }

    @Test
    public void testIsHttpsEnabled() {
        // default
        assertFalse(instance.isHttpsEnabled());
        // explicitly set
        instance.setHttpsEnabled(true);
        assertTrue(instance.isHttpsEnabled());
    }

}
