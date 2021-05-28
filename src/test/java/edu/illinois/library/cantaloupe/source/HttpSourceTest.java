package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

abstract class HttpSourceTest extends AbstractSourceTest {

    private static class RequestCountingHandler extends DefaultHandler {

        private int numHEADRequests, numGETRequests;

        @Override
        public void handle(String target,
                Request baseRequest,
                HttpServletRequest request,
                HttpServletResponse response) {
            switch (request.getMethod().toUpperCase()) {
                case "HEAD":
                    numHEADRequests++;
                    break;
                case "GET":
                    numGETRequests++;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unexpected method: " + request.getMethod());
            }
            baseRequest.setHandled(true);
        }

    }

    private static final Identifier PRESENT_READABLE_IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    WebServer server;

    private HttpSource instance;

    /**
     * Subclasses need to override, call super, and set
     * {@link Key#HTTPSOURCE_URL_PREFIX} to the web server URI using the
     * appropriate scheme.
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        destroyEndpoint();
    }

    abstract String getScheme();

    abstract URI getServerURI();

    @Override
    void destroyEndpoint() throws Exception {
        server.stop();
    }

    @Override
    void initializeEndpoint() throws Exception {
        server.start();
    }

    @Override
    HttpSource newInstance() {
        HttpSource instance = new HttpSource();
        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        return instance;
    }

    @Override
    void useBasicLookupStrategy() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
    }

    @Override
    void useScriptLookupStrategy() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
    }

    /* checkAccess() */

