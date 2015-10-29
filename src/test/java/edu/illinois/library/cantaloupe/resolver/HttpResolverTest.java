package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import org.apache.commons.configuration.BaseConfiguration;

import java.io.FileNotFoundException;
import java.io.IOException;

public class HttpResolverTest extends CantaloupeTestCase {

    private static final Identifier IMAGE = new Identifier("14405804_o1.jpg");
    HttpResolver instance;

    public void setUp() throws IOException {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("HttpResolver.url_prefix",
                "https://ia601502.us.archive.org/4/items/14405804O1_201507/");
        Application.setConfiguration(config);

        instance = new HttpResolver();
    }

    public void testGetInputStream() {
        // present, readable image
        try {
            assertNotNull(instance.getInputStream(IMAGE));
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
        assertEquals(SourceFormat.JPG, instance.getSourceFormat(IMAGE));
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

    public void testGetUrl() {
        BaseConfiguration config = (BaseConfiguration) Application.getConfiguration();
        // with prefix
        config.setProperty("HttpResolver.url_prefix",
                "http://example.org/prefix/");
        assertEquals("http://example.org/prefix/id",
                instance.getUrl(new Identifier("id")).toString());
        // with suffix
        config.setProperty("HttpResolver.url_suffix", "/suffix");
        assertEquals("http://example.org/prefix/id/suffix",
                instance.getUrl(new Identifier("id")).toString());
        // without prefix or suffix
        config.setProperty("HttpResolver.url_prefix", "");
        config.setProperty("HttpResolver.url_suffix", "");
        assertEquals("http://example.org/images/image.jpg",
                instance.getUrl(new Identifier("http://example.org/images/image.jpg")).toString());
        // with path separator
        config.setProperty("HttpResolver.url_prefix", "http://example.org/");
        config.setProperty("HttpResolver.url_suffix", "");
        String separator = "CATS";
        config.setProperty("HttpResolver.path_separator", separator);
        assertEquals("http://example.org/1/2/3",
                instance.getUrl(new Identifier("1" + separator + "2" + separator + "3")).toString());
    }

}
