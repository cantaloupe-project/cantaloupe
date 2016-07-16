package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Color;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.ImageInfo;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.*;

public class FilesystemCacheTest {

    File fixturePath;
    File sourceImagePath;
    File derivativeImagePath;
    File infoPath;
    FilesystemCache instance;

    @Before
    public void setUp() throws IOException {
        fixturePath = new File(TestUtil.getTempFolder().getAbsolutePath() + "/cache");
        sourceImagePath = new File(fixturePath.getAbsolutePath() + "/source");
        derivativeImagePath = new File(fixturePath.getAbsolutePath() + "/image");
        infoPath = new File(fixturePath.getAbsolutePath() + "/info");

        if (!sourceImagePath.mkdirs()) {
            throw new IOException("Failed to create folder: " + sourceImagePath);
        }
        if (!derivativeImagePath.mkdirs()) {
            throw new IOException("Failed to create folder: " + derivativeImagePath);
        }
        if (!infoPath.mkdirs()) {
            throw new IOException("Failed to create folder: " + infoPath);
        }

        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(FilesystemCache.DIRECTORY_DEPTH_CONFIG_KEY, 3);
        config.setProperty(FilesystemCache.DIRECTORY_NAME_LENGTH_CONFIG_KEY, 2);
        config.setProperty(FilesystemCache.PATHNAME_CONFIG_KEY,
                fixturePath.toString());
        config.setProperty(FilesystemCache.TTL_CONFIG_KEY, 0);

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
                FilesystemCache.getHashedStringBasedSubdirectory("cats"));

        Configuration config = Configuration.getInstance();
        config.setProperty(FilesystemCache.DIRECTORY_DEPTH_CONFIG_KEY, 2);
        config.setProperty(FilesystemCache.DIRECTORY_NAME_LENGTH_CONFIG_KEY, 3);
        assertEquals(
                String.format("/083%s2c1", File.separator, File.separator),
                FilesystemCache.getHashedStringBasedSubdirectory("cats"));

