package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

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
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                TestUtil.getFixturePath() + "/images" + File.separator);
    }

    @Override
    void useScriptLookupStrategy() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());
        config.setProperty(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY,
                "ScriptLookupStrategy");
    }

    /* checkAccess() */

    @Test
    void testCheckAccessUsingBasicLookupStrategyWithPresentUnreadableFile()
            throws Exception {
        Path path = instance.getFile();
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
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
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
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);

        Path path = instance.getFile();
        try {
            assumeTrue(path.toFile().setReadable(false));
            Files.setPosixFilePermissions(path, Collections.emptySet());
            assertThrows(AccessDeniedException.class, instance::checkAccess);
        } finally {
            path.toFile().setReadable(true);
        }
    }

    @Test
    void testCheckAccessUsingScriptLookupStrategyWithMissingFile() {
        useScriptLookupStrategy();

        Identifier identifier = new Identifier("missing");
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(identifier);
        instance.setDelegateProxy(proxy);
        instance.setIdentifier(identifier);
        assertThrows(NoSuchFileException.class, instance::checkAccess);
    }

    /* getFormatIterator() */

    @Test
    void testGetFormatIteratorHasNext() {
        instance.setIdentifier(IDENTIFIER);

        FilesystemSource.FormatIterator<Format> it = instance.getFormatIterator();
        assertTrue(it.hasNext());
        it.next(); // object key
        assertTrue(it.hasNext());
        it.next(); // identifier extension
        assertTrue(it.hasNext());
        it.next(); // magic bytes
        assertFalse(it.hasNext());
    }

    @Test
    void testGetFormatIteratorNext() {
        instance.setIdentifier(new Identifier("jpg-incorrect-extension.png"));

        FilesystemSource.FormatIterator<Format> it =
                instance.getFormatIterator();
        assertEquals(Format.get("png"), it.next()); // object key
        assertEquals(Format.get("png"), it.next()); // identifier extension
        assertEquals(Format.get("jpg"), it.next()); // magic bytes
        assertThrows(NoSuchElementException.class, it::next);
    }

    /* getFile() */

    @Test
    void testGetFile() throws Exception {
        assertNotNull(instance.getFile());
    }

    @Test
    void testgetFileUsingBasicLookupStrategyWithPrefix() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "");

        instance.setIdentifier(new Identifier("id"));
        assertEquals(File.separator + "prefix" + File.separator + "id",
                instance.getFile().toString());
    }

    @Test
    void testgetFileUsingBasicLookupStrategyWithSuffix() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "/suffix");

        instance.setIdentifier(new Identifier("id"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getFile().toString());
    }

    @Test
    void testgetFileUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("id", instance.getFile().toString());
    }

    /**
     * Tests that all instances of ../, ..\, /.., and \.. are removed
     * to disallow ascending up the directory tree.
     */
    @Test
    void testgetFileSanitization() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "/suffix");

        instance.setIdentifier(new Identifier("id/../"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getFile().toString());

        instance.setIdentifier(new Identifier("/../id"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getFile().toString());

        instance.setIdentifier(new Identifier("id\\..\\"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getFile().toString());

        instance.setIdentifier(new Identifier("\\..\\id"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getFile().toString());

        // test injection-safety
        instance.setIdentifier(new Identifier("/id/../cats\\..\\dogs/../..\\foxes/.\\...\\/....\\.."));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" +
                        File.separator + "cats" + File.separator + "dogs" +
                        File.separator + "foxes" + File.separator + "suffix",
                instance.getFile().toString());
    }

    @Test
    void testgetFileWithScriptLookupStrategy() throws Exception {
        useScriptLookupStrategy();

        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(IDENTIFIER);
        instance.setDelegateProxy(proxy);

        assertEquals(IDENTIFIER.toString(), instance.getFile().toString());
    }

    /* newStreamFactory() */

    @Test
    void testNewStreamFactoryWithPresentReadableFile() throws Exception {
        assertNotNull(instance.newStreamFactory());
    }

}
