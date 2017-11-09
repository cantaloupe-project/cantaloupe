package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

abstract class HttpResolverTest extends BaseTest {

    private static final Identifier PRESENT_READABLE_IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    static WebServer server;

    private HttpResolver instance;
    private RequestContext context;

    /**
     * Subclasses need to override, call super, and set
     * {@link Key#HTTPRESOLVER_URL_PREFIX} to the web server URI using the
     * appropriate scheme.
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context = new RequestContext();
        instance = new HttpResolver();
        instance.setIdentifier(PRESENT_READABLE_IDENTIFIER);
        instance.setContext(context);

        useBasicLookupStrategy();
    }

    private void useBasicLookupStrategy() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
    }

    private void useScriptLookupStrategy() throws IOException {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
    }

    /* newStreamSource() */

    @Test
    public void testNewStreamSourceUsingBasicLookupStrategyWithPresentReadableImage() {
        useBasicLookupStrategy();
        doTestNewStreamSourceWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    @Test
    public void testNewStreamSourceUsingBasicLookupStrategyWithMissingImage() {
        useBasicLookupStrategy();
        doTestNewStreamSourceWithMissingImage(new Identifier("bogus"));
    }

    @Test
    public void testNewStreamSourceWithUsingBasicLookupStrategyPresentUnreadableImage()
            throws Exception {
        useBasicLookupStrategy();
        doTestNewStreamSourceWithPresentUnreadableImage(new Identifier("gif"));
    }

    @Test
    public void testNewStreamSourceUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(server.getHTTPURI() + "/" +
                PRESENT_READABLE_IDENTIFIER);
        doTestNewStreamSourceWithPresentReadableImage(identifier);
    }