        config.setProperty(FilesystemCache.DIRECTORY_DEPTH_CONFIG_KEY, 0);
        assertEquals("", FilesystemCache.getHashedStringBasedSubdirectory("cats"));
    }

    /* cleanUp() */

    @Test
    public void testCleanUp() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        // create a new source image file
        File sourceImageFile = instance.getSourceImageFile(ops.getIdentifier());
        sourceImageFile.getParentFile().mkdirs();
        sourceImageFile.createNewFile();
        // create a new derivative image file
        File derivativeImageFile = instance.getDerivativeImageFile(ops);
        derivativeImageFile.getParentFile().mkdirs();
        derivativeImageFile.createNewFile();
        // create a new info file
        File infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();
        // create some temp files
        File sourceImageTempFile = instance.getSourceImageTempFile(ops.getIdentifier());
        sourceImageTempFile.createNewFile();
        File derivativeImageTempFile = instance.getDerivativeImageTempFile(ops);
        derivativeImageTempFile.createNewFile();
        File infoTempFile = instance.getInfoTempFile(ops.getIdentifier());
        infoTempFile.createNewFile();

        instance.cleanUp();

        // the temp files aren't expired yet, so expect them to be present
        Iterator<File> it = FileUtils.iterateFiles(fixturePath, null, true);
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(6, count);

        // expire them and check again
        for (File file : new File[] { sourceImageTempFile,
                derivativeImageTempFile, infoTempFile }) {
            file.setLastModified(System.currentTimeMillis() - 1000 * 60 * 300);
        }

        instance.cleanUp();

        it = FileUtils.iterateFiles(fixturePath, null, true);
        count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(3, count);
    }

    /* getDerivativeImageFile(OperationList) */

    @Test
    public void testGetDerivativeImageFileWithOperationList() throws Exception {
        String pathname = Configuration.getInstance().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Crop crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.905f);
        Rotate rotate = new Rotate(10);
        Color color = Color.BITONAL;
        Format format = Format.TIF;

        OperationList ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.add(color);
        ops.setOutputFormat(format);

        final String expected = String.format("%s%simage%s%s%s_%s_%s_%s_%s.%s",
                pathname,
                File.separator,
                FilesystemCache.getHashedStringBasedSubdirectory(identifier.toString()),
                File.separator,
                FilesystemCache.filenameSafe(identifier.toString()),
                FilesystemCache.filenameSafe(crop.toString()),
                FilesystemCache.filenameSafe(scale.toString()),
                FilesystemCache.filenameSafe(rotate.toString()),
                FilesystemCache.filenameSafe(color.toString()),
                format);
        assertEquals(new File(expected), instance.getDerivativeImageFile(ops));
    }

    @Test
    public void testGetDerivativeImageFileWithNoOpOperations() throws Exception {
        String pathname = Configuration.getInstance().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);

        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        Rotate rotate = new Rotate(0);
        Format format = Format.TIF;

        final OperationList ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.setOutputFormat(format);

        final String expected = String.format("%s%simage%s%s%s.%s",
                pathname,
                File.separator,
                FilesystemCache.getHashedStringBasedSubdirectory(ops.getIdentifier().toString()),
                File.separator,
                FilesystemCache.filenameSafe(identifier.toString()),
                format);
        assertEquals(new File(expected), instance.getDerivativeImageFile(ops));
    }

    /* getDerivativeImageFiles(Identifier) */

    @Test
    public void testGetDerivativeImageFiles() throws Exception {
        Identifier identifier = new Identifier("dogs");
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(identifier);

        File imageFile = instance.getDerivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        ops.add(new Rotate(15));
        imageFile = instance.getDerivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        ops.add(Color.GRAY);
        imageFile = instance.getDerivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        assertEquals(3, instance.getDerivativeImageFiles(identifier).size());
    }

    /* getDerivativeImageTempFile(OperationList) */

    @Test
    public void testGetDerivativeImageTempFile() throws Exception {
        // TODO: write this
    }

    /* getImageFile(Identifier) */

    @Test
    public void testGetImageFileWithIdentifierWithZeroTtl()
            throws Exception {
        Identifier identifier = new Identifier("cats");
        assertNull(instance.getImageFile(identifier));

        File imageFile = instance.getSourceImageFile(identifier);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        assertNotNull(instance.getImageFile(identifier));
    }

    @Test
    public void testGetImageFileWithIdentifierWithNonzeroTtl()
            throws Exception {
        Configuration.getInstance().
                setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);

        Identifier identifier = new Identifier("cats");
        File cacheFile = instance.getSourceImageFile(identifier);
        cacheFile.getParentFile().mkdirs();
        cacheFile.createNewFile();
        assertNotNull(instance.getImageFile(identifier));

        Thread.sleep(1100);

        assertNull(instance.getImageFile(identifier));
        assertFalse(cacheFile.exists());
    }

    /* getImageInfo(Identifier) */

    @Test
    public void testGetImageInfoWithZeroTtl() throws Exception {
        Identifier identifier = new Identifier("test");
        File file = instance.getInfoFile(identifier);
        file.getParentFile().mkdirs();
        file.createNewFile();

        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = new ImageInfo(50, 50);
        mapper.writeValue(file, info);
        assertEquals(info, instance.getImageInfo(identifier));
    }

    @Test
    public void testGetImageInfoWithNonZeroTtl() throws Exception {
        Configuration.getInstance().
                setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);

        Identifier identifier = new Identifier("test");
        File file = instance.getInfoFile(identifier);
        file.getParentFile().mkdirs();
        file.createNewFile();

        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = new ImageInfo(50, 50);
        mapper.writeValue(file, info);

        Thread.sleep(1100);
        assertNull(instance.getImageInfo(identifier));
    }

    /* getImageInputStream(OperationList) */

    @Test
    public void testGetImageInputStreamWithOpListWithZeroTtl() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        assertNull(instance.getImageInputStream(ops));

        File imageFile = instance.getDerivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        assertNotNull(instance.getImageInputStream(ops));
    }

    @Test
    public void testGetImageInputStreamWithOpListWithNonzeroTtl() throws Exception {
        Configuration.getInstance().
                setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);

        OperationList ops = TestUtil.newOperationList();
        File cacheFile = instance.getDerivativeImageFile(ops);
        cacheFile.getParentFile().mkdirs();
        cacheFile.createNewFile();
        assertNotNull(instance.getImageInputStream(ops));

        Thread.sleep(1100);

        assertNull(instance.getImageInputStream(ops));
        assertFalse(cacheFile.exists());
    }

    /* getImageOutputStream(Identifier) */

    @Test
    public void testGetImageOutputStreamWithIdentifier() throws Exception {
        assertNotNull(instance.getImageOutputStream(new Identifier("cats")));
    }

    @Test
    public void testGetImageOutputStreamWithIdentifierCreatesFolder()
            throws Exception {
        FileUtils.deleteDirectory(sourceImagePath);

        Identifier identifier = new Identifier("cats");
        instance.getImageOutputStream(identifier);
        assertTrue(sourceImagePath.exists());
    }

    /* getImageOutputStream(OperationList) */

    @Test
    public void testGetImageOutputStreamWithOpList() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        assertNotNull(instance.getImageOutputStream(ops));
    }

    @Test
    public void testGetImageOutputStreamWithOpListCreatesFolder() throws Exception {
        FileUtils.deleteDirectory(derivativeImagePath);

        OperationList ops = TestUtil.newOperationList();
        instance.getImageOutputStream(ops);
        assertTrue(derivativeImagePath.exists());
    }

    /* getInfoFile(Identifier) */

    @Test
    public void testGetInfoFile() throws CacheException {
        final String pathname = Configuration.getInstance().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);

        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");

        final String expected = String.format("%s%sinfo%s%s%s.json",
                pathname,
                File.separator,
                FilesystemCache.getHashedStringBasedSubdirectory(identifier.toString()),
                File.separator,
                FilesystemCache.filenameSafe(identifier.toString()));
        assertEquals(new File(expected), instance.getInfoFile(identifier));
    }

    @Test
    public void testGetInfoTempFile() throws Exception {
        // TODO: write this
    }

    /* getSourceImageFile(Identifier) */

    @Test
    public void testGetSourceImageFileWithIdentifier() throws Exception {
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");

        final String pathname = Configuration.getInstance().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);
        final String expected = String.format("%s%ssource%s%s%s",
                pathname,
                File.separator,
                FilesystemCache.getHashedStringBasedSubdirectory(identifier.toString()),
                File.separator,
                FilesystemCache.filenameSafe(identifier.toString()));
        assertEquals(new File(expected), instance.getSourceImageFile(identifier));
    }

    /* getSourceImageTempFile(Identifier) */

    @Test
    public void testGetSourceImageTempFile() throws Exception {
        // TODO: write this
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        // create a new source image file
        File sourceImageFile = instance.getSourceImageFile(ops.getIdentifier());
        sourceImageFile.getParentFile().mkdirs();
        sourceImageFile.createNewFile();
        // create a new derivative image file
        File derivativeImageFile = instance.getDerivativeImageFile(ops);
        derivativeImageFile.getParentFile().mkdirs();
        derivativeImageFile.createNewFile();
        // create a new info file
        File infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        // change the op list
        ops.setIdentifier(new Identifier("dogs"));
        ops.add(new Rotate(15));

        // create a new derivative image file based on the changed op list
        derivativeImageFile = instance.getDerivativeImageFile(ops);
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

        final File imageFile = instance.getDerivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        instance.purge(ops);
        assertEquals(0, FileUtils.listFiles(derivativeImagePath, null, true).size());
    }

    /* purgeExpired() */

    @Test
    public void testPurgeExpired() throws Exception {
        Configuration.getInstance().setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);

        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);

        // add a source image
        OperationList ops = TestUtil.newOperationList();
        File imageFile = instance.getSourceImageFile(ops.getIdentifier());
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        // add a derivative image
        ops = TestUtil.newOperationList();
        imageFile = instance.getDerivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        // add an info
        File infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        // wait for them to expire
        Thread.sleep(1500);

        // add a changed derivative
        ops.setIdentifier(new Identifier("dogs"));
        imageFile = instance.getDerivativeImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        instance.purgeExpired();
        assertEquals(1, FileUtils.listFiles(sourceImagePath, null, true).size());
        assertEquals(1, FileUtils.listFiles(derivativeImagePath, null, true).size());
        assertEquals(0, FileUtils.listFiles(infoPath, null, true).size());
    }

    /* purgeImage(Identifier) */

    @Test
    public void testPurgeImage() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        Identifier id1 = new Identifier("dogs");
        ops.setIdentifier(id1);

        // create a new source image
        File sourceImageFile = instance.getSourceImageFile(ops.getIdentifier());
        sourceImageFile.getParentFile().mkdirs();
        sourceImageFile.createNewFile();
        // create a new derivative image
        File derivativeImageFile = instance.getDerivativeImageFile(ops);
        derivativeImageFile.getParentFile().mkdirs();
        derivativeImageFile.createNewFile();
        // create a new info
        File infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        Identifier id2 = new Identifier("ferrets");
        ops.setIdentifier(id2);
        ops.add(new Rotate(15));

        sourceImageFile = instance.getSourceImageFile(ops.getIdentifier());
        sourceImageFile.getParentFile().mkdirs();
        sourceImageFile.createNewFile();
        derivativeImageFile = instance.getDerivativeImageFile(ops);
        derivativeImageFile.getParentFile().mkdirs();
        derivativeImageFile.createNewFile();
        infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        assertEquals(2, FileUtils.listFiles(sourceImagePath, null, true).size());
        assertEquals(2, FileUtils.listFiles(derivativeImagePath, null, true).size());
        assertEquals(2, FileUtils.listFiles(infoPath, null, true).size());
        instance.purgeImage(id1);
        assertEquals(1, FileUtils.listFiles(sourceImagePath, null, true).size());
        assertEquals(1, FileUtils.listFiles(derivativeImagePath, null, true).size());
        assertEquals(1, FileUtils.listFiles(infoPath, null, true).size());
        instance.purgeImage(id2);
        assertEquals(0, FileUtils.listFiles(sourceImagePath, null, true).size());
        assertEquals(0, FileUtils.listFiles(derivativeImagePath, null, true).size());
        assertEquals(0, FileUtils.listFiles(infoPath, null, true).size());
    }

    /* putImageInfo(Identifier, ImageInfo) */

    @Test
    public void testPutImageInfo() throws CacheException {
        Identifier identifier = new Identifier("cats");
        ImageInfo info = new ImageInfo(52, 42);
        instance.putImageInfo(identifier, info);
        assertEquals(info, instance.getImageInfo(identifier));
    }

    @Test
    public void testPutImageInfoFailureThrowsException() throws CacheException {
        final Identifier identifier = new Identifier("cats");
        final File cacheFile = instance.getInfoFile(identifier);
        cacheFile.getParentFile().mkdirs();
        cacheFile.getParentFile().setWritable(false);
        try {
            try {
                ImageInfo info = new ImageInfo(52, 52);
                instance.putImageInfo(identifier, info);
                fail("Expected exception");
            } catch (CacheException e) {
                assertTrue(e.getMessage().startsWith("Unable to create"));
            }
        } finally {
            cacheFile.getParentFile().setWritable(true);
        }
    }

}
