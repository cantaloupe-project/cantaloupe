package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.illinois.library.cantaloupe.cache.FilesystemCache.getHashedStringBasedSubdirectory;
import static edu.illinois.library.cantaloupe.cache.FilesystemCache.rootDerivativeImagePathname;
import static edu.illinois.library.cantaloupe.cache.FilesystemCache.rootInfoPathname;
import static edu.illinois.library.cantaloupe.cache.FilesystemCache.rootSourceImagePathname;
import static org.junit.Assert.*;

public class FilesystemCacheTest extends BaseTest {

    private File fixturePath;
    private File sourceImagePath;
    private File derivativeImagePath;
    private File infoPath;
    private FilesystemCache instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        fixturePath = new File(TestUtil.getTempFolder().getAbsolutePath() + "/cache");
        sourceImagePath = new File(fixturePath.getAbsolutePath() + "/source");
        derivativeImagePath = new File(fixturePath.getAbsolutePath() + "/image");
        infoPath = new File(fixturePath.getAbsolutePath() + "/info");

        if (!sourceImagePath.isDirectory() && !sourceImagePath.mkdirs()) {
            throw new IOException("Failed to create folder: " + sourceImagePath);
        }
        if (!derivativeImagePath.isDirectory() && !derivativeImagePath.mkdirs()) {
            throw new IOException("Failed to create folder: " + derivativeImagePath);
        }
        if (!infoPath.isDirectory() && !infoPath.mkdirs()) {
            throw new IOException("Failed to create folder: " + infoPath);
        }

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(FilesystemCache.DIRECTORY_DEPTH_CONFIG_KEY, 3);
        config.setProperty(FilesystemCache.DIRECTORY_NAME_LENGTH_CONFIG_KEY, 2);
        config.setProperty(FilesystemCache.PATHNAME_CONFIG_KEY,
                fixturePath.toString());
        config.setProperty(Cache.TTL_CONFIG_KEY, 0);

