package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.AccessDeniedException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

abstract class HttpResolverTest extends BaseTest {

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    static WebServer server;

    private HttpResolver instance;

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

        instance = new HttpResolver();
        instance.setIdentifier(IDENTIFIER);
    }

    /* newStreamSource() */

    @Test
    public void testNewStreamSourceWithPresentReadableImage() {
        try {
            assertNotNull(instance.newStreamSource());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testNewStreamSourceWithMissingImage() {
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
    @Ignore // TODO: Jetty server returns HTTP 200 for unreadable files
    public void testNewStreamSourceWithPresentUnreadableImage()
            throws Exception {
        File image = TestUtil.getFixture("gif");
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
    public void testGetSourceFormat() throws Exception {
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
    public void testGetResourceInfoUsingBasicLookupStrategyWithPrefix()
            throws Exception {
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
    public void testGetResourceInfoUsingScriptLookupStrategyReturningHash()
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
    public void testGetResourceInfoUsingScriptLookupStrategyReturningNil()
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
