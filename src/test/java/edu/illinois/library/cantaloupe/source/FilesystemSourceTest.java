package edu.illinois.library.cantaloupe.source;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.Assume.assumeTrue;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;

public class FilesystemSourceTest extends AbstractSourceTest {

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private FilesystemSource instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Override
    void destroyEndpoint() {
        // nothing to do
    }

    @Override
    void initializeEndpoint() {
        // nothing to do
    }

    @Override
    FilesystemSource newInstance() {
        FilesystemSource instance = new FilesystemSource();
        instance.setIdentifier(IDENTIFIER);
        return instance;
    }

    @Override
    void useBasicLookupStrategy() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
            config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                    TestUtil.getFixture("delegates.rb").toString());
            config.setProperty(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                    TestUtil.getFixturePath() + "/images" + File.separator);
        } catch (IOException e) {
            fail();
        }
    }

    @Override
    void useScriptLookupStrategy() {
        try {
            Configuration config = Configuration.getInstance();
            config.setProperty(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY,
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
    void testCheckAccessUsingBasicLookupStrategyWithPresentUnreadableFile()
            throws Exception {
        Path path = instance.getPath();
        try {
            assumeTrue(path.toFile().setReadable(false));
            assertThrows(AccessDeniedException.class, instance::checkAccess);
        } finally {
            path.toFile().setReadable(true);
        }
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithPresentReadableFile()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier(
                TestUtil.getImage(IDENTIFIER.toString()).toString());
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);

        instance.setIdentifier(identifier);
        instance.checkAccess();
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithPresentUnreadableFile()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier(
                TestUtil.getImage(IDENTIFIER.toString()).toString());
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        Path path = instance.getPath();
        try {
            assumeTrue(path.toFile().setReadable(false));
            Files.setPosixFilePermissions(path, Collections.emptySet());
            assertThrows(AccessDeniedException.class, instance::checkAccess);
        } finally {
            path.toFile().setReadable(true);
        }
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithMissingFile()
            throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("missing");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);

        instance.setIdentifier(identifier);
        assertThrows(NoSuchFileException.class, instance::checkAccess);
    }

    /* getPath() */

    @Test
    void testGetPath() throws Exception {
        assertNotNull(instance.getPath());
    }

    @Test
    void testGetPathUsingBasicLookupStrategyWithPrefix() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "");

        instance.setIdentifier(new Identifier("id"));
        assertEquals(File.separator + "prefix" + File.separator + "id",
                instance.getPath().toString());
    }

    @Test
    void testGetPathUsingBasicLookupStrategyWithSuffix() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "/suffix");

        instance.setIdentifier(new Identifier("id"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getPath().toString());
    }

    @Test
    void testGetPathUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("id", instance.getPath().toString());
    }

    /**
     * Tests that all instances of ../, ..\, /.., and \.. are removed
     * to disallow ascending up the directory tree.
     */
    @Test
    void testGetPathSanitization() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "/suffix");

        instance.setIdentifier(new Identifier("id/../"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getPath().toString());

        instance.setIdentifier(new Identifier("/../id"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getPath().toString());

        instance.setIdentifier(new Identifier("id\\..\\"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getPath().toString());

        instance.setIdentifier(new Identifier("\\..\\id"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getPath().toString());

        // test injection-safety
        instance.setIdentifier(new Identifier("/id/../cats\\..\\dogs/../..\\foxes/.\\...\\/....\\.."));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" +
                        File.separator + "cats" + File.separator + "dogs" +
                        File.separator + "foxes" + File.separator + "suffix",
                instance.getPath().toString());
    }

    @Test
    void testGetPathWithScriptLookupStrategy() throws Exception {
        useScriptLookupStrategy();

        RequestContext context = new RequestContext();
        context.setIdentifier(IDENTIFIER);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);

        assertEquals(IDENTIFIER.toString(), instance.getPath().toString());
    }

    /* getFormat() */

    @Test
    void testGetFormatWithFilenameExtension() throws Exception {
        instance.setIdentifier(new Identifier("bmp-rgb-64x56x8.bmp"));
        assertEquals(Format.BMP, instance.getFormat());

        instance.setIdentifier(new Identifier("gif-rgb-64x56x8.gif"));
        assertEquals(Format.GIF, instance.getFormat());

        instance.setIdentifier(new Identifier("jp2-5res-rgb-64x56x8-monotiled-lossy.jp2"));
        assertEquals(Format.JP2, instance.getFormat());

        instance.setIdentifier(new Identifier("jpg-rgb-64x56x8-baseline.jpg"));
        assertEquals(Format.JPG, instance.getFormat());

        instance.setIdentifier(new Identifier("pdf.pdf"));
        assertEquals(Format.PDF, instance.getFormat());

        instance.setIdentifier(new Identifier("png-rgb-64x56x8.png"));
        assertEquals(Format.PNG, instance.getFormat());

        instance.setIdentifier(new Identifier("tif-rgb-1res-64x56x8-striped-jpeg.tif"));
        assertEquals(Format.TIF, instance.getFormat());
    }

    @Test
    void testGetFormatWithIdentifierExtension() throws Exception {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("FilesystemSourceTest-" +
                "extension-in-identifier-but-not-filename.jpg");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        assertEquals(Format.JPG, instance.getFormat());
    }

    @Test
    void testGetFormatByDetection() throws Exception {
        instance.setIdentifier(new Identifier("bmp"));
        assertEquals(Format.BMP, instance.getFormat());

        instance.setIdentifier(new Identifier("gif"));
        assertEquals(Format.GIF, instance.getFormat());

        instance.setIdentifier(new Identifier("jp2"));
        assertEquals(Format.JP2, instance.getFormat());

        instance.setIdentifier(new Identifier("jpg"));
        assertEquals(Format.JPG, instance.getFormat());

        instance.setIdentifier(new Identifier("pdf"));
        assertEquals(Format.PDF, instance.getFormat());

        instance.setIdentifier(new Identifier("png"));
        assertEquals(Format.PNG, instance.getFormat());

        instance.setIdentifier(new Identifier("tif"));
        assertEquals(Format.TIF, instance.getFormat());

        instance.setIdentifier(new Identifier("txt"));
        assertEquals(Format.UNKNOWN, instance.getFormat());
    }

    /* newStreamFactory() */

    @Test
    void testNewStreamFactoryWithPresentReadableFile() throws Exception {
        assertNotNull(instance.newStreamFactory());
    }

}