    @Test
    void testCheckAccessUsingBasicLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        doTestCheckAccessWithPresentUnreadableImage(new Identifier("gif"));
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/" +
                PRESENT_READABLE_IDENTIFIER);
        doTestCheckAccessWithPresentReadableImage(identifier);
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithMissingImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/bogus");
        doTestCheckAccessWithMissingImage(identifier);
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/gif");
        doTestCheckAccessWithPresentUnreadableImage(identifier);
    }

    private void doTestCheckAccessWithPresentReadableImage(Identifier identifier)
            throws Exception {
        server.start();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        instance.checkAccess();
    }

    private void doTestCheckAccessWithPresentUnreadableImage(Identifier identifier)
            throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                response.setStatus(403);
                baseRequest.setHandled(true);
            }
        });
        server.start();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);
        instance.setIdentifier(identifier);
        assertThrows(AccessDeniedException.class, instance::checkAccess);
    }

    private void doTestCheckAccessWithMissingImage(Identifier identifier)
            throws Exception {
        try {
            server.start();

            DelegateProxy proxy = TestUtil.newDelegateProxy();
            proxy.getRequestContext().setIdentifier(identifier);
            instance.setDelegateProxy(proxy);
            instance.setIdentifier(identifier);

            instance.checkAccess();
            fail("Expected exception");
        } catch (NoSuchFileException e) {
            // pass
        }
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithValidAuthentication()
            throws Exception {
        useScriptLookupStrategy();

        server.setBasicAuthEnabled(true);
        server.start();

        Identifier identifier = new Identifier("valid-auth-" +
                getServerURI() + "/" + PRESENT_READABLE_IDENTIFIER);
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        instance.checkAccess();
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithInvalidAuthentication()
            throws Exception {
        useScriptLookupStrategy();

        server.setBasicAuthEnabled(true);
        server.start();

        Identifier identifier = new Identifier("invalid-auth-" +
                getServerURI() + "/" + PRESENT_READABLE_IDENTIFIER);
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertThrows(AccessDeniedException.class, instance::checkAccess);
    }

    @Test
    void testCheckAccessWith403Response() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                response.setStatus(403);
                baseRequest.setHandled(true);
            }
        });
        server.start();

        try {
            instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
            instance.checkAccess();
            fail("Expected exception");
        } catch (AccessDeniedException e) {
            assertTrue(e.getMessage().contains("403"));
        }
    }

    @Test
    void testCheckAccessWith500Response() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                response.setStatus(500);
                baseRequest.setHandled(true);
            }
        });
        server.start();

        try {
            instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
            instance.checkAccess();
            fail("Expected exception");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("500"));
        }
    }

    @Test
    void testCheckAccessSendsUserAgentHeader() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                String expected = String.format("%s/%s (%s/%s; java/%s; %s/%s)",
                        HttpSource.class.getSimpleName(),
                        Application.getVersion(),
                        Application.getName(),
                        Application.getVersion(),
                        System.getProperty("java.version"),
                        System.getProperty("os.name"),
                        System.getProperty("os.version"));
                assertEquals(expected, baseRequest.getHeader("User-Agent"));
                baseRequest.setHandled(true);
            }
        });
        server.start();

        instance.checkAccess();
    }

    @Test
    void testCheckAccessSendsCustomHeaders() throws Exception {
        useScriptLookupStrategy();

        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                assertEquals("yes", request.getHeader("X-Custom"));
                baseRequest.setHandled(true);
            }
        });
        server.start();

        Identifier identifier = new Identifier(
                getServerURI() + "/" + PRESENT_READABLE_IDENTIFIER);
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        instance.checkAccess();
    }

    @Test
    void testCheckAccessWithMalformedURI() throws Exception {
        server.start();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX, "");

        Identifier identifier = new Identifier(
                getServerURI().toString().replace("://", "//") + "/" +
                        PRESENT_READABLE_IDENTIFIER);
        instance.setIdentifier(identifier);

        assertThrows(IOException.class, () -> instance.checkAccess());
    }

    /* getFormatIterator() */

    @Test
    void testGetFormatIteratorHasNext() {
        instance.setIdentifier(new Identifier("jpg.jpg"));

        HttpSource.FormatIterator<Format> it = instance.getFormatIterator();

        assertTrue(it.hasNext());
        it.next(); // URI path extension
        assertTrue(it.hasNext());
        it.next(); // identifier extension
        assertTrue(it.hasNext());
        it.next(); // Content-Type is null
        assertTrue(it.hasNext());
        it.next(); // magic bytes
        assertFalse(it.hasNext());
    }

    @Test
    void testGetFormatIteratorNext() throws Exception {
        final String fixture = "jpg-incorrect-extension.png";
        instance.setIdentifier(new Identifier(fixture));
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
                response.setHeader("Accept-Ranges", "bytes");
                try (OutputStream os = response.getOutputStream()) {
                    Files.copy(TestUtil.getImage(fixture), os);
                }
            }
        });
        server.start();

        HttpSource.FormatIterator<Format> it = instance.getFormatIterator();
        assertEquals(Format.get("png"), it.next()); // URI path extension
        assertEquals(Format.get("png"), it.next()); // identifier extension
        assertEquals(Format.UNKNOWN, it.next());    // Content-Type is null
        assertEquals(Format.get("jpg"), it.next()); // magic bytes
        assertThrows(NoSuchElementException.class, it::next);
    }

    /* getRequestInfo() */

    @Test
    void testGetRequestInfoUsingBasicLookupStrategyWithPrefix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                getScheme() + "://example.org/prefix/");

        server.start();

        instance.setIdentifier(new Identifier("id"));
        assertEquals(getScheme() + "://example.org/prefix/id",
                instance.getRequestInfo().getURI());
    }

    @Test
    void testGetRequestInfoUsingBasicLookupStrategyWithPrefixAndSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                getScheme() + "://example.org/prefix/");
        config.setProperty(Key.HTTPSOURCE_URL_SUFFIX, "/suffix");

        server.start();

        instance.setIdentifier(new Identifier("id"));
        assertEquals(getScheme() + "://example.org/prefix/id/suffix",
                instance.getRequestInfo().toString());
    }

    @Test
    void testGetRequestInfoUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX, "");
        config.setProperty(Key.HTTPSOURCE_URL_SUFFIX, "");

        server.start();

        instance.setIdentifier(new Identifier(getScheme() + "://example.org/images/image.jpg"));
        assertEquals(getScheme() + "://example.org/images/image.jpg",
                instance.getRequestInfo().toString());
    }

    @Test
    void testGetRequestInfoUsingScriptLookupStrategyReturningString()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier(getScheme() + "-" +
                PRESENT_READABLE_IDENTIFIER);
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);

        server.start();

        instance.setIdentifier(identifier);
        assertEquals(getScheme() + "://example.org/bla/" + identifier,
                instance.getRequestInfo().getURI());
    }

    @Test
    void testGetRequestInfoUsingScriptLookupStrategyWithContextReturningString()
            throws Exception {
        useScriptLookupStrategy();

        final Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-Proto", getScheme());

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(PRESENT_READABLE_IDENTIFIER);
        proxy.getRequestContext().setClientIP("1.2.3.4");
        proxy.getRequestContext().setRequestHeaders(headers);
        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        instance.setDelegateProxy(proxy);

        server.start();

        assertEquals(getScheme() + "://other-example.org/bleh/" + PRESENT_READABLE_IDENTIFIER,
                instance.getRequestInfo().getURI());
    }

    @Test
    void testGetRequestInfoUsingScriptLookupStrategyReturningHash()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier(getScheme() + "-jpg-rgb-64x56x8-plane.jpg");
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        server.start();

        HTTPRequestInfo actual = instance.getRequestInfo();
        assertEquals(getScheme() + "://example.org/bla/" + identifier,
                actual.getURI());
        assertEquals("username", actual.getUsername());
        assertEquals("secret", actual.getSecret());
        Headers headers = actual.getHeaders();
        assertEquals("yes", headers.getFirstValue("X-Custom"));
        assertTrue(actual.isSendingHeadRequest());
    }

    @Test
    void testGetRequestInfoUsingScriptLookupStrategyReturningNil()
            throws Exception {
        useScriptLookupStrategy();
        server.start();

        Identifier identifier = new Identifier("bogus");
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertThrows(NoSuchFileException.class, instance::getRequestInfo);
    }

    /* newStreamFactory() */

    @Test
    void testNewStreamFactoryUsingBasicLookupStrategyWithValidAuthentication()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_BASIC_AUTH_USERNAME,
                WebServer.BASIC_USER);
        config.setProperty(Key.HTTPSOURCE_BASIC_AUTH_SECRET,
                WebServer.BASIC_SECRET);

        server.setBasicAuthEnabled(true);
        server.start();

        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        assertNotNull(instance.newStreamFactory());
    }

    @Test
    void testNewStreamFactoryUsingBasicLookupStrategyWithPresentReadableImage()
            throws Exception {
        doTestNewStreamFactoryWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    @Test
    void testNewStreamFactoryUsingScriptLookupStrategyWithValidAuthentication()
            throws Exception {
        useScriptLookupStrategy();

        server.setBasicAuthEnabled(true);
        server.start();

        Identifier identifier = new Identifier("valid-auth-" +
                getServerURI() + "/" + PRESENT_READABLE_IDENTIFIER);
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertNotNull(instance.newStreamFactory());
    }

    @Test
    void testNewStreamFactoryUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/" +
                PRESENT_READABLE_IDENTIFIER);
        doTestNewStreamFactoryWithPresentReadableImage(identifier);
    }

    private void doTestNewStreamFactoryWithPresentReadableImage(Identifier identifier)
            throws Exception {
        server.start();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertNotNull(instance.newStreamFactory());
    }

    /**
     * Simulates a full usage cycle, checking that no unnecessary requests are
     * made.
     */
    @Test
    void testNoUnnecessaryRequestsWithHEADRequestsEnabled() throws Exception {
        final RequestCountingHandler handler = new RequestCountingHandler();
        server.setHandler(handler);
        server.start();

        instance.checkAccess();
        instance.getFormatIterator().next();

        StreamFactory source = instance.newStreamFactory();
        try (InputStream is = source.newInputStream()) {
            is.readAllBytes();
        }

        assertEquals(1, handler.numHEADRequests);
        assertEquals(1, handler.numGETRequests);
    }

    /**
     * Simulates a full usage cycle, checking that no unnecessary requests are
     * made.
     */
    @Test
    void testNoUnnecessaryRequestsWithHEADRequestsDisabled() throws Exception {
        var config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_SEND_HEAD_REQUESTS, false);

        final RequestCountingHandler handler = new RequestCountingHandler();
        server.setHandler(handler);
        server.start();

        instance.checkAccess();
        instance.getFormatIterator().next();

        StreamFactory source = instance.newStreamFactory();
        try (InputStream is = source.newInputStream()) {
            is.readAllBytes();
        }

        assertEquals(0, handler.numHEADRequests);
        assertEquals(2, handler.numGETRequests);
    }

}
