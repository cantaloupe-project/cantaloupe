package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

abstract class HttpSource2Test extends AbstractSourceTest {

    private static final Identifier PRESENT_READABLE_IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    WebServer server;

    private HttpSource2 instance;

    /**
     * Subclasses need to override, call super, and set
     * {@link Key#HTTPSOURCE_URL_PREFIX} to the web server URI using the
     * appropriate scheme.
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb"));

        instance = newInstance();
    }

    @Override
    @After
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
    HttpSource2 newInstance() {
        HttpSource2 instance = new HttpSource2();
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
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.HTTPSOURCE_LOOKUP_STRATEGY,
                    "ScriptLookupStrategy");
            config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
            config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                    TestUtil.getFixture("delegates.rb").toString());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /* checkAccess() */

    @Test
    public void testCheckAccessUsingBasicLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        doTestCheckAccessWithPresentUnreadableImage(new Identifier("gif"));
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/" +
                PRESENT_READABLE_IDENTIFIER);
        doTestCheckAccessWithPresentReadableImage(identifier);
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithMissingImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/bogus");
        doTestCheckAccessWithMissingImage(identifier);
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/gif");
        doTestCheckAccessWithPresentUnreadableImage(identifier);
    }

    private void doTestCheckAccessWithPresentReadableImage(Identifier identifier)
            throws Exception {
        server.start();

        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        instance.checkAccess();
    }

    private void doTestCheckAccessWithPresentUnreadableImage(Identifier identifier)
            throws Exception {
        try {
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

            RequestContext context = new RequestContext();
            context.setIdentifier(identifier);
            DelegateProxyService service = DelegateProxyService.getInstance();
            DelegateProxy proxy = service.newDelegateProxy(context);
            instance.setDelegateProxy(proxy);
            instance.setIdentifier(identifier);
            instance.setIdentifier(identifier);
            instance.checkAccess();
            fail("Expected exception");
        } catch (AccessDeniedException e) {
            // pass
        }
    }

    private void doTestCheckAccessWithMissingImage(Identifier identifier)
            throws Exception {
        try {
            server.start();

            RequestContext context = new RequestContext();
            context.setIdentifier(identifier);
            DelegateProxyService service = DelegateProxyService.getInstance();
            DelegateProxy proxy = service.newDelegateProxy(context);
            instance.setDelegateProxy(proxy);
            instance.setIdentifier(identifier);

            instance.checkAccess();
            fail("Expected exception");
        } catch (NoSuchFileException e) {
            // pass
        }
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithValidAuthentication()
            throws Exception {
        useScriptLookupStrategy();

        server.setBasicAuthEnabled(true);
        server.start();

        Identifier identifier = new Identifier("valid-auth-" +
                getServerURI() + "/" + PRESENT_READABLE_IDENTIFIER);
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        instance.checkAccess();
    }

    @Test(expected = AccessDeniedException.class)
    public void testCheckAccessUsingScriptLookupStrategyWithInvalidAuthentication()
            throws Exception {
        useScriptLookupStrategy();

        server.setBasicAuthEnabled(true);
        server.start();

        Identifier identifier = new Identifier("invalid-auth-" +
                getServerURI() + "/" + PRESENT_READABLE_IDENTIFIER);
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        instance.checkAccess();
    }

    @Test
    public void testCheckAccessWith403Response() throws Exception {
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
    public void testCheckAccessWith500Response() throws Exception {
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
    public void testCheckAccessSendsUserAgentHeader() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                String expected = String.format("%s/%s (%s/%s; java/%s; %s/%s)",
                        HttpSource2.class.getSimpleName(),
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
    public void testCheckAccessSendsCustomHeaders() throws Exception {
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
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        instance.checkAccess();
    }

    @Test
    public void testCheckAccessWithMalformedURI() throws Exception {
        server.start();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX, "");

        Identifier identifier = new Identifier(
                getServerURI().toString().replace("://", "//") + "/" + PRESENT_READABLE_IDENTIFIER);
        instance.setIdentifier(identifier);

        try {
            instance.checkAccess();
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    /* getFormat() */

    /**
     * Tests {@link HttpSource2#getFormat()} when the URI contains an extension.
     */
    @Test
    public void testGetFormat1() {
        assertEquals(Format.JPG, instance.getFormat());
    }

    /**
     * Tests {@link HttpSource#getFormat()} when the identifier contains an
     * extension.
     */
    @Test
    public void testGetFormat2() throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("HttpSourceTest-" +
                "extension-in-identifier-but-not-filename.jpg");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(new Identifier(getServerURI() + "/" + identifier));

        assertEquals(Format.JPG, instance.getFormat());
    }

    /**
     * Tests {@link HttpSource2#getFormat()} when neither the URI nor identifier
     * contain an extension, but there is a recognized Content-Type header.
     */
    @Test
    public void testGetFormat3() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                response.setHeader("Content-Type", "image/jpeg; charset=UTF-8");
                baseRequest.setHandled(true);
            }
        });
        server.start();

        instance.setIdentifier(new Identifier("jpg"));
        assertEquals(Format.JPG, instance.getFormat());
    }

    /**
     * Tests {@link HttpSource2#getFormat()} when neither the URI nor identifier
     * contain an extension, there is an unrecognized Content-Type header, and
     * the server does not support ranges.
     */
    @Test
    public void testGetFormat4() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                response.setHeader("Content-Type", "application/octet-stream");
                baseRequest.setHandled(true);
            }
        });
        server.start();

        instance.setIdentifier(new Identifier("jpg"));
        assertEquals(Format.UNKNOWN, instance.getFormat());
    }

    /**
     * Tests {@link HttpSource2#getFormat()} when neither the URI nor identifier
     * contain an extension, there is no Content-Type header, and the server
     * does not support ranges.
     */
    @Test
    public void testGetFormat5() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                baseRequest.setHandled(true);
            }
        });
        server.start();

        instance.setIdentifier(new Identifier("jpg"));
        assertEquals(Format.UNKNOWN, instance.getFormat());
    }

    /**
     * Tests {@link HttpSource2#getFormat()} when neither the URI nor identifier
     * contain an extension, there is no Content-Type header, and the server
     * supports ranges.
     */
    @Test
    public void testGetFormat6() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
                response.setHeader("Accept-Ranges", "bytes");
                try (OutputStream os = response.getOutputStream()) {
                    Files.copy(TestUtil.getImage("jpg"), os);
                }
            }
        });
        server.start();

        // N.B.: Neither identifier nor URI can contain an extension.
        instance.setIdentifier(new Identifier("jpg"));
        assertEquals(Format.JPG, instance.getFormat());
    }

    /**
     * Tests {@link HttpSource2#getFormat()} when neither the URI nor identifier
     * contain an extension, there is no Content-Type header, and the server
     * supports ranges, but the ranged GET response returns unknown magic bytes.
     */
    @Test
    public void testGetFormat7() throws Exception {
        server.start();

        instance.setIdentifier(new Identifier("txt"));
        assertEquals(Format.UNKNOWN, instance.getFormat());
    }

    /* getRequestInfo() */

    @Test
    public void testGetRequestInfoUsingBasicLookupStrategyWithPrefix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                getScheme() + "://example.org/prefix/");

        server.start();

        instance.setIdentifier(new Identifier("id"));
        assertEquals(getScheme() + "://example.org/prefix/id",
                instance.getRequestInfo().getURI().toString());
    }

    @Test
    public void testGetRequestInfoUsingBasicLookupStrategyWithPrefixAndSuffix()
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
    public void testGetRequestInfoUsingBasicLookupStrategyWithoutPrefixOrSuffix()
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
    public void testGetRequestInfoUsingScriptLookupStrategyReturningString()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier(getScheme() + "-" +
                PRESENT_READABLE_IDENTIFIER);
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);

        server.start();

        instance.setIdentifier(identifier);
        assertEquals(getScheme() + "://example.org/bla/" + identifier,
                instance.getRequestInfo().getURI());
    }

    @Test
    public void testGetRequestInfoUsingScriptLookupStrategyWithContextReturningString()
            throws Exception {
        useScriptLookupStrategy();

        final Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-Proto", getScheme());

        RequestContext context = new RequestContext();
        context.setClientIP("1.2.3.4");
        context.setRequestHeaders(headers);
        context.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        instance.setDelegateProxy(proxy);

        server.start();

        assertEquals(getScheme() + "://other-example.org/bleh/" + PRESENT_READABLE_IDENTIFIER,
                instance.getRequestInfo().getURI());
    }

    @Test
    public void testGetRequestInfoUsingScriptLookupStrategyReturningHash()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier(getScheme() + "-jpg-rgb-64x56x8-plane.jpg");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
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
    }

    @Test(expected = NoSuchFileException.class)
    public void testGetRequestInfoUsingScriptLookupStrategyReturningNil()
            throws Exception {
        useScriptLookupStrategy();
        server.start();

        Identifier identifier = new Identifier("bogus");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        instance.getRequestInfo();
    }

    /* newStreamFactory() */

    @Test
    public void testNewStreamFactoryUsingBasicLookupStrategyWithValidAuthentication()
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
    public void testNewStreamFactoryUsingBasicLookupStrategyWithPresentReadableImage()
            throws Exception {
        doTestNewStreamFactoryWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    @Test
    public void testNewStreamFactoryUsingScriptLookupStrategyWithValidAuthentication()
            throws Exception {
        useScriptLookupStrategy();

        server.setBasicAuthEnabled(true);
        server.start();

        Identifier identifier = new Identifier("valid-auth-" +
                getServerURI() + "/" + PRESENT_READABLE_IDENTIFIER);
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertNotNull(instance.newStreamFactory());
    }

    @Test
    public void testNewStreamFactoryUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/" +
                PRESENT_READABLE_IDENTIFIER);
        doTestNewStreamFactoryWithPresentReadableImage(identifier);
    }

    private void doTestNewStreamFactoryWithPresentReadableImage(Identifier identifier)
            throws Exception {
        server.start();

        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertNotNull(instance.newStreamFactory());
    }

    @Test
    public void testNoUnnecessaryRequests() throws Exception {
        final AtomicInteger numHEADRequests = new AtomicInteger(0);
        final AtomicInteger numGETRequests = new AtomicInteger(0);

        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                switch (request.getMethod().toUpperCase()) {
                    case "HEAD":
                        numHEADRequests.incrementAndGet();
                        break;
                    case "GET":
                        numGETRequests.incrementAndGet();
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Unexpected method: " + request.getMethod());
                }
                baseRequest.setHandled(true);
            }
        });
        server.start();

        instance.checkAccess();
        instance.getFormat();

        StreamFactory source = instance.newStreamFactory();
        try (InputStream is = source.newInputStream()) {
            is.read();
        }

        assertEquals(1, numHEADRequests.get());
        assertEquals(1, numGETRequests.get());
    }

}
