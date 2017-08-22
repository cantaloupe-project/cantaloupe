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

    private static final Identifier IDENTIFIER =
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

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");

        context = new RequestContext();
        instance = new HttpResolver();
        instance.setIdentifier(IDENTIFIER);
        instance.setContext(context);
    }

    /* newStreamSource() */

    @Test
    public void newStreamSourceWithPresentReadableImage() {
        try {
            assertNotNull(instance.newStreamSource());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void newStreamSourceWithMissingImage() {
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.newStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    @Test
    public void newStreamSourceWithPresentUnreadableImage() throws Exception {
        File image = TestUtil.getImage("gif");
        try {
            image.setReadable(false);
            instance.setIdentifier(new Identifier("gif"));
            instance.newStreamSource();
            fail("Expected exception");
        } catch (AccessDeniedException e) {
            // pass
        } finally {
            image.setReadable(true);
        }
    }

    /* getSourceFormat() */

    @Test
    public void getSourceFormat() throws Exception {
        assertEquals(Format.JPG, instance.getSourceFormat());
        try {
            instance.setIdentifier(new Identifier("image.bogus"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    /* getResourceInfo() */

    @Test
    public void getResourceInfoUsingBasicLookupStrategyWithPrefix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                "http://example.org/prefix/");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("http://example.org/prefix/id",
                instance.getResourceInfo().getURI().toString());
    }

    @Test
    public void getResourceInfoUsingBasicLookupStrategyWithPrefixAndSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                "http://example.org/prefix/");
        config.setProperty(Key.HTTPRESOLVER_URL_SUFFIX, "/suffix");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("http://example.org/prefix/id/suffix",
                instance.getResourceInfo().toString());
    }

    @Test
    public void getResourceInfoUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX, "");
        config.setProperty(Key.HTTPRESOLVER_URL_SUFFIX, "");
        instance.setIdentifier(new Identifier("http://example.org/images/image.jpg"));
        assertEquals("http://example.org/images/image.jpg",
                instance.getResourceInfo().toString());
    }

    @Test
    public void getResourceInfoUsingScriptLookupStrategyReturningString()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        assertEquals(new URI("http://example.org/bla/" + IDENTIFIER),
                instance.getResourceInfo().getURI());
    }

    @Test
    public void getResourceInfoUsingScriptLookupStrategyWithContextReturningString()
            throws Exception {
        final Map<String, String> headers = new HashMap<>();
        headers.put("x-test-header", "foo");
        context.setClientIP("1.2.3.4");
        context.setRequestHeaders(headers);

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        assertEquals(new URI("http://other-example.org/bleh/" + IDENTIFIER),
                instance.getResourceInfo().getURI());
    }

    @Test
    public void getResourceInfoUsingScriptLookupStrategyReturningHash()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());

        Identifier identifier = new Identifier("jpg-rgb-64x56x8-plane.jpg");
        instance.setIdentifier(identifier);
        HttpResolver.ResourceInfo actual = instance.getResourceInfo();
        assertEquals(new URI("http://example.org/bla/" + identifier),
                actual.getURI());
        assertEquals("username", actual.getUsername());
        assertEquals("secret", actual.getSecret());
    }

    @Test
    public void getResourceInfoUsingScriptLookupStrategyReturningNil()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());

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
