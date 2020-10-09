package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.CropToSquare;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.test.ConcurrentReaderWriter;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.DeletingFileVisitor;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.illinois.library.cantaloupe.cache.FilesystemCache.*;
import static edu.illinois.library.cantaloupe.test.Assert.PathAssert.assertRecursiveFileCount;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class FilesystemCacheTest extends AbstractCacheTest {

    private Path fixturePath;
    private Path infoPath;
    private Path sourceImagePath;
    private Path derivativeImagePath;
    private FilesystemCache instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        fixturePath = Files.createTempDirectory("test").resolve("cache");
        sourceImagePath = fixturePath.resolve("source");
        derivativeImagePath = fixturePath.resolve("image");
        infoPath = fixturePath.resolve("info");

        instance = newInstance();
    }

    @AfterEach
    public void tearDown() throws IOException {
        try {
            Files.walkFileTree(fixturePath, new DeletingFileVisitor());
        } catch (DirectoryNotEmptyException e) {
            // This happens in Windows 7 (maybe other versions?) sometimes; not
            // sure why, but it shouldn't result in a test failure.
            System.err.println(e.getMessage());
        }
    }

    @Override
    FilesystemCache newInstance() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH, 3);
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH, 2);
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                fixturePath.toString());

        return new FilesystemCache();
    }

    private void createEmptyFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.createFile(path);
    }

    private void writeStringToFile(Path path,
                                   String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, contents.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testHashedPathFragment() {
        // depth = 2, length = 3
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH, 2);
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH, 3);
        assertEquals(
                String.format("083%s2c1", File.separator),
                FilesystemCache.hashedPathFragment("cats"));

        // depth = 0
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH, 0);
        assertEquals("", hashedPathFragment("cats"));
    }

    @Test
    void testDerivativeImageFile() {
        String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");

        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(
                        new ScaleByPercent(0.905),
                        new Encode(Format.get("tif")))
                .build();

        final Path expected = Paths.get(
                pathname,
                "image",
                hashedPathFragment(identifier.toString()),
                ops.toFilename());
        assertEquals(expected, derivativeImageFile(ops));
    }

    @Test
    void testDerivativeImageTempFile() {
        String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);

        Identifier identifier    = new Identifier("cats_~!@#$%^&*()");
        Crop crop                = new CropToSquare();
        Scale scale              = new ScaleByPercent(0.905);
        Rotate rotate            = new Rotate(10);
        ColorTransform transform = ColorTransform.BITONAL;
        Encode encode            = new Encode(Format.get("tif"));

        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(crop, scale, rotate, transform, encode)
                .build();

        final Path expected = Paths.get(
                pathname,
                "image",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                ops.toFilename() + FilesystemCache.tempFileSuffix());
        assertEquals(expected, FilesystemCache.derivativeImageTempFile(ops));
    }

    @Test
    void testInfoFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "info",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtils.md5(identifier.toString()) + ".json");
        assertEquals(expected, infoFile(identifier));
    }

    @Test
    void testInfoTempFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "info",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtils.md5(identifier.toString()) + ".json"
                        + FilesystemCache.tempFileSuffix());
        assertEquals(expected, infoTempFile(identifier));
    }

    @Test
    void testSourceImageFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "source",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtils.md5(identifier.toString()));
        assertEquals(expected, sourceImageFile(identifier));
    }

    @Test
    void testSourceImageTempFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "source",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtils.md5(identifier.toString())
                        + FilesystemCache.tempFileSuffix());
        assertEquals(expected, sourceImageTempFile(identifier));
    }

    @Test
    void testTempFileSuffix() {
        assertEquals("_" + Thread.currentThread().getName() + ".tmp",
                FilesystemCache.tempFileSuffix());
    }

    /* cleanUp() */

    @Test
    void testCleanUpDoesNotDeleteValidFiles() throws Exception {
        OperationList ops = new OperationList(new Identifier("cats"));

        // create a new source image file
        Path sourceImageFile = sourceImageFile(ops.getIdentifier());
        writeStringToFile(sourceImageFile, "not empty");

        // create a new derivative image file
        Path derivativeImageFile = derivativeImageFile(ops);
        Files.createDirectories(derivativeImageFile.getParent());
        writeStringToFile(derivativeImageFile, "not empty");

        // create a new info file
        Path infoFile = infoFile(ops.getIdentifier());
        Files.createDirectories(infoFile.getParent());
        writeStringToFile(infoFile, "not empty");

        // create some temp files
        Path sourceImageTempFile = sourceImageTempFile(ops.getIdentifier());
        writeStringToFile(sourceImageTempFile, "not empty");

        Path derivativeImageTempFile = derivativeImageTempFile(ops);
        writeStringToFile(derivativeImageTempFile, "not empty");

        Path infoTempFile = infoTempFile(ops.getIdentifier());
        writeStringToFile(infoTempFile, "not empty");

        // create some empty files
        Path root = FilesystemCache.rootSourceImagePath();
        Path subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        root = FilesystemCache.rootDerivativeImagePath();
        subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        root = FilesystemCache.rootInfoPath();
        subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        instance.setMinCleanableAge(10000);
        instance.cleanUp();

        assertRecursiveFileCount(fixturePath, 12);
    }

    @Test
    void testCleanUpDeletesInvalidFiles() throws Exception {
        OperationList ops = new OperationList(new Identifier("cats"));

        // create a new source image file
        Path sourceImageFile = sourceImageFile(ops.getIdentifier());
        writeStringToFile(sourceImageFile, "not empty");

        // create a new derivative image file
        Path derivativeImageFile = derivativeImageFile(ops);
        writeStringToFile(derivativeImageFile, "not empty");

        // create a new info file
        Path infoFile = infoFile(ops.getIdentifier());
        writeStringToFile(infoFile, "not empty");

        // create some temp files
        Path sourceImageTempFile = sourceImageTempFile(ops.getIdentifier());
        writeStringToFile(sourceImageTempFile, "not empty");

        Path derivativeImageTempFile = derivativeImageTempFile(ops);
        writeStringToFile(derivativeImageTempFile, "not empty");

        Path infoTempFile = infoTempFile(ops.getIdentifier());
        writeStringToFile(infoTempFile, "not empty");

        // create some empty files
        Path root = FilesystemCache.rootSourceImagePath();
        Path subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        root = FilesystemCache.rootDerivativeImagePath();
        subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        root = FilesystemCache.rootInfoPath();
        subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        instance.setMinCleanableAge(10);

        Thread.sleep(1000);

        instance.cleanUp();

        assertRecursiveFileCount(fixturePath, 3);
    }

    @Test
    void testGetDerivativeImageFiles() throws Exception {
        Identifier identifier = new Identifier("dogs");
        OperationList ops = new OperationList(identifier);

        Path imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);

        ops.add(new Rotate(15));
        imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);

        ops.add(ColorTransform.GRAY);
        imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);

        assertEquals(3, instance.getDerivativeImageFiles(identifier).size());
    }

    /* getSourceImageFile(Identifier) */

    @Test
    void testGetSourceImageFileWithZeroTTL() throws Exception {
        Configuration.getInstance().setProperty(Key.SOURCE_CACHE_TTL, 0);

        Identifier identifier = new Identifier("cats");
        assertFalse(instance.getSourceImageFile(identifier).isPresent());

        Path imageFile = sourceImageFile(identifier);
        Files.createDirectories(imageFile.getParent());
        Files.createFile(imageFile);
        assertTrue(instance.getSourceImageFile(identifier).isPresent());
    }

    @Test
    void testGetSourceImageFileWithNonzeroTTL() throws Exception {
        Configuration.getInstance().setProperty(Key.SOURCE_CACHE_TTL, 1);

        Identifier identifier = new Identifier("cats");
        Path cacheFile = sourceImageFile(identifier);
        Files.createDirectories(cacheFile.getParent());
        Files.createFile(cacheFile);
        assertNotNull(instance.getSourceImageFile(identifier));

        Thread.sleep(1100);

        assertFalse(instance.getSourceImageFile(identifier).isPresent());

        Thread.sleep(1000);

        assertFalse(Files.exists(cacheFile));
    }

    @Test
    void testGetSourceImageFileConcurrently() throws Exception {
        assumeFalse(SystemUtils.IS_OS_WINDOWS); // TODO: this fails in Windows CI with a flurry of AccessDeniedExceptions
        final Identifier identifier = new Identifier("monkeys");

        new ConcurrentReaderWriter(() -> {
            try (OutputStream os =
                         instance.newSourceImageOutputStream(identifier)) {
                Files.copy(TestUtil.getImage("jpg"), os);
            }
            return null;
        }, () -> {
            instance.getSourceImageFile(identifier);
            return null;
        }).run();
    }

    @Test
    @Override
    void testNewDerivativeImageInputStreamConcurrently() throws Exception {
        assumeFalse(SystemUtils.IS_OS_WINDOWS); // TODO: this fails in Windows CI with a flurry of AccessDeniedExceptions
        super.testNewDerivativeImageInputStreamConcurrently();
    }

    /* newSourceImageOutputStream(Identifier) */

    @Test
    void testNewSourceImageOutputStream() throws Exception {
        try (OutputStream os = instance.newSourceImageOutputStream(new Identifier("cats"))) {
            assertNotNull(os);
        }
    }

    @Test
    void testNewSourceImageOutputStreamCreatesFolder() throws Exception {
        Files.walkFileTree(sourceImagePath, new DeletingFileVisitor());
        assertFalse(Files.exists(sourceImagePath));

        Identifier identifier = new Identifier("cats");
        try (OutputStream os = instance.newSourceImageOutputStream(identifier)) {
            assertTrue(Files.exists(sourceImagePath));
        }
    }

    @Test
    void testNewSourceImageOutputStreamConcurrently() {
        // Tested in testGetSourceImageFileConcurrently()
    }

    /**
     * Override that also tests the source cache.
     */
    @Override
    @Test
    void testPurge() throws Exception {
        OperationList ops = new OperationList(new Identifier("cats"));

        // create a new source image file
        Path sourceImageFile = sourceImageFile(ops.getIdentifier());
        createEmptyFile(sourceImageFile);

        // create a new derivative image file
        Path derivativeImageFile = derivativeImageFile(ops);
        createEmptyFile(derivativeImageFile);

        // create a new info file
        Path infoFile = infoFile(ops.getIdentifier());
        createEmptyFile(infoFile);

        // change the op list
        ops.setIdentifier(new Identifier("dogs"));
        ops.add(new Rotate(15));

        // create a new derivative image file based on the changed op list
        derivativeImageFile = derivativeImageFile(ops);
        createEmptyFile(derivativeImageFile);

        instance.purge();

        assertRecursiveFileCount(fixturePath, 0);
    }

    /**
     * Override that also tests the source cache.
     */
    @Override
    @Test
    void testPurgeWithIdentifier() throws Exception {
        OperationList ops = new OperationList();

        Identifier id1 = new Identifier("dogs");
        ops.setIdentifier(id1);

        // create a new source image
        Path sourceImageFile = sourceImageFile(ops.getIdentifier());
        createEmptyFile(sourceImageFile);

        // create a new derivative image
        Path derivativeImageFile = derivativeImageFile(ops);
        createEmptyFile(derivativeImageFile);

        // create a new info
        Path infoFile = infoFile(ops.getIdentifier());
        createEmptyFile(infoFile);

        Identifier id2 = new Identifier("ferrets");
        ops.setIdentifier(id2);
        ops.add(new Rotate(15));

        sourceImageFile = sourceImageFile(ops.getIdentifier());
        createEmptyFile(sourceImageFile);

        derivativeImageFile = derivativeImageFile(ops);
        createEmptyFile(derivativeImageFile);

        infoFile = infoFile(ops.getIdentifier());
        createEmptyFile(infoFile);

        assertRecursiveFileCount(sourceImagePath, 2);
        assertRecursiveFileCount(derivativeImagePath, 2);
        assertRecursiveFileCount(infoPath, 2);
        instance.purge(id1);
        assertRecursiveFileCount(sourceImagePath, 1);
        assertRecursiveFileCount(derivativeImagePath, 1);
        assertRecursiveFileCount(infoPath, 1);
        instance.purge(id2);
        assertRecursiveFileCount(sourceImagePath, 0);
        assertRecursiveFileCount(derivativeImagePath, 0);
        assertRecursiveFileCount(infoPath, 0);
    }

    /**
     * Override that also tests the source cache.
     */
    @Override
    @Test
    void testPurgeInvalid() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE_TTL, 1);
        config.setProperty(Key.DERIVATIVE_CACHE_TTL, 1);

        // add a source image
        Identifier id = new Identifier("cats");
        OperationList ops = new OperationList(id);
        Path imageFile = sourceImageFile(ops.getIdentifier());
        createEmptyFile(imageFile);

        // add a derivative image
        ops = new OperationList(id);
        imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);

        // add an info
        Path infoFile = infoFile(ops.getIdentifier());
        createEmptyFile(infoFile);

        // wait for them to expire
        Thread.sleep(1500);

        // add a changed derivative
        ops.setIdentifier(new Identifier("dogs"));
        imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);

        instance.purgeInvalid();
        assertRecursiveFileCount(sourceImagePath, 0);
        assertRecursiveFileCount(derivativeImagePath, 1);
        assertRecursiveFileCount(infoPath, 0);
    }

}
