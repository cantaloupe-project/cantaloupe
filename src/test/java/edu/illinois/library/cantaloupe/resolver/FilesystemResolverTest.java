package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
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

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    FilesystemResolver instance;

    private static Configuration newConfiguration() throws IOException {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(FilesystemResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "BasicLookupStrategy");
        config.setProperty(FilesystemResolver.PATH_PREFIX_CONFIG_KEY,
                TestUtil.getFixturePath() + "/images" + File.separator);
        return config;
    }

    @Before
    public void setUp() throws IOException {
        Application.setConfiguration(newConfiguration());
        instance = new FilesystemResolver();
    }

    @Test
    public void testGetStreamSource() {
        // present, readable image
        try {
            assertNotNull(instance.getStreamSource(IDENTIFIER));
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.getStreamSource(new Identifier("bogus"));
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
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        assertEquals("/bla/" + IDENTIFIER,
                instance.getPathname(IDENTIFIER, File.separator));

        // missing script
        try {
            config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_CONFIG_KEY,
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
                instance.getSourceFormat(new Identifier("bmp")));
        assertEquals(SourceFormat.GIF,
                instance.getSourceFormat(new Identifier("gif")));
        assertEquals(SourceFormat.JP2,
                instance.getSourceFormat(new Identifier("jp2")));
        assertEquals(SourceFormat.JPG,
                instance.getSourceFormat(new Identifier("jpg")));
        assertEquals(SourceFormat.PDF,
                instance.getSourceFormat(new Identifier("pdf")));
        assertEquals(SourceFormat.PNG,
                instance.getSourceFormat(new Identifier("png")));
        assertEquals(SourceFormat.TIF,
                instance.getSourceFormat(new Identifier("tif")));
        assertEquals(SourceFormat.UNKNOWN,
                instance.getSourceFormat(new Identifier("txt")));
    }

    @Test
    public void testGetSourceFormatByInference() throws IOException {
        assertEquals(SourceFormat.BMP,
                instance.getSourceFormat(new Identifier("bmp-rgb-64x56x8.bmp")));
        assertEquals(SourceFormat.GIF,
                instance.getSourceFormat(new Identifier("gif-rgb-64x56x8.gif")));
        assertEquals(SourceFormat.JP2,
                instance.getSourceFormat(new Identifier("jp2-rgb-64x56x8-monotiled-lossy.jp2")));
        assertEquals(SourceFormat.JPG,
                instance.getSourceFormat(new Identifier("jpg-rgb-64x56x8-baseline.jpg")));
        assertEquals(SourceFormat.PDF,
                instance.getSourceFormat(new Identifier("pdf.pdf")));
        assertEquals(SourceFormat.PNG,
                instance.getSourceFormat(new Identifier("png-rgb-64x56x8.png")));
        assertEquals(SourceFormat.TIF,
                instance.getSourceFormat(new Identifier("tif-rgb-64x56x8-striped-jpeg.tif")));
    }

    @Test
    public void testGetSourceFormatThrowsExceptionWhenResourceIsMissing()
            throws IOException {
        try {
            instance.getSourceFormat(new Identifier("bogus"));
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
        try {
            instance.getSourceFormat(new Identifier("bla.jpg"));
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

}