        instance = new FilesystemCache();
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(fixturePath);
    }

    /* getHashedStringBasedSubdirectory(String) */

    @Test
    public void testGetHashedStringBasedSubdirectory() throws Exception {
        assertEquals(
                String.format("/08%s32%sc1", File.separator, File.separator),
                getHashedStringBasedSubdirectory("cats"));

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(FilesystemCache.DIRECTORY_DEPTH_CONFIG_KEY, 2);
        config.setProperty(FilesystemCache.DIRECTORY_NAME_LENGTH_CONFIG_KEY, 3);
        assertEquals(
                String.format("/083%s2c1", File.separator, File.separator),
                getHashedStringBasedSubdirectory("cats"));

        config.setProperty(FilesystemCache.DIRECTORY_DEPTH_CONFIG_KEY, 0);
        assertEquals("", getHashedStringBasedSubdirectory("cats"));
    }

    /* cleanUp() */

    @Test
    public void testCleanUpDoesNotDeleteUnexpiredFiles() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        // create a new source image file
        File sourceImageFile = instance.sourceImageFile(ops.getIdentifier());
        sourceImageFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(sourceImageFile, "not empty");
        // create a new derivative image file
        File derivativeImageFile = instance.derivativeImageFile(ops);
        derivativeImageFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(derivativeImageFile, "not empty");
        // create a new info file
        File infoFile = instance.infoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(infoFile, "not empty");
        // create some temp files
        File sourceImageTempFile = instance.sourceImageTempFile(ops.getIdentifier());
        FileUtils.writeStringToFile(sourceImageTempFile, "not empty");
        File derivativeImageTempFile = instance.derivativeImageTempFile(ops);
        FileUtils.writeStringToFile(derivativeImageTempFile, "not empty");
        File infoTempFile = instance.infoTempFile(ops.getIdentifier());
        FileUtils.writeStringToFile(infoTempFile, "not empty");

        // create some empty files
        String root = rootSourceImagePathname();
        File path = new File(root + "/bogus");
        path.mkdirs();
        new File(path + "/empty").createNewFile();
        new File(path + "/empty2").createNewFile();

        root = rootDerivativeImagePathname();
        path = new File(root + "/bogus");
        path.mkdirs();
        new File(path + "/empty").createNewFile();
        new File(path + "/empty2").createNewFile();

        root = rootInfoPathname();
        path = new File(root + "/bogus");
        path.mkdirs();
        new File(path + "/empty").createNewFile();
        new File(path + "/empty2").createNewFile();

        instance.setMinCleanableAge(10000);
        instance.cleanUp();

        Iterator<File> it = FileUtils.iterateFiles(fixturePath, null, true);
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(12, count);
    }

    @Test
    public void testCleanUpDeletesExpiredFiles() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        // create a new source image file
        File sourceImageFile = instance.sourceImageFile(ops.getIdentifier());
        sourceImageFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(sourceImageFile, "not empty");
        // create a new derivative image file
        File derivativeImageFile = instance.derivativeImageFile(ops);
        derivativeImageFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(derivativeImageFile, "not empty");
        // create a new info file
        File infoFile = instance.infoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(infoFile, "not empty");
        // create some temp files
        File sourceImageTempFile = instance.sourceImageTempFile(ops.getIdentifier());
        FileUtils.writeStringToFile(sourceImageTempFile, "not empty");
        File derivativeImageTempFile = instance.derivativeImageTempFile(ops);
        FileUtils.writeStringToFile(derivativeImageTempFile, "not empty");
        File infoTempFile = instance.infoTempFile(ops.getIdentifier());
        FileUtils.writeStringToFile(infoTempFile, "not empty");

        // create some empty files
        String root = rootSourceImagePathname();
        File path = new File(root + "/bogus");
        path.mkdirs();
        new File(path + "/empty").createNewFile();
        new File(path + "/empty2").createNewFile();

        root = rootDerivativeImagePathname();
        path = new File(root + "/bogus");
        path.mkdirs();
        new File(path + "/empty").createNewFile();
        new File(path + "/empty2").createNewFile();

        root = rootInfoPathname();
        path = new File(root + "/bogus");
        path.mkdirs();
        new File(path + "/empty").createNewFile();
        new File(path + "/empty2").createNewFile();

        instance.setMinCleanableAge(10);

        Thread.sleep(1000);

        instance.cleanUp();

        Iterator<File> it = FileUtils.iterateFiles(fixturePath, null, true);
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(3, count);
    }

    /* derivativeImageFile(OperationList) */

    @Test
    public void testDerivativeImageFileWithOperationList() throws Exception {
        String pathname = ConfigurationFactory.getInstance().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);

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

        OperationList ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.add(transform);
        ops.setOutputFormat(format);

        final String expected = String.format("%s%simage%s%s%s",
                pathname,
                File.separator,
                getHashedStringBasedSubdirectory(identifier.toString()),
                File.separator,
                ops.toFilename());
        assertEquals(new File(expected), instance.derivativeImageFile(ops));
    }

    @Test
    public void testDerivativeImageFileWithNoOpOperations() throws Exception {
        String pathname = ConfigurationFactory.getInstance().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);

        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        Rotate rotate = new Rotate(0);
        Format format = Format.TIF;

        final OperationList ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.setOutputFormat(format);

        final String expected = String.format("%s%simage%s%s%s",
                pathname,
                File.separator,
                getHashedStringBasedSubdirectory(ops.getIdentifier().toString()),
                File.separator,
                ops.toFilename());
        assertEquals(new File(expected), instance.derivativeImageFile(ops));
    }

    /* derivativeImageFiles(Identifier) */

    @Test
    public void testDerivativeImageFiles() throws Exception {
        Identifier identifier = new Identifier("dogs");
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(identifier);

        File imageFile = instance.derivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        ops.add(new Rotate(15));
        imageFile = instance.derivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        ops.add(ColorTransform.GRAY);
        imageFile = instance.derivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        assertEquals(3, instance.derivativeImageFiles(identifier).size());
    }

    /* derivativeImageTempFile(OperationList) */

    @Test
    public void testDerivativeImageTempFile() throws Exception {
        // TODO: write this
    }

    /* getImageInfo(Identifier) */

    @Test
    public void testGetImageInfoWithZeroTtl() throws Exception {
        Identifier identifier = new Identifier("test");
        File file = instance.infoFile(identifier);
        file.getParentFile().mkdirs();
        file.createNewFile();

        ObjectMapper mapper = new ObjectMapper();
        Info info = new Info(50, 50);
        mapper.writeValue(file, info);
        assertEquals(info, instance.getImageInfo(identifier));
    }

    @Test
    public void testGetImageInfoWithNonZeroTtl() throws Exception {
        ConfigurationFactory.getInstance().setProperty(Cache.TTL_CONFIG_KEY, 1);

        Identifier identifier = new Identifier("test");
        File file = instance.infoFile(identifier);
        file.getParentFile().mkdirs();
        file.createNewFile();

        ObjectMapper mapper = new ObjectMapper();
        Info info = new Info(50, 50);
        mapper.writeValue(file, info);

        Thread.sleep(1100);
        assertNull(instance.getImageInfo(identifier));
    }

    /* getSourceImageFile(Identifier) */

    @Test
    public void testGetSourceImageFileWithIdentifierWithZeroTtl()
            throws Exception {
        Identifier identifier = new Identifier("cats");
        assertNull(instance.getSourceImageFile(identifier));

        File imageFile = instance.sourceImageFile(identifier);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        assertNotNull(instance.getSourceImageFile(identifier));
    }

    @Test
    public void testGetSourceImageFileWithIdentifierWithNonzeroTtl()
            throws Exception {
        ConfigurationFactory.getInstance().setProperty(Cache.TTL_CONFIG_KEY, 1);

        Identifier identifier = new Identifier("cats");
        File cacheFile = instance.sourceImageFile(identifier);
        cacheFile.getParentFile().mkdirs();
        cacheFile.createNewFile();
        assertNotNull(instance.getSourceImageFile(identifier));

        Thread.sleep(1100);

        assertNull(instance.getSourceImageFile(identifier));
        assertFalse(cacheFile.exists());
    }

    /* infoFile(Identifier) */

    @Test
    public void testInfoFile() throws CacheException {
        final String pathname = ConfigurationFactory.getInstance().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);

        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");

        final String expected = String.format("%s%sinfo%s%s%s.json",
                pathname,
                File.separator,
                getHashedStringBasedSubdirectory(identifier.toString()),
                File.separator,
                identifier.toFilename());
        assertEquals(new File(expected), instance.infoFile(identifier));
    }

    @Test
    public void testInfoTempFile() throws Exception {
        // TODO: write this
    }

    /* newDerivativeImageInputStream(OperationList) */

    @Test
    public void testNewDerivativeImageInputStreamWithOpListWithZeroTtl()
            throws Exception {
        OperationList ops = TestUtil.newOperationList();
        assertNull(instance.newDerivativeImageInputStream(ops));

        File imageFile = instance.derivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        assertNotNull(instance.newDerivativeImageInputStream(ops));
    }

    @Test
    public void testNewDerivativeImageInputStreamWithOpListWithNonzeroTtl()
            throws Exception {
        ConfigurationFactory.getInstance().setProperty(Cache.TTL_CONFIG_KEY, 1);

        OperationList ops = TestUtil.newOperationList();
        File cacheFile = instance.derivativeImageFile(ops);
        cacheFile.getParentFile().mkdirs();
        cacheFile.createNewFile();
        assertNotNull(instance.newDerivativeImageInputStream(ops));

        Thread.sleep(1100);

        assertNull(instance.newDerivativeImageInputStream(ops));
        assertFalse(cacheFile.exists());
    }

    /* newDerivativeImageOutputStream(OperationList) */

    @Test
    public void testNewDerivativeImageOutputStreamWithOpList() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        assertNotNull(instance.newDerivativeImageOutputStream(ops));
    }

    @Test
    public void testNewDerivativeImageOutputStreamWithOpListCreatesFolder()
            throws Exception {
        FileUtils.deleteDirectory(derivativeImagePath);

        OperationList ops = TestUtil.newOperationList();
        instance.newDerivativeImageOutputStream(ops);
        assertTrue(derivativeImagePath.exists());
    }

    /* newSourceImageOutputStream(Identifier) */

    @Test
    public void testNewSourceImageOutputStreamWithIdentifier() throws Exception {
        assertNotNull(instance.newSourceImageOutputStream(new Identifier("cats")));
    }

    @Test
    public void testNewSourceImageOutputStreamWithIdentifierCreatesFolder()
            throws Exception {
        FileUtils.deleteDirectory(sourceImagePath);

        Identifier identifier = new Identifier("cats");
        instance.newSourceImageOutputStream(identifier);
        assertTrue(sourceImagePath.exists());
    }

    /* sourceImageFile(Identifier) */

    @Test
    public void testGetSourceImageFileWithIdentifier() throws Exception {
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");

        final String pathname = ConfigurationFactory.getInstance().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);
        final String expected = String.format("%s%ssource%s%s%s",
                pathname,
                File.separator,
                getHashedStringBasedSubdirectory(identifier.toString()),
                File.separator,
                identifier.toFilename());
        assertEquals(new File(expected), instance.sourceImageFile(identifier));
    }

    /* sourceImageTempFile(Identifier) */

    @Test
    public void testGetSourceImageTempFile() throws Exception {
        // TODO: write this
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        // create a new source image file
        File sourceImageFile = instance.sourceImageFile(ops.getIdentifier());
        sourceImageFile.getParentFile().mkdirs();
        sourceImageFile.createNewFile();
        // create a new derivative image file
        File derivativeImageFile = instance.derivativeImageFile(ops);
        derivativeImageFile.getParentFile().mkdirs();
        derivativeImageFile.createNewFile();
        // create a new info file
        File infoFile = instance.infoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        // change the op list
        ops.setIdentifier(new Identifier("dogs"));
        ops.add(new Rotate(15));

        // create a new derivative image file based on the changed op list
        derivativeImageFile = instance.derivativeImageFile(ops);
        derivativeImageFile.getParentFile().mkdirs();
        derivativeImageFile.createNewFile();

        instance.purge();

        Iterator<File> it = FileUtils.iterateFiles(fixturePath, null, true);
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(0, count);
    }

    /* purge(OperationsList) */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        final File imageFile = instance.derivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        instance.purge(ops);
        assertEquals(0, FileUtils.listFiles(derivativeImagePath, null, true).size());
    }

    /* purgeExpired() */

    @Test
    public void testPurgeExpired() throws Exception {
        ConfigurationFactory.getInstance().setProperty(Cache.TTL_CONFIG_KEY, 1);

        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();

        // add a source image
        OperationList ops = TestUtil.newOperationList();
        File imageFile = instance.sourceImageFile(ops.getIdentifier());
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        // add a derivative image
        ops = TestUtil.newOperationList();
        imageFile = instance.derivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        // add an info
        File infoFile = instance.infoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        // wait for them to expire
        Thread.sleep(1500);

        // add a changed derivative
        ops.setIdentifier(new Identifier("dogs"));
        imageFile = instance.derivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        instance.purgeExpired();
        assertEquals(0, FileUtils.listFiles(sourceImagePath, null, true).size());
        assertEquals(1, FileUtils.listFiles(derivativeImagePath, null, true).size());
        assertEquals(0, FileUtils.listFiles(infoPath, null, true).size());
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        Identifier id1 = new Identifier("dogs");
        ops.setIdentifier(id1);

        // create a new source image
        File sourceImageFile = instance.sourceImageFile(ops.getIdentifier());
        sourceImageFile.getParentFile().mkdirs();
        sourceImageFile.createNewFile();
        // create a new derivative image
        File derivativeImageFile = instance.derivativeImageFile(ops);
        derivativeImageFile.getParentFile().mkdirs();
        derivativeImageFile.createNewFile();
        // create a new info
        File infoFile = instance.infoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        Identifier id2 = new Identifier("ferrets");
        ops.setIdentifier(id2);
        ops.add(new Rotate(15));

        sourceImageFile = instance.sourceImageFile(ops.getIdentifier());
        sourceImageFile.getParentFile().mkdirs();
        sourceImageFile.createNewFile();
        derivativeImageFile = instance.derivativeImageFile(ops);
        derivativeImageFile.getParentFile().mkdirs();
        derivativeImageFile.createNewFile();
        infoFile = instance.infoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        assertEquals(2, FileUtils.listFiles(sourceImagePath, null, true).size());
        assertEquals(2, FileUtils.listFiles(derivativeImagePath, null, true).size());
        assertEquals(2, FileUtils.listFiles(infoPath, null, true).size());
        instance.purge(id1);
        assertEquals(1, FileUtils.listFiles(sourceImagePath, null, true).size());
        assertEquals(1, FileUtils.listFiles(derivativeImagePath, null, true).size());
        assertEquals(1, FileUtils.listFiles(infoPath, null, true).size());
        instance.purge(id2);
        assertEquals(0, FileUtils.listFiles(sourceImagePath, null, true).size());
        assertEquals(0, FileUtils.listFiles(derivativeImagePath, null, true).size());
        assertEquals(0, FileUtils.listFiles(infoPath, null, true).size());
    }

    /* put(Identifier, Info) */

    @Test
    public void testPut() throws CacheException {
        Identifier identifier = new Identifier("cats");
        Info info = new Info(52, 42);
        instance.put(identifier, info);
        assertEquals(info, instance.getImageInfo(identifier));
    }

    /**
     * This isn't foolproof, but is hopefully better than nothing.
     */
    @Test
    public void concurrentlyTestPut() throws CacheException {
        final Identifier identifier = new Identifier("monkeys");
        final Info info = new Info(52, 42);

        final AtomicBoolean anyFailures = new AtomicBoolean(false);
        final AtomicInteger readCount = new AtomicInteger(0);
        final AtomicInteger writeCount = new AtomicInteger(0);
        final short numWriterThreads = 500;

        // Fire off a bunch of threads to write the same info
        // hopefully-concurrently, and a bunch more to read it
        // hopefully-concurrently.
        for (int i = 0; i < numWriterThreads; i++) {
            new Thread(() -> { // writer thread
                try {
                    instance.put(identifier, info);
                } catch (Exception e) {
                    anyFailures.set(true);
                    e.printStackTrace();
                } finally {
                    writeCount.incrementAndGet();
                }
            }).start();

            new Thread(() -> { // reader thread
                while (true) {
                    // Spin until we have something to read.
                    if (writeCount.get() > 0) {
                        try {
                            Info otherInfo =
                                    instance.getImageInfo(identifier);
                            if (!info.equals(otherInfo)) {
                                throw new CacheException("Fail!");
                            }
                        } catch (Exception e) {
                            anyFailures.set(true);
                            e.printStackTrace();
                        } finally {
                            readCount.incrementAndGet();
                        }
                        break;
                    } else {
                        sleep(1);
                    }
                }
            }).start();
        }

        while (readCount.get() < numWriterThreads ||
                writeCount.get() < numWriterThreads) {
            sleep(1);
        }

        if (anyFailures.get()) {
            fail();
        } else {
            assertEquals(info, instance.getImageInfo(identifier));
        }
    }

    /* sourceImageFile(Identifier) */

    @Test
    public void testSourceImageFile() throws CacheException {
        // TODO: write this
    }

    @Test
    public void testSourceImageTempFile() throws Exception {
        // TODO: write this
    }

    private void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {}
    }

}
