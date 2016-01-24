package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

public class FilesystemResolverTest {

    private static final Identifier IDENTIFIER = new Identifier("images/escher_lego.jpg");

    FilesystemResolver instance;

    private static Configuration newConfiguration() throws IOException {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("delegate_script",
                TestUtil.getFixture("lookup.rb").getAbsolutePath());
        config.setProperty(FilesystemResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "BasicLookupStrategy");
        config.setProperty(FilesystemResolver.PATH_PREFIX_CONFIG_KEY,
                TestUtil.getFixturePath() + File.separator);
        return config;
    }

    @Before
    public void setUp() throws IOException {
        Application.setConfiguration(newConfiguration());
        instance = new FilesystemResolver();
    }

    @Test
    public void testGetChannel() {
        // present, readable image
        try {
            assertNotNull(instance.getChannelSource(IDENTIFIER));
        } catch (IOException e) {
            fail();
        }
        // missing image
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
    public void testGetFile() throws Exception {
        // present, readable file
        try {
            assertNotNull(instance.getFile(IDENTIFIER));
        } catch (FileNotFoundException e) {
            fail();
        }

        // present, unreadable file
        File file = new File(instance.getPathname(IDENTIFIER, File.separator));
        try {
            file.setReadable(false);
            instance.getFile(IDENTIFIER);
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            fail();
        } catch (AccessDeniedException e) {
            // pass
        } finally {
            file.setReadable(true);
        }

        // missing file
        try {
            instance.getFile(new Identifier("bogus"));
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
    }

    // getPathname(Identifier)

    @Test
    public void testGetPathnameWithBasicLookupStrategy() throws IOException {
        Configuration config = Application.getConfiguration();
        config.setProperty(FilesystemResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "BasicLookupStrategy");
        // with prefix
        config.setProperty(FilesystemResolver.PATH_PREFIX_CONFIG_KEY, "/prefix/");
        assertEquals("/prefix/id", instance.getPathname(new Identifier("id"), File.separator));
        // with suffix
        config.setProperty(FilesystemResolver.PATH_SUFFIX_CONFIG_KEY, "/suffix");
        assertEquals("/prefix/id/suffix", instance.getPathname(new Identifier("id"), File.separator));
        // without prefix or suffix
        config.setProperty(FilesystemResolver.PATH_PREFIX_CONFIG_KEY, "");
        config.setProperty(FilesystemResolver.PATH_SUFFIX_CONFIG_KEY, "");
        assertEquals("id", instance.getPathname(new Identifier("id"), File.separator));
        // test sanitization
        config.setProperty(FilesystemResolver.PATH_PREFIX_CONFIG_KEY, "");
        config.setProperty(FilesystemResolver.PATH_SUFFIX_CONFIG_KEY, "");
        assertEquals("id/", instance.getPathname(new Identifier("id/../"), "/"));
        assertEquals("/id", instance.getPathname(new Identifier("/../id"), "/"));
        assertEquals("id\\", instance.getPathname(new Identifier("id\\..\\"), "\\"));
        assertEquals("\\id", instance.getPathname(new Identifier("\\..\\id"), "\\"));
    }

    @Test
    public void testGetPathnameWithScriptLookupStrategy()
            throws IOException {
        Configuration config = Application.getConfiguration();
        config.setProperty(FilesystemResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");

        // valid, present script
        config.setProperty("delegate_script",
                TestUtil.getFixture("lookup.rb").getAbsolutePath());
        assertEquals("/bla/" + IDENTIFIER,
                instance.getPathname(IDENTIFIER, File.separator));

        // missing script
        try {
            config.setProperty("delegate_script",
                    TestUtil.getFixture("bogus.rb").getAbsolutePath());
            instance.getPathname(IDENTIFIER, File.separator);
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetSourceFormatByDetection() throws IOException {
        assertEquals(SourceFormat.BMP,
                instance.getSourceFormat(new Identifier("images/bmp")));
        assertEquals(SourceFormat.GIF,
                instance.getSourceFormat(new Identifier("images/gif")));
        assertEquals(SourceFormat.JP2,
                instance.getSourceFormat(new Identifier("images/jp2")));
        assertEquals(SourceFormat.JPG,
                instance.getSourceFormat(new Identifier("images/jpg")));
        assertEquals(SourceFormat.PDF,
                instance.getSourceFormat(new Identifier("images/pdf")));
        assertEquals(SourceFormat.PNG,
                instance.getSourceFormat(new Identifier("images/png")));
        assertEquals(SourceFormat.TIF,
                instance.getSourceFormat(new Identifier("images/tif")));
        assertEquals(SourceFormat.UNKNOWN,
                instance.getSourceFormat(new Identifier("images/txt")));
    }

    @Test
    public void testGetSourceFormatByInference() throws IOException {
        assertEquals(SourceFormat.BMP,
                instance.getSourceFormat(new Identifier("bla.bmp")));
        assertEquals(SourceFormat.GIF,
                instance.getSourceFormat(new Identifier("bla.gif")));
        assertEquals(SourceFormat.JP2,
                instance.getSourceFormat(new Identifier("bla.JP2")));
        assertEquals(SourceFormat.PDF,
                instance.getSourceFormat(new Identifier("bla.pdf")));
        assertEquals(SourceFormat.PNG,
                instance.getSourceFormat(new Identifier("bla.png")));
        assertEquals(SourceFormat.TIF,
                instance.getSourceFormat(new Identifier("bla.tif")));
        try {
            assertEquals(SourceFormat.UNKNOWN,
                    instance.getSourceFormat(new Identifier("bla.bogus")));
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

}
