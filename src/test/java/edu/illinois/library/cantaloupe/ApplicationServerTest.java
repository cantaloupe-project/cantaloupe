package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.SocketUtils;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class ApplicationServerTest extends BaseTest {

    private static int HTTP_PORT;
    private static int HTTPS_PORT;

    private ApplicationServer instance;

    @BeforeClass
    public static void beforeClass() {
        int[] ports = SocketUtils.getOpenPorts(2);
        HTTP_PORT = ports[0];
        HTTPS_PORT = ports[1];
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new ApplicationServer();
    }

    @After
    public void tearDown() throws Exception {
        instance.stop();
    }

    @Test
    public void getAcceptQueueLimit() {
        // default
        assertEquals(0, instance.getAcceptQueueLimit());
        // explicitly set
        instance.setAcceptQueueLimit(0);
        assertEquals(0, instance.getAcceptQueueLimit());
    }

    @Test
    public void getHTTPHost() {
        // default
        assertEquals("0.0.0.0", instance.getHTTPHost());
        // explicitly set
        instance.setHTTPHost("127.0.0.1");
        assertEquals("127.0.0.1", instance.getHTTPHost());
    }

    @Test
    public void getHTTPPort() {
        // default
        assertEquals(8182, instance.getHTTPPort());
        // explicitly set
        instance.setHTTPPort(5000);
        assertEquals(5000, instance.getHTTPPort());
    }

    @Test
    public void getHTTPSHost() {
        // default
        assertEquals("0.0.0.0", instance.getHTTPSHost());
        // explicitly set
        instance.setHTTPSHost("127.0.0.1");
        assertEquals("127.0.0.1", instance.getHTTPSHost());
    }

    @Test
    public void getHTTPSKeyPassword() {
        // default
        assertNull(instance.getHTTPSKeyPassword());
        // explicitly set
        instance.setHTTPSKeyPassword("cats");
        assertEquals("cats", instance.getHTTPSKeyPassword());
    }

    @Test
    public void getHTTPSKeyStorePassword() {
        // default
        assertNull(instance.getHTTPSKeyStorePassword());
        // explicitly set
        instance.setHTTPSKeyStorePassword("cats");
        assertEquals("cats", instance.getHTTPSKeyStorePassword());
    }

    @Test
    public void getHTTPSKeyStorePath() {
        // default
        assertNull(instance.getHTTPSKeyStorePath());
        // explicitly set
        instance.setHTTPSKeyStorePath("/cats");
        assertEquals("/cats", instance.getHTTPSKeyStorePath());
    }

    @Test
    public void getHTTPSKeyStoreType() {
        // default
        assertNull(instance.getHTTPSKeyStoreType());
        // explicitly set
        instance.setHTTPSKeyStoreType("cats");
        assertEquals("cats", instance.getHTTPSKeyStoreType());
    }

    @Test
    public void getHTTPSPort() {
        // default
        assertEquals(8183, instance.getHTTPSPort());
        // explicitly set
        instance.setHTTPSPort(5000);
        assertEquals(5000, instance.getHTTPSPort());
    }

    @Test
    public void isHTTPEnabled() {
        // default
        assertFalse(instance.isHTTPEnabled());
        // explicitly set
        instance.setHTTPEnabled(true);
        assertTrue(instance.isHTTPEnabled());
    }

    @Test
    public void isHTTPSEnabled() {
        // default
        assertFalse(instance.isHTTPSEnabled());
        // explicitly set
        instance.setHTTPSEnabled(true);
        assertTrue(instance.isHTTPSEnabled());
    }

    @Test
    public void isInsecureHTTP2Enabled() {
        // default
        assertTrue(instance.isInsecureHTTP2Enabled());
        // explicitly set
        instance.setInsecureHTTP2Enabled(false);
        assertFalse(instance.isInsecureHTTP2Enabled());
    }

    @Test
    public void isSecureHTTP2Enabled() {
        // default
        assertTrue(instance.isSecureHTTP2Enabled());
        // explicitly set
        instance.setSecureHTTP2Enabled(false);
        assertFalse(instance.isSecureHTTP2Enabled());
    }

    @Test
    public void isStarted() throws Exception {
        assertFalse(instance.isStarted());
        assertTrue(instance.isStopped());
        instance.start();
        assertTrue(instance.isStarted());
        assertFalse(instance.isStopped());
    }

    @Test
    public void isStopped() throws Exception {
        isStarted();
    }

    @Test
    public void startStartsHTTPServer() throws Exception {
        initializeHTTP();
        instance.start();

        assertStatus(200, "http://127.0.0.1:" + HTTP_PORT +"/");
    }

    @Test
    public void startStartsHTTPSServer() throws Exception {
        initializeHTTPS();
        instance.setHTTPEnabled(false);
        instance.start();

        assertStatus(200, "https://127.0.0.1:" + HTTPS_PORT +"/");
    }

    @Test
    public void startStartsInsecureHTTP2Server() {
        assumeTrue(SystemUtils.isALPNAvailable());
        // TODO: write this
    }

    @Test
    public void startStartsSecureHTTP2Server() {
        assumeTrue(SystemUtils.isALPNAvailable());
        // TODO: write this
    }

    @Test
    public void stopStopsHTTPServer() throws Exception {
        initializeHTTP();
        try {
            instance.start();
        } finally {
            instance.stop();
        }
        assertConnectionRefused("http://127.0.0.1:" + HTTP_PORT +"/");
    }

    @Test
    public void stopStopsHTTPSServer() throws Exception {
        initializeHTTPS();
        try {
            instance.start();
        } finally {
            instance.stop();
        }
        assertConnectionRefused("https://127.0.0.1:" + HTTPS_PORT +"/");
    }

    private void initializeHTTP() {
        instance.setHTTPEnabled(true);
        instance.setHTTPPort(HTTP_PORT);
    }

    private void initializeHTTPS() throws IOException {
        instance.setHTTPSEnabled(true);
        instance.setHTTPSPort(HTTPS_PORT);
        instance.setHTTPSKeyStorePath(TestUtil.getFixture("keystore.jks").toString());
        instance.setHTTPSKeyStorePassword("password");
        instance.setHTTPSKeyStoreType("JKS");
        instance.setHTTPSKeyPassword("password");
    }

}
