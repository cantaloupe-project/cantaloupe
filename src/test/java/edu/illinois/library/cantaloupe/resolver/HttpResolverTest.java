package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.restlet.data.Reference;

import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class HttpResolverTest extends CantaloupeTestCase {

    private static final Identifier IDENTIFIER = new Identifier("14405804_o1.jpg");

    private HttpResolver instance;

    public void setUp() throws IOException {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty(HttpResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "BasicLookupStrategy");
        config.setProperty(HttpResolver.URL_PREFIX_CONFIG_KEY,
                "https://ia601502.us.archive.org/4/items/14405804O1_201507/");
        Application.setConfiguration(config);

        instance = new HttpResolver();
    }

    public void testExecuteLookupScript() throws Exception {
        // valid script
        File script = TestUtil.getFixture("lookup.rb");
        String result = instance.executeLookupScript(IDENTIFIER, script);
        assertEquals("http://example.org/bla/" + IDENTIFIER, result);

        // unsupported script
        try {
            script = TestUtil.getFixture("lookup.js");
            instance.executeLookupScript(IDENTIFIER, script);
            fail("Expected exception");
        } catch (ScriptException e) {
            assertEquals("Unsupported script type: js", e.getMessage());
        }
    }

    public void testGetInputStream() {
        // present, readable image
        try {
            assertNotNull(instance.getInputStream(IDENTIFIER));
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.getInputStream(new Identifier("bogus"));
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
        // present, unreadable image
        // TODO: write this
    }

    public void testGetSourceFormat() throws IOException {
        assertEquals(SourceFormat.JPG, instance.getSourceFormat(IDENTIFIER));
        try {
            instance.getSourceFormat(new Identifier("image.bogus"));
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
        try {
            instance.getSourceFormat(new Identifier("image"));
            fail("Expected exception");
        } catch (IOException e) {
            // pass
        }
    }

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

    public void testGetUrlWithScriptLookupStrategyAndAbsolutePath()
            throws Exception {
        Configuration config = Application.getConfiguration();
        config.setProperty(HttpResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");

        // valid, present script
        config.setProperty(HttpResolver.LOOKUP_SCRIPT_CONFIG_KEY,
                TestUtil.getFixture("lookup.rb").getAbsolutePath());
        assertEquals(new Reference("http://example.org/bla/" + IDENTIFIER),
                instance.getUrl(IDENTIFIER));

        // missing script
        try {
            config.setProperty(HttpResolver.LOOKUP_SCRIPT_CONFIG_KEY,
                    TestUtil.getFixture("bogus.rb").getAbsolutePath());
            instance.getUrl(IDENTIFIER);
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    public void testGetUrlWithScriptLookupStrategyAndRelativePath()
            throws Exception {
        Configuration config = Application.getConfiguration();
        config.setProperty(HttpResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");

        // filename of script, located in cwd
        config.setProperty(HttpResolver.LOOKUP_SCRIPT_CONFIG_KEY,
                "lookup.rb");
        final File tempFile = new File("./lookup.rb");
        try {
            FileUtils.copyFile(TestUtil.getFixture("lookup.rb"), tempFile);
            assertEquals(new Reference("http://example.org/bla/" + IDENTIFIER),
                    instance.getUrl(IDENTIFIER));
        } finally {
            //FileUtils.forceDelete(tempFile);
        }
    }

    public void testGetUrlWithScriptLookupStrategyAndPathSeparator()
            throws Exception {
        Configuration config = Application.getConfiguration();
        config.setProperty(HttpResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");
        config.setProperty(HttpResolver.LOOKUP_SCRIPT_CONFIG_KEY,
                TestUtil.getFixture("lookup.rb").getAbsolutePath());

        String separator = "CATS";
        config.setProperty(HttpResolver.PATH_SEPARATOR_CONFIG_KEY, separator);
        final Reference expected = new Reference("http://example.org/bla/1" +
                File.separator + "2" + File.separator + "3");
        final Reference actual = instance.getUrl(
                new Identifier("1" + separator + "2" + separator + "3"));
        assertEquals(expected, actual);
    }

}
