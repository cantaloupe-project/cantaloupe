package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class FilesystemCacheTest {

    File fixturePath;
    File imagePath;
    File infoPath;
    FilesystemCache instance;

    @Before
    public void setUp() throws IOException {
        fixturePath = new File(TestUtil.getTempFolder().getAbsolutePath() + "/cache");
        imagePath = new File(fixturePath.getAbsolutePath() + "/image");
        infoPath = new File(fixturePath.getAbsolutePath() + "/info");

        if (!imagePath.mkdirs()) {
            throw new IOException("Failed to create temp image folder");
        }
        if (!infoPath.mkdirs()) {
            throw new IOException("Failed to create temp info folder");
        }

        final BaseConfiguration config = new BaseConfiguration();
        config.setProperty(FilesystemCache.DIRECTORY_DEPTH_CONFIG_KEY, 3);
        config.setProperty(FilesystemCache.DIRECTORY_NAME_LENGTH_CONFIG_KEY, 2);
        config.setProperty(FilesystemCache.PATHNAME_CONFIG_KEY,
                fixturePath.toString());
        config.setProperty(FilesystemCache.TTL_CONFIG_KEY, 0);
        Application.setConfiguration(config);

        instance = new FilesystemCache();
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(fixturePath);
    }

    /* purge() */

    @Test
    public void testPurge() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        // create new image and info files
        File imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        File infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        // change the op list
        ops.setIdentifier(new Identifier("dogs"));
        ops.add(new Rotate(15));

        // create new image and info files based on the changed op list
        imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        instance.purge();
        assertEquals(0, fixturePath.listFiles().length);
    }

    @Test
    public void testPurgeFailureThrowsException() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        // create unwritable cache files
        File imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        imageFile.getParentFile().setWritable(false);
        File infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();
        infoFile.getParentFile().setWritable(false);
        try {
            try {
                instance.purge();
                fail("Expected exception");
            } catch (CacheException e) {
                assertTrue(e.getMessage().startsWith("Unable to delete"));
            }
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
            instance.purge();
            assertEquals(0, fixturePath.listFiles().length);
        } finally {
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
        }
    }

    /* purge(Identifier) */

    @Test
    public void testPurgeWithIdentifier() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        Identifier id1 = new Identifier("dogs");
        ops.setIdentifier(id1);

        File imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        File infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        Identifier id2 = new Identifier("ferrets");
        ops.setIdentifier(id2);
        ops.add(new Rotate(15));

        imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        assertEquals(2, FileUtils.listFiles(imagePath, null, true).size());
        assertEquals(2, FileUtils.listFiles(infoPath, null, true).size());
        instance.purge(id1);
        assertEquals(1, FileUtils.listFiles(imagePath, null, true).size());
        assertEquals(1, FileUtils.listFiles(infoPath, null, true).size());
        instance.purge(id2);
        assertEquals(0, FileUtils.listFiles(imagePath, null, true).size());
        assertEquals(0, FileUtils.listFiles(infoPath, null, true).size());
    }

    /* purge(OperationsList) */

    @Test
    public void testPurgeWithOperationList() throws Exception {
        OperationList ops = TestUtil.newOperationList();

        final File imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        final File dimensionFile = instance.getInfoFile(ops.getIdentifier());
        dimensionFile.getParentFile().mkdirs();
        dimensionFile.createNewFile();

        instance.purge(ops);
        assertEquals(0, FileUtils.listFiles(imagePath, null, true).size());
        assertEquals(0, FileUtils.listFiles(infoPath, null, true).size());
    }

    @Test
    public void testPurgeWithOperationListFailureThrowsException()
            throws Exception {
        OperationList ops = TestUtil.newOperationList();

        File imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        imageFile.getParentFile().setWritable(false);

        File infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();
        infoFile.getParentFile().setWritable(false);

        try {
            try {
                instance.purge(ops);
                fail("Expected exception");
            } catch (CacheException e) {
                assertTrue(e.getMessage().startsWith("Unable to delete"));
            }
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
            instance.purge(ops);
            assertEquals(0, FileUtils.listFiles(imagePath, null, true).size());
            assertEquals(0, FileUtils.listFiles(infoPath, null, true).size());
        } finally {
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
        }
    }

    /* purgeExpired() */

    @Test
    public void testPurgeExpired() throws Exception {
        Application.getConfiguration().setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);

        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);

        OperationList ops = TestUtil.newOperationList();
        File imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        File infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        Thread.sleep(1500);

        ops.setIdentifier(new Identifier("dogs"));
        imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();

        instance.purgeExpired();
        assertEquals(1, FileUtils.listFiles(imagePath, null, true).size());
        assertEquals(1, FileUtils.listFiles(infoPath, null, true).size());
    }

    @Test
    public void testPurgeExpiredFailureThrowsException() throws Exception {
        Application.getConfiguration().setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);

        OperationList ops = TestUtil.newOperationList();
        // create an unwritable image cache file
        File imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        imageFile.getParentFile().setWritable(false);

        File infoFile = instance.getInfoFile(ops.getIdentifier());
        infoFile.getParentFile().mkdirs();
        infoFile.createNewFile();
        infoFile.getParentFile().setWritable(false);

        Thread.sleep(1500);

        try {
            try {
                instance.purgeExpired();
                fail("Expected exception");
            } catch (CacheException e) {
                assertTrue(e.getMessage().startsWith("Unable to delete"));
            }
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
            instance.purgeExpired();
            assertEquals(0, FileUtils.listFiles(imagePath, null, true).size());
            assertEquals(0, FileUtils.listFiles(infoPath, null, true).size());
        } finally {
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
        }
    }

    /* getDimension(Identifier) */

    @Test
    public void testGetDimensionWithZeroTtl() throws Exception {
        Identifier identifier = new Identifier("test");
        File file = instance.getInfoFile(identifier);
        file.getParentFile().mkdirs();
        file.createNewFile();

        ObjectMapper mapper = new ObjectMapper();
        FilesystemCache.ImageInfo info = new FilesystemCache.ImageInfo();
        info.width = 50;
        info.height = 50;
        mapper.writeValue(file, info);
        assertEquals(new Dimension(50, 50), instance.getDimension(identifier));
    }

    @Test
    public void testGetDimensionWithNonZeroTtl() throws Exception {
        Application.getConfiguration().setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);

        Identifier identifier = new Identifier("test");
        File file = instance.getInfoFile(identifier);
        file.getParentFile().mkdirs();
        file.createNewFile();

        ObjectMapper mapper = new ObjectMapper();
        FilesystemCache.ImageInfo info = new FilesystemCache.ImageInfo();
        info.width = 50;
        info.height = 50;
        mapper.writeValue(file, info);

        Thread.sleep(1100);
        assertNull(instance.getDimension(identifier));
    }

    /* getHashedStringBasedSubdirectory(String) */

    @Test
    public void testGetIdentifierBasedSubdirectory() throws Exception {
        assertEquals(
                String.format("/08%s32%sc1", File.separator, File.separator),
                instance.getHashedStringBasedSubdirectory("cats"));

        Configuration config = Application.getConfiguration();
        config.setProperty(FilesystemCache.DIRECTORY_DEPTH_CONFIG_KEY, 2);
        config.setProperty(FilesystemCache.DIRECTORY_NAME_LENGTH_CONFIG_KEY, 3);
        assertEquals(
                String.format("/083%s2c1", File.separator, File.separator),
                instance.getHashedStringBasedSubdirectory("cats"));

        config.setProperty(FilesystemCache.DIRECTORY_DEPTH_CONFIG_KEY, 0);
        assertEquals("", instance.getHashedStringBasedSubdirectory("cats"));
    }

    /* getImageFile(OperationList) */

    @Test
    public void testGetImageFile() throws Exception {
        String pathname = Application.getConfiguration().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Crop crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.905f);
        Rotate rotate = new Rotate(10);
        Filter filter = Filter.BITONAL;
        Format format = Format.TIF;

        OperationList ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.add(filter);
        ops.setOutputFormat(format);

        final String expected = String.format("%s%simage%s%s%s_%s_%s_%s_%s.%s",
                pathname,
                File.separator,
                instance.getHashedStringBasedSubdirectory(identifier.toString()),
                File.separator,
                FilesystemCache.filenameSafe(identifier.toString()),
                FilesystemCache.filenameSafe(crop.toString()),
                FilesystemCache.filenameSafe(scale.toString()),
                FilesystemCache.filenameSafe(rotate.toString()),
                FilesystemCache.filenameSafe(filter.toString()),
                format);
        assertEquals(new File(expected), instance.getImageFile(ops));
    }

    @Test
    public void testGetImageFileWithNoOpOperations() throws Exception {
        String pathname = Application.getConfiguration().
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
                instance.getHashedStringBasedSubdirectory(ops.getIdentifier().toString()),
                File.separator,
                FilesystemCache.filenameSafe(identifier.toString()),
                format);
        assertEquals(new File(expected), instance.getImageFile(ops));
    }

    /* getImageFiles(Identifier) */

    @Test
    public void testGetImageFiles() throws Exception {
        Identifier identifier = new Identifier("dogs");

        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(identifier);
        File imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        ops.add(new Rotate(15));
        imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        ops.add(Filter.GRAY);
        imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();

        assertEquals(3, instance.getImageFiles(identifier).size());
    }

    /* getImageInputStream(OperationList) */

    @Test
    public void testGetImageInputStreamWithZeroTtl() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        assertNull(instance.getImageInputStream(ops));

        File imageFile = instance.getImageFile(ops);
        imageFile.getParentFile().mkdirs();
        imageFile.createNewFile();
        assertNotNull(instance.getImageInputStream(ops));
    }

    @Test
    public void testGetImageInputStreamWithNonzeroTtl() throws Exception {
        Application.getConfiguration().
                setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);

        OperationList ops = TestUtil.newOperationList();
        File cacheFile = instance.getImageFile(ops);
        cacheFile.getParentFile().mkdirs();
        cacheFile.createNewFile();
        assertNotNull(instance.getImageInputStream(ops));

        Thread.sleep(1100);

        assertNull(instance.getImageInputStream(ops));
        assertFalse(cacheFile.exists());
    }

    /* getImageOutputStream(OperationList) */

    @Test
    public void testGetImageOutputStream() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        assertNotNull(instance.getImageOutputStream(ops));
    }

    @Test
    public void testImageOutputStreamCreatesFolder() throws Exception {
        FileUtils.deleteDirectory(imagePath);

        OperationList ops = TestUtil.newOperationList();
        instance.getImageOutputStream(ops);
        assertTrue(imagePath.exists());
    }

    /* getInfoFile(Identifier) */

    @Test
    public void testGetInfoFile() throws CacheException {
        final String pathname = Application.getConfiguration().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);

        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");

        final String expected = String.format("%s%sinfo%s%s%s.json",
                pathname,
                File.separator,
                instance.getHashedStringBasedSubdirectory(identifier.toString()),
                File.separator,
                FilesystemCache.filenameSafe(identifier.toString()));
        assertEquals(new File(expected), instance.getInfoFile(identifier));
    }

    /* putDimension(Identifier, Dimension) */

    @Test
    public void testPutDimension() throws CacheException {
        Identifier identifier = new Identifier("cats");
        Dimension dimension = new Dimension(52, 52);
        instance.putDimension(identifier, dimension);
        assertEquals(dimension, instance.getDimension(identifier));
    }

    @Test
    public void testPutDimensionFailureThrowsException() throws CacheException {
        final Identifier identifier = new Identifier("cats");
        final File cacheFile = instance.getInfoFile(identifier);
        cacheFile.getParentFile().mkdirs();
        cacheFile.getParentFile().setWritable(false);
        try {
            try {
                instance.putDimension(identifier, new Dimension(52, 52));
                fail("Expected exception");
            } catch (CacheException e) {
                assertTrue(e.getMessage().startsWith("Unable to create"));
            }
        } finally {
            cacheFile.getParentFile().setWritable(true);
        }
    }

}
