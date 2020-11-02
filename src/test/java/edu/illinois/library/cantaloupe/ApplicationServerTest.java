package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.SocketUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static org.junit.Assert.*;

public class ApplicationServerTest extends BaseTest {

    private static int HTTP_PORT;
    private static int HTTPS_PORT;

    private ApplicationServer instance;

    @BeforeClass
    public static void beforeClass() throws Exception {
        BaseTest.beforeClass();
        int[] ports = SocketUtils.getOpenPorts(2);
        HTTP_PORT   = ports[0];
        HTTPS_PORT  = ports[1];
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
        assertEquals(ApplicationServer.DEFAULT_HTTP_HOST,
                instance.getHTTPHost());
        // explicitly set
        instance.setHTTPHost("127.0.0.1");
        assertEquals("127.0.0.1", instance.getHTTPHost());
    }

    @Test
    public void getHTTPPort() {
        // default
        assertEquals(ApplicationServer.DEFAULT_HTTP_PORT,
                instance.getHTTPPort());
        // explicitly set
        instance.setHTTPPort(5000);
        assertEquals(5000, instance.getHTTPPort());
    }

    @Test
    public void getHTTPSHost() {
        // default
        assertEquals(ApplicationServer.DEFAULT_HTTPS_HOST,
                instance.getHTTPSHost());
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
        assertEquals(ApplicationServer.DEFAULT_HTTPS_PORT,
                instance.getHTTPSPort());
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
        assertFalse(instance.isInsecureHTTP2Enabled());
        // explicitly set
        instance.setInsecureHTTP2Enabled(true);
        assertTrue(instance.isInsecureHTTP2Enabled());
    }

    @Test
    public void isSecureHTTP2Enabled() {
        // default
        assertFalse(instance.isSecureHTTP2Enabled());
        // explicitly set
        instance.setSecureHTTP2Enabled(true);
        assertTrue(instance.isSecureHTTP2Enabled());
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
        assertStatus(200, "http://127.0.0.1:" + HTTP_PORT);
    }

    @Test
    public void startStartsHTTPSServerWithJKSKeyStoreWithPassword()
            throws Exception {
        initializeHTTPSWithJKSKeyStoreWithPassword();
        instance.setHTTPEnabled(false);
        instance.start();
        assertStatus(200, "https://127.0.0.1:" + HTTPS_PORT);
    }

    @Test
    public void startStartsHTTPSServerWithPKCS12KeyStoreWithPassword()
            throws Exception {
        initializeHTTPSWithPKCS12KeyStoreWithPassword();
        instance.setHTTPEnabled(false);
        instance.start();
        assertStatus(200, "https://127.0.0.1:" + HTTPS_PORT);
    }

    @Ignore // TODO: this fails in Jetty 9.4.24
    @Test
    public void startStartsHTTPSServerWithPKCS12KeyStoreWithoutPassword()
            throws Exception {
        initializeHTTPSWithPKCS12KeyStoreWithoutPassword();
        instance.setHTTPEnabled(false);
        instance.start();
        assertStatus(200, "https://127.0.0.1:" + HTTPS_PORT);
    }

    @Test
    public void startStartsInsecureHTTP2Server() {
        // TODO: write this
    }

    @Test
    public void startStartsSecureHTTP2Server() {
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
        assertConnectionRefused("http://127.0.0.1:" + HTTP_PORT);
    }

    @Test
    public void stopStopsHTTPSServerWithJKSKeyStoreWithPassword()
            throws Exception {
        initializeHTTPSWithJKSKeyStoreWithPassword();
        try {
            instance.start();
        } finally {
            instance.stop();
        }
        assertConnectionRefused("https://127.0.0.1:" + HTTPS_PORT);
    }

    @Test
    public void stopStopsHTTPSServerWithPKCS12KeyStoreWithPassword()
            throws Exception {
        initializeHTTPSWithPKCS12KeyStoreWithPassword();
        try {
            instance.start();
        } finally {
            instance.stop();
        }
        assertConnectionRefused("https://127.0.0.1:" + HTTPS_PORT);
    }

    @Test
    public void stopStopsHTTPSServerWithPKCS12KeyStoreWithoutPassword()
            throws Exception {
        initializeHTTPSWithPKCS12KeyStoreWithoutPassword();
        try {
            instance.start();
        } finally {
            instance.stop();
        }
        assertConnectionRefused("https://127.0.0.1:" + HTTPS_PORT);
    }

    private void initializeHTTP() {
        instance.setHTTPEnabled(true);
        instance.setHTTPPort(HTTP_PORT);
    }

    private void initializeHTTPSWithJKSKeyStoreWithPassword() throws IOException {
        instance.setHTTPSEnabled(true);
        instance.setHTTPSPort(HTTPS_PORT);
        instance.setHTTPSKeyStorePath(TestUtil.getFixture("keystore-password.jks").toString());
        instance.setHTTPSKeyStorePassword("password");
        instance.setHTTPSKeyStoreType("JKS");
        instance.setHTTPSKeyPassword("password");
    }

    private void initializeHTTPSWithPKCS12KeyStoreWithPassword() throws IOException {
        instance.setHTTPSEnabled(true);
        instance.setHTTPSPort(HTTPS_PORT);
        instance.setHTTPSKeyStorePath(TestUtil.getFixture("keystore-password.p12").toString());
        instance.setHTTPSKeyStorePassword("password");
        instance.setHTTPSKeyStoreType("PKCS12");
        instance.setHTTPSKeyPassword("password");
    }

    private void initializeHTTPSWithPKCS12KeyStoreWithoutPassword() throws IOException {
        instance.setHTTPSEnabled(true);
        instance.setHTTPSPort(HTTPS_PORT);
        instance.setHTTPSKeyStorePath(TestUtil.getFixture("keystore-nopass.p12").toString());
        instance.setHTTPSKeyStoreType("PKCS12");
    }

}
