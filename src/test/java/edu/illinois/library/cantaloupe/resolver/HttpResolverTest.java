package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.WebServer;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.Reference;

import java.io.FileNotFoundException;
import java.io.IOException;

public class HttpResolverTest {

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private HttpResolver instance;
    private WebServer server;

    @Before
    public void setUp() throws Exception {
        server = new WebServer();
        server.start();

        BaseConfiguration config = new BaseConfiguration();
        config.setProperty(HttpResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "BasicLookupStrategy");
        config.setProperty(HttpResolver.URL_PREFIX_CONFIG_KEY,
                "http://localhost:" + server.getPort() + "/");
        Application.setConfiguration(config);

        instance = new HttpResolver();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testGetChannelWithPresentReadableImage() throws IOException {
        try {
            assertNotNull(instance.getChannelSource(IDENTIFIER));
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void testGetChannelWithMissingImage() throws IOException {
        try {
            instance.getChannelSource(new Identifier("bogus"));
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    @Test
    public void testGetChannelWithPresentUnreadableImage() throws IOException {
        /* TODO: possible restlet bug: https://github.com/restlet/restlet-framework-java/issues/1179
        File image = TestUtil.getFixture("gif");
        try {
            image.setReadable(false);
            instance.getChannel(new Identifier("gif"));
            fail("Expected exception");
        } catch (AccessDeniedException e) {
            // pass
        } finally {
            image.setReadable(true);
        }
        */
    }

    @Test
    public void testGetSourceFormat() throws IOException {
        assertEquals(SourceFormat.JPG, instance.getSourceFormat(IDENTIFIER));
        try {
            instance.getSourceFormat(new Identifier("image.bogus"));
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetUrlWithBasicLookupStrategy() throws Exception {
        BaseConfiguration config = (BaseConfiguration) Application.getConfiguration();
        // with prefix
        config.setProperty(HttpResolver.URL_PREFIX_CONFIG_KEY,
                "http://example.org/prefix/");
        assertEquals("http://example.org/prefix/id",
                instance.getUrl(new Identifier("id")).toString());
        // with suffix
        config.setProperty(HttpResolver.URL_SUFFIX_CONFIG_KEY, "/suffix");
        assertEquals("http://example.org/prefix/id/suffix",
                instance.getUrl(new Identifier("id")).toString());
        // without prefix or suffix
        config.setProperty(HttpResolver.URL_PREFIX_CONFIG_KEY, "");
        config.setProperty(HttpResolver.URL_SUFFIX_CONFIG_KEY, "");
        assertEquals("http://example.org/images/image.jpg",
                instance.getUrl(new Identifier("http://example.org/images/image.jpg")).toString());
    }

    @Test
    public void testGetUrlWithScriptLookupStrategy() throws Exception {
        Configuration config = Application.getConfiguration();
        config.setProperty(HttpResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");

        // valid, present script
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        assertEquals(new Reference("http://example.org/bla/" + IDENTIFIER),
                instance.getUrl(IDENTIFIER));

        // missing script
        try {
            config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_CONFIG_KEY,
                    TestUtil.getFixture("bogus.rb").getAbsolutePath());
            instance.getUrl(IDENTIFIER);
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

}
