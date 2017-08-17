package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
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
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX,
                TestUtil.getFixturePath() + "/images" + File.separator);

        instance = new FilesystemResolver();
        instance.setIdentifier(IDENTIFIER);
        instance.setContext(new RequestContext());
    }

    @Test
    public void newStreamSourceWithPresentReadableFile() {
        try {
            assertNotNull(instance.newStreamSource());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void newStreamSourceWithPresentUnreadableFile() throws Exception {
        File file = new File(instance.getPathname(File.separator));
        try {
            file.setReadable(false);
            instance.newStreamSource();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            fail();
        } catch (AccessDeniedException e) {
            // pass
        } finally {
            file.setReadable(true);
        }
    }

    @Test
    public void newStreamSourceWithMissingFile() {
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
    public void getFileWithPresentReadableFile() throws Exception {
        try {
            assertNotNull(instance.getFile());
        } catch (FileNotFoundException e) {
            fail();
        }
    }

    @Test
    public void getFileWithPresentUnreadableFile() throws Exception {
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
    }

    @Test
    public void getFileWithMissingFile() throws Exception {
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
    public void getPathnameWithBasicLookupStrategy() throws IOException {
        Configuration config = ConfigurationFactory.getInstance();

        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        // with prefix
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "/prefix/");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("/prefix/id", instance.getPathname(File.separator));
        // with suffix
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "/suffix");
        assertEquals("/prefix/id/suffix", instance.getPathname(File.separator));
        // without prefix or suffix
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "");
        assertEquals("id", instance.getPathname(File.separator));
        // test sanitization
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "");
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
    public void getPathnameWithScriptLookupStrategy() throws IOException {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        assertEquals("/bla/" + IDENTIFIER,
                instance.getPathname(File.separator));
    }

    @Test
    public void getSourceFormatByDetection() throws IOException {
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
    public void getSourceFormatByInference() throws IOException {
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
    public void getSourceFormatThrowsExceptionWhenResourceIsMissing()
            throws IOException {
        try {
            instance.setIdentifier(new Identifier("bogus"));
            instance.getSourceFormat();
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

}