    @Test
    public void testNewStreamSourceUsingScriptLookupStrategyWithMissingImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(server.getHTTPURI() + "/bogus");
        doTestNewStreamSourceWithMissingImage(identifier);
    }

    @Test
    public void testNewStreamSourceUsingScriptLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(server.getHTTPURI() + "/gif");
        doTestNewStreamSourceWithPresentUnreadableImage(identifier);
    }

    private void doTestNewStreamSourceWithPresentReadableImage(
            Identifier identifier) {
        try {
            instance.setIdentifier(identifier);
            assertNotNull(instance.newStreamSource());
        } catch (IOException e) {
            fail();
        }
    }

    private void doTestNewStreamSourceWithPresentUnreadableImage(Identifier identifier) {
        try {
            File image = TestUtil.getImage("gif");
            try {
                image.setReadable(false);
                instance.setIdentifier(identifier);
                instance.newStreamSource();
                fail("Expected exception");
            } finally {
                image.setReadable(true);
            }
        } catch (AccessDeniedException e) {
            // pass
        } catch (Exception e) {
            fail("Expected AccessDeniedException");
        }
    }

    private void doTestNewStreamSourceWithMissingImage(Identifier identifier) {
        try {
            instance.setIdentifier(identifier);
            instance.newStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    /* getSourceFormat() */

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategyWithPresentReadableImage()
            throws Exception {
        useBasicLookupStrategy();
        doTestGetSourceFormatWithPresentReadableImage(PRESENT_READABLE_IDENTIFIER);
    }

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        useBasicLookupStrategy();
        doTestGetSourceFormatWithPresentUnreadableImage(new Identifier("gif"));
    }

    @Test
    public void testGetSourceFormatUsingBasicLookupStrategyWithMissingImage()
            throws Exception {
        useBasicLookupStrategy();
        doTestGetSourceFormatWithMissingImage(new Identifier("bogus"));
    }

    @Test
    public void testGetSourceFormatUsingScriptLookupStrategyWithPresentReadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(server.getHTTPURI() + "/" +
                PRESENT_READABLE_IDENTIFIER);
        doTestGetSourceFormatWithPresentReadableImage(identifier);
    }

    @Test
    public void testGetSourceFormatUsingScriptLookupStrategyWithPresentUnreadableImage()
            throws Exception {
        useScriptLookupStrategy();
        Identifier identifier = new Identifier(server.getHTTPURI() + "/gif");
        doTestGetSourceFormatWithPresentUnreadableImage(identifier);
    }

    @Test
    public void testGetSourceFormatUsingScriptLookupStrategyWithMissingImage()
            throws Exception {
        useScriptLookupStrategy();
        doTestGetSourceFormatWithMissingImage(new Identifier("bogus"));
    }

    private void doTestGetSourceFormatWithPresentReadableImage(
            Identifier identifier) {
        try {
            instance.setIdentifier(identifier);
            assertEquals(Format.JPG, instance.getSourceFormat());
        } catch (Exception e) {
            fail();
        }
    }

    private void doTestGetSourceFormatWithPresentUnreadableImage(
            Identifier identifier) {
        try {
            File image = TestUtil.getImage("gif");
            try {
                image.setReadable(false);
                instance.setIdentifier(identifier);
                instance.getSourceFormat();
                fail("Expected exception");
            } finally {
                image.setReadable(true);
            }
        } catch (AccessDeniedException e) {
            // pass
        } catch (Exception e) {
            fail("Expected AccessDeniedException");
        }
    }

    private void doTestGetSourceFormatWithMissingImage(Identifier identifier) {
        try {
            assertEquals(Format.JPG, instance.getSourceFormat());
            instance.setIdentifier(identifier);
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    /* getResourceInfo() */

    @Test
    public void testGetResourceInfoUsingBasicLookupStrategyWithPrefix()
            throws Exception {
        useBasicLookupStrategy();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                "http://example.org/prefix/");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("http://example.org/prefix/id",
                instance.getResourceInfo().getURI().toString());
    }

    @Test
    public void testGetResourceInfoUsingBasicLookupStrategyWithPrefixAndSuffix()
            throws Exception {
        useBasicLookupStrategy();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                "http://example.org/prefix/");
        config.setProperty(Key.HTTPRESOLVER_URL_SUFFIX, "/suffix");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("http://example.org/prefix/id/suffix",
                instance.getResourceInfo().toString());
    }

    @Test
    public void testGetResourceInfoUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        useBasicLookupStrategy();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX, "");
        config.setProperty(Key.HTTPRESOLVER_URL_SUFFIX, "");
        instance.setIdentifier(new Identifier("http://example.org/images/image.jpg"));
        assertEquals("http://example.org/images/image.jpg",
                instance.getResourceInfo().toString());
    }

    @Test
    public void testGetResourceInfoUsingScriptLookupStrategyReturningString()
            throws Exception {
        useScriptLookupStrategy();
        assertEquals(new URI("http://example.org/bla/" + PRESENT_READABLE_IDENTIFIER),
                instance.getResourceInfo().getURI());
    }

    @Test
    public void testGetResourceInfoUsingScriptLookupStrategyWithContextReturningString()
            throws Exception {
        useScriptLookupStrategy();

        final Map<String, String> headers = new HashMap<>();
        headers.put("x-test-header", "foo");
        context.setClientIP("1.2.3.4");
        context.setRequestHeaders(headers);

        assertEquals(new URI("http://other-example.org/bleh/" + PRESENT_READABLE_IDENTIFIER),
                instance.getResourceInfo().getURI());
    }

    @Test
    public void testGetResourceInfoUsingScriptLookupStrategyReturningHash()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("jpg-rgb-64x56x8-plane.jpg");
        instance.setIdentifier(identifier);
        HttpResolver.ResourceInfo actual = instance.getResourceInfo();
        assertEquals(new URI("http://example.org/bla/" + identifier),
                actual.getURI());
        assertEquals("username", actual.getUsername());
        assertEquals("secret", actual.getSecret());
    }

    @Test
    public void testGetResourceInfoUsingScriptLookupStrategyReturningNil()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("bogus");
        instance.setIdentifier(identifier);
        try {
            instance.getResourceInfo();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

}
