package edu.illinois.library.cantaloupe.resolver;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.RequestContext;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import edu.illinois.library.cantaloupe.script.DelegateProxyService;
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

public class FilesystemResolverTest extends AbstractResolverTest {

    private static final Identifier IDENTIFIER =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    private FilesystemResolver instance;

    @Before
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
    FilesystemResolver newInstance() {
        FilesystemResolver instance = new FilesystemResolver();
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
            config.setProperty(Key.FILESYSTEMRESOLVER_LOOKUP_STRATEGY,
                    "BasicLookupStrategy");
            config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX,
                    TestUtil.getFixturePath() + "/images" + File.separator);
        } catch (IOException e) {
            fail();
        }
    }

    @Override
    void useScriptLookupStrategy() {
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

    @Test
    public void testCheckAccessUsingScriptLookupStrategyWithPresentReadableFile()
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

    @Test(expected = AccessDeniedException.class)
    public void testCheckAccessUsingScriptLookupStrategyWithPresentUnreadableFile()
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

        Identifier identifier = new Identifier("missing");
        RequestContext context = new RequestContext();
        context.setIdentifier(identifier);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);

        instance.setIdentifier(identifier);
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

        RequestContext context = new RequestContext();
        context.setIdentifier(IDENTIFIER);
        DelegateProxyService service = DelegateProxyService.getInstance();
        DelegateProxy proxy = service.newDelegateProxy(context);
        instance.setDelegateProxy(proxy);

        assertEquals(IDENTIFIER.toString(), instance.getPath().toString());
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
