package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.config.Configuration;
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

        Configuration config = Configuration.getInstance();
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
    public void newStreamSourceWithPresentReadableFile() throws Exception {
        assertNotNull(instance.newStreamSource());
    }

    @Test(expected = AccessDeniedException.class)
    public void newStreamSourceWithPresentUnreadableFile() throws Exception {
        File file = new File(instance.getPathname());
        try {
            file.setReadable(false);
            instance.newStreamSource();
            fail("Expected exception");
        } finally {
            file.setReadable(true);
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void newStreamSourceWithMissingFile() throws Exception {
        instance.setIdentifier(new Identifier("bogus"));
        instance.newStreamSource();
    }

    @Test
    public void getFileWithPresentReadableFile() throws Exception {
        assertNotNull(instance.getFile());
    }

    @Test(expected = AccessDeniedException.class)
    public void getFileWithPresentUnreadableFile() throws Exception {
        File file = new File(instance.getPathname());
        try {
            file.setReadable(false);
            instance.getFile();
            fail("Expected exception");
        } finally {
            file.setReadable(true);
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void getFileWithMissingFile() throws Exception {
        instance.setIdentifier(new Identifier("bogus"));
        instance.getFile();
    }

    // getPathname(Identifier)

    @Test
    public void getPathnameWithBasicLookupStrategyWithPrefix()
            throws IOException {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");

        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("/prefix/id", instance.getPathname());
    }

    @Test
    public void getPathnameWithBasicLookupStrategyWithSuffix()
            throws IOException {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");

        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "/suffix");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("/prefix/id/suffix", instance.getPathname());
    }

    @Test
    public void getPathnameWithBasicLookupStrategyWithoutPrefixOrSuffix()
            throws IOException {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");

        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "");
        instance.setIdentifier(new Identifier("id"));
        assertEquals("id", instance.getPathname());
    }

    @Test
    public void getPathnameSanitization() throws IOException {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");

        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "/suffix");
        instance.setIdentifier(new Identifier("id/../"));
        assertEquals("/prefix/id//suffix", instance.getPathname());
        instance.setIdentifier(new Identifier("/../id"));
        assertEquals("/prefix//id/suffix", instance.getPathname());
        instance.setIdentifier(new Identifier("id\\..\\"));
        assertEquals("/prefix/id\\/suffix", instance.getPathname());
        instance.setIdentifier(new Identifier("\\..\\id"));
        assertEquals("/prefix/\\id/suffix", instance.getPathname());
        instance.setIdentifier(new Identifier("/id/../cats\\..\\dogs/../..\\foxes/.\\...\\/....\\.."));
        assertEquals("/prefix//id/cats\\dogs\\foxes/suffix", instance.getPathname());
    }

    @Test
    public void getPathnameWithScriptLookupStrategy() throws IOException {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").getAbsolutePath());
        assertEquals("/bla/" + IDENTIFIER, instance.getPathname());
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

    @Test(expected = FileNotFoundException.class)
    public void getSourceFormatThrowsExceptionWhenResourceIsMissing()
            throws IOException {
        instance.setIdentifier(new Identifier("bogus"));
        instance.getSourceFormat();
    }

}
