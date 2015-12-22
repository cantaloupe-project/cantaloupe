package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

public class FilesystemResolverTest {

    private static final Identifier IDENTIFIER = new Identifier("escher_lego.jpg");

    FilesystemResolver instance;

    private static Configuration newConfiguration() throws IOException {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty(FilesystemResolver.LOOKUP_SCRIPT_CONFIG_KEY,
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
    public void testExecuteLookupScript() throws Exception {
        // valid script
        File script = TestUtil.getFixture("lookup.rb");
        String result = (String) instance.executeLookupScript(IDENTIFIER, script);
        assertEquals("/bla/" + IDENTIFIER, result);

        // unsupported script
        try {
            script = TestUtil.getFixture("lookup.js");
            instance.executeLookupScript(IDENTIFIER, script);
            fail("Expected exception");
        } catch (ScriptException e) {
            assertEquals("Unsupported script type: js", e.getMessage());
        }

        // script returns nil
        script = TestUtil.getFixture("lookup_nil.rb");
        try {
            instance.executeLookupScript(IDENTIFIER, script);
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetChannel() {
        // present, readable image
        try {
            assertNotNull(instance.getChannel(IDENTIFIER));
        } catch (IOException e) {
            fail();
        }
        // missing image
        try {
            instance.getChannel(new Identifier("bogus"));
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
        // with path separator
        String separator = "CATS";
        config.setProperty(FilesystemResolver.PATH_SEPARATOR_CONFIG_KEY, separator);
        assertEquals("1" + File.separator + "2" + File.separator + "3",
                instance.getPathname(new Identifier("1" + separator + "2" + separator + "3"), File.separator));
        // test sanitization
        config.setProperty(FilesystemResolver.PATH_PREFIX_CONFIG_KEY, "");
        config.setProperty(FilesystemResolver.PATH_SUFFIX_CONFIG_KEY, "");
        assertEquals("id/", instance.getPathname(new Identifier("id/../"), "/"));
        assertEquals("/id", instance.getPathname(new Identifier("/../id"), "/"));
        assertEquals("id\\", instance.getPathname(new Identifier("id\\..\\"), "\\"));
        assertEquals("\\id", instance.getPathname(new Identifier("\\..\\id"), "\\"));
    }

    @Test
    public void testGetPathnameWithScriptLookupStrategyAndAbsolutePath()
            throws IOException {
        Configuration config = Application.getConfiguration();
        config.setProperty(FilesystemResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");

        // valid, present script
        config.setProperty(FilesystemResolver.LOOKUP_SCRIPT_CONFIG_KEY,
                TestUtil.getFixture("lookup.rb").getAbsolutePath());
        assertEquals("/bla/" + IDENTIFIER,
                instance.getPathname(IDENTIFIER, File.separator));

        // missing script
        try {
            config.setProperty(FilesystemResolver.LOOKUP_SCRIPT_CONFIG_KEY,
                    TestUtil.getFixture("bogus.rb").getAbsolutePath());
            instance.getPathname(IDENTIFIER, File.separator);
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetPathnameWithScriptLookupStrategyAndRelativePath()
            throws IOException {
        Configuration config = Application.getConfiguration();
        config.setProperty(FilesystemResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");

        // filename of script, located in cwd
        config.setProperty(FilesystemResolver.LOOKUP_SCRIPT_CONFIG_KEY,
                "lookup_test.rb");
        final File tempFile = new File("./lookup_test.rb");
        try {
            FileUtils.copyFile(TestUtil.getFixture("lookup.rb"), tempFile);
            assertEquals("/bla/" + IDENTIFIER,
                    instance.getPathname(IDENTIFIER, File.separator));
        } finally {
            FileUtils.forceDelete(tempFile);
        }
    }

    @Test
    public void testGetPathnameWithScriptLookupStrategyAndPathSeparator()
            throws IOException {
        Configuration config = Application.getConfiguration();
        config.setProperty(FilesystemResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");
        config.setProperty(FilesystemResolver.LOOKUP_SCRIPT_CONFIG_KEY,
                TestUtil.getFixture("lookup.rb").getAbsolutePath());

        String separator = "CATS";
        config.setProperty(FilesystemResolver.PATH_SEPARATOR_CONFIG_KEY, separator);
        final String expected = "/bla/1" + File.separator + "2" + File.separator + "3";
        final String actual = instance.getPathname(
                new Identifier("1" + separator + "2" + separator + "3"),
                File.separator);
        assertEquals(expected, actual);
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
