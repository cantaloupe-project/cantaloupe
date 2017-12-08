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
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Set;

public class FilesystemResolverTest extends BaseTest {

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private FilesystemResolver instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        useBasicLookupStrategy();

        instance = new FilesystemResolver();
        instance.setIdentifier(IDENTIFIER);
        instance.setContext(new RequestContext());
    }

    private void useBasicLookupStrategy() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
            config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                    TestUtil.getFixture("delegates.rb").toString());
            config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX,
                    TestUtil.getFixturePath() + "/images" + File.separator);
        } catch (IOException e) {
            fail();
        }
    }

    private void useScriptLookupStrategy() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                    "ScriptLookupStrategy");
            config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
            config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                    TestUtil.getFixture("delegates.rb").toString());
        } catch (IOException e) {
            fail();
        }
    }

    /* checkAccess() */

    @Test
    public void testCheckAccessUsingBasicLookupStrategyWithPresentReadableFile()
            throws Exception {
        instance.checkAccess();
    }

    @Test(expected = AccessDeniedException.class)
    public void testCheckAccessUsingBasicLookupStrategyWithPresentUnreadableFile()
            throws Exception {
        Path path = instance.getPath();
        Set<PosixFilePermission> initialPermissions =
                Files.getPosixFilePermissions(path);
        try {
            Files.setPosixFilePermissions(path, Collections.emptySet());
            instance.checkAccess();
        } finally {
            Files.setPosixFilePermissions(path, initialPermissions);
        }
    }

    @Test(expected = NoSuchFileException.class)
    public void testCheckAccessUsingBasicLookupStrategyWithMissingFile()
            throws Exception {
        instance.setIdentifier(new Identifier("bogus"));
        instance.checkAccess();
    }

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithPresentReadableFile()
            throws Exception {
        useScriptLookupStrategy();
        instance.setIdentifier(
                new Identifier(TestUtil.getImage(IDENTIFIER.toString()).toString()));
        instance.checkAccess();
    }

    @Test(expected = AccessDeniedException.class)
    public void testCheckAccessUsingScriptLookupStrategyWithPresentUnreadableFile()
            throws Exception {
        useScriptLookupStrategy();

        instance.setIdentifier(
                new Identifier(TestUtil.getImage(IDENTIFIER.toString()).toString()));
        Path path = instance.getPath();
        Set<PosixFilePermission> initialPermissions =
                Files.getPosixFilePermissions(path);
        try {
            Files.setPosixFilePermissions(path, Collections.emptySet());
            instance.checkAccess();
        } finally {
            Files.setPosixFilePermissions(path, initialPermissions);
        }
    }

    @Test(expected = NoSuchFileException.class)
    public void testCheckAccessUsingScriptLookupStrategyWithMissingFile()
            throws Exception {
        useScriptLookupStrategy();
        instance.setIdentifier(new Identifier("bogus"));
        instance.checkAccess();
    }

    /* getPath() */

    @Test
    public void testGetPath() throws Exception {
        assertNotNull(instance.getPath());
    }

    @Test
    public void testGetPathUsingBasicLookupStrategyWithPrefix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("/prefix/id", instance.getPath().toString());
    }

    @Test
    public void testGetPathUsingBasicLookupStrategyWithSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "/suffix");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("/prefix/id/suffix", instance.getPath().toString());
    }

    @Test
    public void testGetPathUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("id", instance.getPath().toString());
    }

    /**
     * Tests that all instances of ../, ..\, /.., and \.. are removed
     * to disallow ascending up the directory tree.
     */
    @Test
    public void testGetPathSanitization() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_SUFFIX, "/suffix");

        instance.setIdentifier(new Identifier("id/../"));
        assertEquals("/prefix/id/suffix", instance.getPath().toString());
        instance.setIdentifier(new Identifier("/../id"));
        assertEquals("/prefix/id/suffix", instance.getPath().toString());
        instance.setIdentifier(new Identifier("id\\..\\"));
        assertEquals("/prefix/id\\/suffix", instance.getPath().toString());
        instance.setIdentifier(new Identifier("\\..\\id"));
        assertEquals("/prefix/\\id/suffix", instance.getPath().toString());
        // test injection-safety
        instance.setIdentifier(new Identifier("/id/../cats\\..\\dogs/../..\\foxes/.\\...\\/....\\.."));
        assertEquals("/prefix/id/cats\\dogs\\foxes/suffix",
                instance.getPath().toString());
    }

    @Test
    public void testGetPathWithScriptLookupStrategy() throws Exception {
        useScriptLookupStrategy();
        assertEquals("/bla/" + IDENTIFIER, instance.getPath().toString());
    }

    /* getSourceFormat() */

    @Test
    public void testGetSourceFormatWithPresentReadableFile() throws Exception {
        assertEquals(Format.JPG, instance.getSourceFormat());
    }

    @Test
    public void testGetSourceFormatByDetection() throws Exception {
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
    public void testGetSourceFormatByInference() throws Exception {
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

    /* newStreamSource() */

    @Test
    public void testNewStreamSourceWithPresentReadableFile() throws Exception {
        assertNotNull(instance.newStreamSource());
    }

}
