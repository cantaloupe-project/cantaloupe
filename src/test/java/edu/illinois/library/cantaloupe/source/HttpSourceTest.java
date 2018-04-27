package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MediaType;
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

abstract class HttpSourceTest extends AbstractSourceTest {

    private static final Identifier PRESENT_READABLE_IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    WebServer server;

    private HttpSource instance;

    /**
     * Subclasses need to override, call super, and set
     * {@link Key#HTTPSOURCE_URL_PREFIX} to the web server URI using the
     * appropriate scheme.
     */
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

    /* mediaTypeFromContentType() */

    @Test
    public void testMediaTypeFromContentTypeWithoutSemicolon() {
        assertEquals(MediaType.APPLICATION_JSON,
                HttpSource.mediaTypeFromContentType("application/json"));
    }

    @Test
    public void testMediaTypeFromContentTypeWithSemicolon() {
        assertEquals(MediaType.APPLICATION_JSON,
                HttpSource.mediaTypeFromContentType("application/json; charset=utf-8"));
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

    /* getSourceFormat() */

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategyWithValidAuthentication()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_BASIC_AUTH_USERNAME,
                WebServer.BASIC_USER);
        config.setProperty(Key.HTTPSOURCE_BASIC_AUTH_SECRET,
                WebServer.BASIC_SECRET);

        server.setBasicAuthEnabled(true);
        server.start();

        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategyWithPresentReadableImage() {
        doTestGetSourceFormatWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    @Test
    public void testGetSourceFormatUsingScriptLookupStrategyWithPresentReadableImage() {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/" +
                PRESENT_READABLE_IDENTIFIER);
        doTestGetSourceFormatWithPresentReadableImage(identifier);
    }

    @Test
    public void testGetSourceFormatFollowsRedirect() throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                if (baseRequest.getPathInfo().startsWith("/" + PRESENT_READABLE_IDENTIFIER)) {
                    response.setStatus(301);
                    response.setHeader("Location",
                            getServerURI().resolve("/jpg-rgb-64x56x8-line.jpg").toString());
                }
                baseRequest.setHandled(true);
            }
        });
        server.start();

        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatWithNoIdentifierExtensionAndContentType()
            throws Exception {
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
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatWithNoIdentifierExtensionAndNoContentType()
            throws Exception {
        server.start();

        instance.setIdentifier(new Identifier("jpg"));
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatWithNoIdentifierExtensionAndUnrecognizedContentType()
            throws Exception {
        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
                response.setHeader("Content-Type", "application/octet-stream");
                try (OutputStream os = response.getOutputStream()) {
                    Files.copy(TestUtil.getImage("jpg"), os);
                }
            }
        });
        server.start();

        instance.setIdentifier(new Identifier("jpg"));
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatWithNoIdentifierExtensionAndNoContentTypeAndUnrecognizedMagicBytes()
            throws Exception {
        server.start();

        instance.setIdentifier(new Identifier("txt"));
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    private void doTestGetSourceFormatWithPresentReadableImage(
            Identifier identifier) {
        try {
            server.start();

            instance.setIdentifier(identifier);
            assertEquals(Format.JPG, instance.getSourceFormat());
        } catch (Exception e) {
            fail();
        }
    }

    /* getResourceInfo() */

    @Test
    public void testGetResourceInfoUsingBasicLookupStrategyWithPrefix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                getScheme() + "://example.org/prefix/");

        server.start();

        instance.setIdentifier(new Identifier("id"));
        assertEquals(getScheme() + "://example.org/prefix/id",
                instance.getResourceInfo().getURI().toString());
    }

    @Test
    public void testGetResourceInfoUsingBasicLookupStrategyWithPrefixAndSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX,
                getScheme() + "://example.org/prefix/");
        config.setProperty(Key.HTTPSOURCE_URL_SUFFIX, "/suffix");

        server.start();

        instance.setIdentifier(new Identifier("id"));
        assertEquals(getScheme() + "://example.org/prefix/id/suffix",
                instance.getResourceInfo().toString());
    }

    @Test
    public void testGetResourceInfoUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPSOURCE_URL_PREFIX, "");
        config.setProperty(Key.HTTPSOURCE_URL_SUFFIX, "");

        server.start();

        instance.setIdentifier(new Identifier(getScheme() + "://example.org/images/image.jpg"));
        assertEquals(getScheme() + "://example.org/images/image.jpg",
                instance.getResourceInfo().toString());
    }

    @Test
    public void testGetResourceInfoUsingScriptLookupStrategyReturningString()
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
        assertEquals(new URI(getScheme() + "://example.org/bla/" + identifier),
                instance.getResourceInfo().getURI());
    }

    @Test
    public void testGetResourceInfoUsingScriptLookupStrategyWithContextReturningString()
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

        assertEquals(new URI(getScheme() + "://other-example.org/bleh/" + PRESENT_READABLE_IDENTIFIER),
                instance.getResourceInfo().getURI());
    }

    @Test
    public void testGetResourceInfoUsingScriptLookupStrategyReturningHash()
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

        HttpSource.ResourceInfo actual = instance.getResourceInfo();
        assertEquals(new URI(getScheme() + "://example.org/bla/" + identifier),
                actual.getURI());
        assertEquals("username", actual.getUsername());
        assertEquals("secret", actual.getSecret());
    }

    @Test(expected = NoSuchFileException.class)
    public void testGetResourceInfoUsingScriptLookupStrategyReturningNil()
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

        instance.getResourceInfo();
    }

    /* newStreamFactory() */

    @Test
    public void testNewStreamSourceUsingBasicLookupStrategyWithValidAuthentication()
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
    public void testNewStreamSourceUsingBasicLookupStrategyWithPresentReadableImage()
            throws Exception {
        doTestNewStreamSourceWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    @Test
    public void testNewStreamSourceUsingScriptLookupStrategyWithValidAuthentication()
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
    public void testNewStreamSourceUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(getServerURI() + "/" +
                PRESENT_READABLE_IDENTIFIER);
        doTestNewStreamSourceWithPresentReadableImage(identifier);
    }

    private void doTestNewStreamSourceWithPresentReadableImage(Identifier identifier)
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
        final AtomicInteger numGETRequests = new AtomicInteger(0);

        server.setHandler(new DefaultHandler() {
            @Override
            public void handle(String target,
                               Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response) {
                switch (request.getMethod().toUpperCase()) {
                    case "GET":
                        numGETRequests.incrementAndGet();
                        break;
                    default:
                        throw new IllegalArgumentException("WTF");
                }
                baseRequest.setHandled(true);
            }
        });
        server.start();

        instance.checkAccess();
        instance.getSourceFormat();

        StreamFactory source = instance.newStreamFactory();
        try (InputStream is = source.newInputStream()) {
            is.read();
        }

        assertEquals(2, numGETRequests.get());
    }

    @Test
    public void testUserAgent() throws Exception {
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

}
