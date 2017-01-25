package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

public class FilesystemResolverTest extends BaseTest {

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private FilesystemResolver instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(FilesystemResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "BasicLookupStrategy");
        config.setProperty(FilesystemResolver.PATH_PREFIX_CONFIG_KEY,
                TestUtil.getFixturePath() + "/images" + File.separator);

        instance = new FilesystemResolver();
        instance.setIdentifier(IDENTIFIER);
    }

    @Test
    public void testNewStreamSource() {
        // present, readable image
        try {
            assertNotNull(instance.newStreamSource());
        } catch (IOException e) {
            fail();
        }
        // missing image
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
    public void testGetFile() throws Exception {
        // present, readable file
        try {
            assertNotNull(instance.getFile());
        } catch (FileNotFoundException e) {
            fail();
        }

        // present, unreadable file
        File file = new File(instance.getPathname(File.separator));
        try {
            file.setReadable(false);
            instance.getFile();
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
            instance.setIdentifier(new Identifier("bogus"));
            instance.getFile();
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
        Configuration config = ConfigurationFactory.getInstance();

        config.setProperty(FilesystemResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "BasicLookupStrategy");
        // with prefix
        config.setProperty(FilesystemResolver.PATH_PREFIX_CONFIG_KEY, "/prefix/");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("/prefix/id", instance.getPathname(File.separator));
        // with suffix
        config.setProperty(FilesystemResolver.PATH_SUFFIX_CONFIG_KEY, "/suffix");
        assertEquals("/prefix/id/suffix", instance.getPathname(File.separator));
        // without prefix or suffix
        config.setProperty(FilesystemResolver.PATH_PREFIX_CONFIG_KEY, "");
        config.setProperty(FilesystemResolver.PATH_SUFFIX_CONFIG_KEY, "");
        assertEquals("id", instance.getPathname(File.separator));
        // test sanitization
        config.setProperty(FilesystemResolver.PATH_PREFIX_CONFIG_KEY, "");
        config.setProperty(FilesystemResolver.PATH_SUFFIX_CONFIG_KEY, "");
        instance.setIdentifier(new Identifier("id/../"));
        assertEquals("id/", instance.getPathname("/"));
        instance.setIdentifier(new Identifier("/../id"));
        assertEquals("/id", instance.getPathname("/"));
        instance.setIdentifier(new Identifier("id\\..\\"));
        assertEquals("id\\", instance.getPathname("\\"));
        instance.setIdentifier(new Identifier("\\..\\id"));
        assertEquals("\\id", instance.getPathname("\\"));
    }

    @Test
    public void testGetPathnameWithScriptLookupStrategy()
            throws IOException {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(FilesystemResolver.LOOKUP_STRATEGY_CONFIG_KEY,
                "ScriptLookupStrategy");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_ENABLED_CONFIG_KEY,
                "true");
        config.setProperty(ScriptEngineFactory.DELEGATE_SCRIPT_PATHNAME_CONFIG_KEY,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        assertEquals("/bla/" + IDENTIFIER,
                instance.getPathname(File.separator));
    }

    @Test
    public void testGetSourceFormatByDetection() throws IOException {
        instance.setIdentifier(new Identifier("bmp"));
        assertEquals(Format.BMP, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("gif"));
        assertEquals(Format.GIF, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("jp2"));
        assertEquals(Format.JP2, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("jpg"));
        assertEquals(Format.JPG, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("pdf"));
        assertEquals(Format.PDF, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("png"));
        assertEquals(Format.PNG, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("tif"));
        assertEquals(Format.TIF, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("txt"));
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatByInference() throws IOException {
        instance.setIdentifier(new Identifier("bmp-rgb-64x56x8.bmp"));
        assertEquals(Format.BMP, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("gif-rgb-64x56x8.gif"));
        assertEquals(Format.GIF, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("jp2-rgb-64x56x8-monotiled-lossy.jp2"));
        assertEquals(Format.JP2, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("jpg-rgb-64x56x8-baseline.jpg"));
        assertEquals(Format.JPG, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("pdf.pdf"));
        assertEquals(Format.PDF, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("png-rgb-64x56x8.png"));
        assertEquals(Format.PNG, instance.getSourceFormat());

        instance.setIdentifier(new Identifier("tif-rgb-monores-64x56x8-striped-jpeg.tif"));
        assertEquals(Format.TIF, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatThrowsExceptionWhenResourceIsMissing()
            throws IOException {
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
        try {
            instance.setIdentifier(new Identifier("bla.jpg"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

}
