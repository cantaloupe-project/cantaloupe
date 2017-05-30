package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.restlet.Client;
import org.restlet.Request;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Status;

import java.io.IOException;

import static org.junit.Assert.*;

public class WebServerTest extends BaseTest {

    private static final int HTTP_PORT = TestUtil.getOpenPort();
    private static final int HTTPS_PORT = TestUtil.getOpenPort();

    private WebServer instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new WebServer();
    }

    @After
    public void tearDown() throws Exception {
        instance.stop();
    }

    // getAcceptQueueLimit()

    @Test
    public void testGetAcceptQueueLimit() {
        // default
        assertEquals(0, instance.getAcceptQueueLimit());
        // explicitly set
        instance.setAcceptQueueLimit(0);
        assertEquals(0, instance.getAcceptQueueLimit());
    }

    // getHTTPHost()

    @Test
    public void testGetHTTPHost() {
        // default
        assertEquals("0.0.0.0", instance.getHTTPHost());
        // explicitly set
        instance.setHTTPHost("127.0.0.1");
        assertEquals("127.0.0.1", instance.getHTTPHost());
    }

    // getHTTPPort()

    @Test
    public void testGetHTTPPort() {
        // default
        assertEquals(8182, instance.getHTTPPort());
        // explicitly set
        instance.setHTTPPort(5000);
        assertEquals(5000, instance.getHTTPPort());
    }

    // getHTTPSHost()

    @Test
    public void testGetHTTPSHost() {
        // default
        assertEquals("0.0.0.0", instance.getHTTPSHost());
        // explicitly set
        instance.setHTTPSHost("127.0.0.1");
        assertEquals("127.0.0.1", instance.getHTTPSHost());
    }

    // getHTTPSKeyPassword()

    @Test
    public void testGetHTTPSKeyPassword() {
        // default
        assertNull(instance.getHTTPSKeyPassword());
        // explicitly set
        instance.setHTTPSKeyPassword("cats");
        assertEquals("cats", instance.getHTTPSKeyPassword());
    }

    // getHTTPSKeyStorePassword()

    @Test
    public void testGetHTTPSKeyStorePassword() {
        // default
        assertNull(instance.getHTTPSKeyStorePassword());
        // explicitly set
        instance.setHTTPSKeyStorePassword("cats");
        assertEquals("cats", instance.getHTTPSKeyStorePassword());
    }

    // getHTTPSKeyStorePath()

    @Test
    public void testGetHTTPSKeyStorePath() {
        // default
        assertNull(instance.getHTTPSKeyStorePath());
        // explicitly set
        instance.setHTTPSKeyStorePath("/cats");
        assertEquals("/cats", instance.getHTTPSKeyStorePath());
    }

    // getHTTPSKeyStoreType()

    @Test
    public void testGetHTTPSKeyStoreType() {
        // default
        assertNull(instance.getHTTPSKeyStoreType());
        // explicitly set
        instance.setHTTPSKeyStoreType("cats");
        assertEquals("cats", instance.getHTTPSKeyStoreType());
    }

    // getHTTPSPort()

    @Test
    public void testGetHTTPSPort() {
        // default
        assertEquals(8183, instance.getHTTPSPort());
        // explicitly set
        instance.setHTTPSPort(5000);
        assertEquals(5000, instance.getHTTPSPort());
    }

    // isHTTPEnabled()

    @Test
    public void testIsHTTPEnabled() {
        // default
        assertFalse(instance.isHTTPEnabled());
        // explicitly set
        instance.setHTTPEnabled(true);
        assertTrue(instance.isHTTPEnabled());
    }

    // isHTTPSEnabled()

    @Test
    public void testIsHTTPSEnabled() {
        // default
        assertFalse(instance.isHTTPSEnabled());
        // explicitly set
        instance.setHTTPSEnabled(true);
        assertTrue(instance.isHTTPSEnabled());
    }

    // isInsecureHTTP2Enabled()

    @Test
    public void testIsInsecureHTTP2Enabled() {
        // default
        assertTrue(instance.isInsecureHTTP2Enabled());
        // explicitly set
        instance.setInsecureHTTP2Enabled(false);
        assertFalse(instance.isInsecureHTTP2Enabled());
    }

    // isSecureHTTP2Enabled()

    @Test
    public void testIsSecureHTTP2Enabled() {
        // default
        assertTrue(instance.isSecureHTTP2Enabled());
        // explicitly set
        instance.setSecureHTTP2Enabled(false);
        assertFalse(instance.isSecureHTTP2Enabled());
    }

    // isStarted()

    @Test
    public void testIsStarted() throws Exception {
        assertFalse(instance.isStarted());
        assertTrue(instance.isStopped());
        instance.start();
        assertTrue(instance.isStarted());
        assertFalse(instance.isStopped());
    }

    // isStopped()

    @Test
    public void testIsStopped() throws Exception {
        testIsStarted();
    }

    // start()

    @Test
    public void testStartStartsHTTPServer() throws Exception {
        initializeHTTP();
        instance.start();

        Client client = new Client(Protocol.HTTP);
        Request req = new Request();
        req.setResourceRef("http://127.0.0.1:" + HTTP_PORT +"/");
        req.setMethod(Method.GET);
        assertEquals(Status.SUCCESS_OK, client.handle(req).getStatus());
    }

    @Test
    @Ignore // TODO: the Restlet client may be missing dependencies
    public void testStartStartsHTTPSServer() throws Exception {
        initializeHTTPS();
        instance.setHTTPEnabled(false);
        instance.start();

        Client client = new Client(Protocol.HTTPS);
        Request req = new Request();
        req.setResourceRef("https://127.0.0.1:" + HTTPS_PORT +"/");
        req.setMethod(Method.GET);
        assertEquals(Status.SUCCESS_OK, client.handle(req).getStatus());
    }

    @Test
    @Ignore // TODO: ignored until Java 9 is required (for ALPN)
    public void testStartStartsInsecureHTTP2Server() {}

    @Test
    @Ignore // TODO: ignored until Java 9 is required (for ALPN)
    public void testStartStartsSecureHTTP2Server() {}

    // stop()

    @Test
    public void testStopStopsHTTPServer() throws Exception {
        initializeHTTP();
        try {
            instance.start();
        } finally {
            instance.stop();
        }

        Client client = new Client(Protocol.HTTP);
        Request req = new Request();
        req.setResourceRef("http://127.0.0.1:" + HTTP_PORT +"/");
        req.setMethod(Method.GET);

        assertTrue(client.handle(req).getStatus().getCode() >= 1000);
    }

    @Test
    public void testStopStopsHTTPSServer() throws Exception {
        initializeHTTPS();
        try {
            instance.start();
        } finally {
            instance.stop();
        }

        Client client = new Client(Protocol.HTTPS);
        Request req = new Request();
        req.setResourceRef("https://127.0.0.1:" + HTTPS_PORT +"/");
        req.setMethod(Method.GET);

        assertTrue(client.handle(req).getStatus().getCode() >= 1000);
    }

    private void initializeHTTP() {
        instance.setHTTPEnabled(true);
        instance.setHTTPPort(HTTP_PORT);
    }

    private void initializeHTTPS() throws IOException {
        instance.setHTTPSEnabled(true);
        instance.setHTTPSPort(HTTPS_PORT);
        instance.setHTTPSKeyStorePath(TestUtil.getFixture("keystore.jks").getAbsolutePath());
        instance.setHTTPSKeyStorePassword("password");
        instance.setHTTPSKeyStoreType("JKS");
        instance.setHTTPSKeyPassword("password");
    }

}
