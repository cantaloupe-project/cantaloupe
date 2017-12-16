package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.ConcurrentReaderWriter;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.DeletingFileVisitor;
import edu.illinois.library.cantaloupe.util.StringUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.illinois.library.cantaloupe.cache.FilesystemCache.*;
import static edu.illinois.library.cantaloupe.test.Assert.PathAssert.assertRecursiveFileCount;
import static org.junit.Assert.*;

public class FilesystemCacheTest extends BaseTest {

    private Path fixturePath;
    private Path sourceImagePath;
    private Path derivativeImagePath;
    private Path infoPath;
    private FilesystemCache instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        fixturePath = Files.createTempDirectory("test").resolve("cache");
        sourceImagePath = fixturePath.resolve("source");
        derivativeImagePath = fixturePath.resolve("image");
        infoPath = fixturePath.resolve("info");

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH, 3);
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH, 2);
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                fixturePath.toString());
        config.setProperty(Key.CACHE_SERVER_TTL, 0);

        instance = new FilesystemCache();
    }

    @After
    public void tearDown() throws IOException {
        Files.walkFileTree(fixturePath, new DeletingFileVisitor());
    }

    private void createEmptyFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.createFile(path);
    }

    private void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    private void writeStringToFile(Path path, String contents)
            throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, contents.getBytes("UTF-8"));
    }

    @Test
    public void testHashedPathFragment() {
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
    public void testDerivativeImageFile() {
        String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.905f);
        Format format = Format.TIF;

        OperationList ops = new OperationList(identifier, format, scale);

        final Path expected = Paths.get(
                pathname,
                "image",
                hashedPathFragment(identifier.toString()),
                ops.toFilename());
        assertEquals(expected, derivativeImageFile(ops));
    }

    @Test
    public void testDerivativeImageTempFile() {
        String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Crop crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.905f);
        Rotate rotate = new Rotate(10);
        ColorTransform transform = ColorTransform.BITONAL;
        Format format = Format.TIF;

        OperationList ops = new OperationList(identifier, format,
                crop, scale, rotate, transform);

        final Path expected = Paths.get(
                pathname,
                "image",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                ops.toFilename() + FilesystemCache.tempFileSuffix());
        assertEquals(expected, FilesystemCache.derivativeImageTempFile(ops));
    }

    @Test
    public void testInfoFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "info",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtil.filesystemSafe(identifier.toString()) + ".json");
        assertEquals(expected, infoFile(identifier));
    }

    @Test
    public void testInfoTempFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "info",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtil.filesystemSafe(identifier.toString()) + ".json"
                        + FilesystemCache.tempFileSuffix());
        assertEquals(expected, infoTempFile(identifier));
    }

    @Test
    public void testSourceImageFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "source",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtil.filesystemSafe(identifier.toString()));
        assertEquals(expected, sourceImageFile(identifier));
    }

    @Test
    public void testSourceImageTempFile() {
        final String pathname = Configuration.getInstance().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "source",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtil.filesystemSafe(identifier.toString())
                        + FilesystemCache.tempFileSuffix());
        assertEquals(expected, sourceImageTempFile(identifier));
    }

    @Test
    public void testTempFileSuffix() {
        assertEquals("_" + Thread.currentThread().getName() + ".tmp",
                FilesystemCache.tempFileSuffix());
    }

    @Test
    public void testCleanUpShouldNotDeleteUnexpiredFiles() throws Exception {
        OperationList ops = TestUtil.newOperationList();

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
    public void testCleanUpShouldDeleteExpiredFiles() throws Exception {
        OperationList ops = TestUtil.newOperationList();

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

        sleep(1000);

        instance.cleanUp();

        assertRecursiveFileCount(fixturePath, 3);
    }

    @Test
    public void testGetDerivativeImageFiles() throws Exception {
        Identifier identifier = new Identifier("dogs");
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(identifier);

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

    @Test
    public void testGetImageInfoWithZeroTTL() throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 0);

        Identifier identifier = new Identifier("test");
        Path file = infoFile(identifier);
        createEmptyFile(file);

        ObjectMapper mapper = new ObjectMapper();
        Info info = new Info(50, 50);
        mapper.writeValue(file.toFile(), info);
        assertEquals(info, instance.getImageInfo(identifier));
    }

    @Test
    public void testGetImageInfoWithNonZeroTTL() throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 1);

        Identifier identifier = new Identifier("test");
        Path file = infoFile(identifier);
        Files.createDirectories(file.getParent());
        Files.createFile(file);

        ObjectMapper mapper = new ObjectMapper();
        Info info = new Info(50, 50);
        mapper.writeValue(file.toFile(), info);

        sleep(1100);
        assertNull(instance.getImageInfo(identifier));
    }

    @Test
    public void testGetImageInfoConcurrently() {
        // This is tested in testPutConcurrently()
    }

    @Test
    public void testGetSourceImageFileWithZeroTTL() throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 0);

        Identifier identifier = new Identifier("cats");
        assertNull(instance.getSourceImageFile(identifier));

        Path imageFile = sourceImageFile(identifier);
        Files.createDirectories(imageFile.getParent());
        Files.createFile(imageFile);
        assertNotNull(instance.getSourceImageFile(identifier));
    }

    @Test
    public void testGetSourceImageFileWithNonzeroTTL() throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 1);

        Identifier identifier = new Identifier("cats");
        Path cacheFile = sourceImageFile(identifier);
        Files.createDirectories(cacheFile.getParent());
        Files.createFile(cacheFile);
        assertNotNull(instance.getSourceImageFile(identifier));

        sleep(1100);

        assertNull(instance.getSourceImageFile(identifier));

        sleep(1000);

        assertFalse(Files.exists(cacheFile));
    }

    @Test
    public void testGetSourceImageFileConcurrently() throws Exception {
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
    public void testNewDerivativeImageInputStreamWithZeroTTL()
            throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 0);

        OperationList ops = TestUtil.newOperationList();
        try (InputStream is = instance.newDerivativeImageInputStream(ops)) {
            assertNull(is);
        }

        Path imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);
        try (InputStream is = instance.newDerivativeImageInputStream(ops)) {
            assertNotNull(is);
        }
    }

    @Test
    public void testNewDerivativeImageInputStreamWithNonzeroTTL()
            throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 1);

        OperationList ops = TestUtil.newOperationList();
        Path cacheFile = derivativeImageFile(ops);
        createEmptyFile(cacheFile);
        try (InputStream is = instance.newDerivativeImageInputStream(ops)) {
            assertNotNull(is);
        }

        sleep(1100);

        try (InputStream is = instance.newDerivativeImageInputStream(ops)) {
            assertNull(is);
        }

        sleep(1000);

        assertFalse(Files.exists(cacheFile));
    }

    @Test
    public void testNewDerivativeImageInputStreamConcurrently()
            throws Exception {
        OperationList ops = TestUtil.newOperationList();

        new ConcurrentReaderWriter(() -> {
            try (OutputStream os =
                         instance.newDerivativeImageOutputStream(ops)) {
                Files.copy(TestUtil.getImage("jpg"), os);
            }
            return null;
        }, () -> {
            try (InputStream is = instance.newDerivativeImageInputStream(ops)) {
                if (is != null) {
                    while (is.read() != -1) {
                        // consume the stream fully
                    }
                }
            }
            return null;
        }).run();
    }

    @Test
    public void testNewDerivativeImageOutputStream() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            assertNotNull(os);
        }
    }

    @Test
    public void testNewDerivativeImageOutputStreamCreatesFolder()
            throws Exception {
        Files.walkFileTree(derivativeImagePath, new DeletingFileVisitor());
        assertFalse(Files.exists(derivativeImagePath));

        OperationList ops = TestUtil.newOperationList();
        try (OutputStream os = instance.newDerivativeImageOutputStream(ops)) {
            assertTrue(Files.exists(derivativeImagePath));
        }
    }

    @Test
    public void testNewDerivativeImageOutputStreamConcurrently() {
        // tested in testNewDerivativeImageInputStreamConcurrently()
    }

    @Test
    public void testNewSourceImageOutputStream() throws Exception {
        try (OutputStream os = instance.newSourceImageOutputStream(new Identifier("cats"))) {
            assertNotNull(os);
        }
    }

    @Test
    public void testNewSourceImageOutputStreamCreatesFolder()
            throws Exception {
        Files.walkFileTree(sourceImagePath, new DeletingFileVisitor());
        assertFalse(Files.exists(sourceImagePath));

        Identifier identifier = new Identifier("cats");
        try (OutputStream os = instance.newSourceImageOutputStream(identifier)) {
            assertTrue(Files.exists(sourceImagePath));
        }
    }

    @Test
    public void testNewSourceImageOutputStreamConcurrently() {
        // Tested in testGetSourceImageFileConcurrently()
    }

    @Test
    public void testPurge() throws Exception {
        OperationList ops = TestUtil.newOperationList();

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

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        OperationList ops = TestUtil.newOperationList();

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

    @Test
    public void testPurgeWithOperationList() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        final Path imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);

        instance.purge(ops);

        assertRecursiveFileCount(derivativeImagePath, 0);
    }

    @Test
    public void testPurgeExpired() throws Exception {
        Configuration.getInstance().setProperty(Key.CACHE_SERVER_TTL, 1);

        Crop crop = new Crop();
        crop.setFull(true);

        // add a source image
        OperationList ops = TestUtil.newOperationList();
        Path imageFile = sourceImageFile(ops.getIdentifier());
        createEmptyFile(imageFile);

        // add a derivative image
        ops = TestUtil.newOperationList();
        imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);

        // add an info
        Path infoFile = infoFile(ops.getIdentifier());
        createEmptyFile(infoFile);

        // wait for them to expire
        sleep(1500);

        // add a changed derivative
        ops.setIdentifier(new Identifier("dogs"));
        imageFile = derivativeImageFile(ops);
        createEmptyFile(imageFile);

        instance.purgeExpired();
        assertRecursiveFileCount(sourceImagePath, 0);
        assertRecursiveFileCount(derivativeImagePath, 1);
        assertRecursiveFileCount(infoPath, 0);
    }

    @Test
    public void testPut() throws Exception {
        Identifier identifier = new Identifier("cats");
        Info info = new Info(52, 42);
        instance.put(identifier, info);
        assertEquals(info, instance.getImageInfo(identifier));
    }

    @Test
    public void testPutConcurrently() throws Exception {
        final Identifier identifier = new Identifier("monkeys");
        final Info info = new Info(52, 42);

        new ConcurrentReaderWriter(() -> {
            instance.put(identifier, info);
            return null;
        }, () -> {
            Info otherInfo = instance.getImageInfo(identifier);
            if (otherInfo != null && !info.equals(otherInfo)) {
                fail();
            }
            return null;
        }).run();
    }

}
