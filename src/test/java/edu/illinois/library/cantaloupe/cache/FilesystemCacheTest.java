package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.io.FileUtils;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class FilesystemCacheTest extends CantaloupeTestCase {

    File fixturePath;
    File imagePath;
    File infoPath;
    FilesystemCache instance;

    public void setUp() throws IOException {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        fixturePath = Paths.get(cwd, "src", "test", "resources", "cache").toFile();
        imagePath = Paths.get(cwd, "src", "test", "resources", "cache", "image").toFile();
        infoPath = Paths.get(cwd, "src", "test", "resources", "cache", "info").toFile();
        FileUtils.deleteDirectory(fixturePath);
        if (!imagePath.mkdirs()) {
            throw new IOException("Failed to create temp image folder");
        }
        if (!infoPath.mkdirs()) {
            throw new IOException("Failed to create temp info folder");
        }

        BaseConfiguration config = new BaseConfiguration();
        config.setProperty(FilesystemCache.PATHNAME_CONFIG_KEY,
                fixturePath.toString());
        config.setProperty(FilesystemCache.TTL_CONFIG_KEY, 0);
        Application.setConfiguration(config);

        instance = new FilesystemCache();
    }

    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(fixturePath);
    }

    /* purge() */

    public void testPurge() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        instance.getImageFile(ops).createNewFile();
        instance.getDimensionFile(ops.getIdentifier()).createNewFile();

        ops.setIdentifier(new Identifier("dogs"));
        ops.add(new Rotate(15));
        instance.getImageFile(ops).createNewFile();
        instance.getDimensionFile(ops.getIdentifier()).createNewFile();

        instance.purge();
        assertEquals(0, imagePath.listFiles().length);
        assertEquals(0, infoPath.listFiles().length);
    }

    public void testPurgeFailureThrowsException() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        // create an unwritable image cache file
        File imageFile = instance.getImageFile(ops);
        File infoFile = instance.getDimensionFile(ops.getIdentifier());
        imageFile.createNewFile();
        infoFile.createNewFile();
        imageFile.getParentFile().setWritable(false);
        infoFile.getParentFile().setWritable(false);
        try {
            try {
                instance.purge();
                fail("Expected exception");
            } catch (IOException e) {
                assertTrue(e.getMessage().startsWith("Unable to delete"));
            }
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
            instance.purge();
            assertEquals(0, imagePath.listFiles().length);
            assertEquals(0, infoPath.listFiles().length);
        } finally {
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
        }
    }

    /* purge(Identifier) */

    public void testPurgeWithIdentifier() throws Exception {
        Identifier id1 = new Identifier("dogs");
        Identifier id2 = new Identifier("ferrets");

        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(id1);
        instance.getImageFile(ops).createNewFile();
        instance.getDimensionFile(ops.getIdentifier()).createNewFile();

        ops.setIdentifier(id2);
        ops.add(new Rotate(15));
        instance.getImageFile(ops).createNewFile();
        instance.getDimensionFile(ops.getIdentifier()).createNewFile();

        assertEquals(2, imagePath.listFiles().length);
        assertEquals(2, infoPath.listFiles().length);
        instance.purge(id1);
        assertEquals(1, imagePath.listFiles().length);
        assertEquals(1, infoPath.listFiles().length);
        instance.purge(id2);
        assertEquals(0, imagePath.listFiles().length);
        assertEquals(0, infoPath.listFiles().length);
    }

    /* purge(OperationsList) */

    public void testPurgeWithOperationList() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        instance.getImageFile(ops).createNewFile();
        instance.getDimensionFile(ops.getIdentifier()).createNewFile();
        instance.purge(ops);
        assertEquals(0, imagePath.listFiles().length);
        assertEquals(0, infoPath.listFiles().length);
    }

    public void testPurgeWithOperationListFailureThrowsException()
            throws Exception {
        OperationList ops = TestUtil.newOperationList();
        File imageFile = instance.getImageFile(ops);
        File dimensionFile = instance.getDimensionFile(ops.getIdentifier());
        imageFile.createNewFile();
        dimensionFile.createNewFile();
        imageFile.getParentFile().setWritable(false);
        dimensionFile.getParentFile().setWritable(false);
        try {
            try {
                instance.purge(ops);
                fail("Expected exception");
            } catch (IOException e) {
                assertTrue(e.getMessage().startsWith("Unable to delete"));
            }
            imageFile.getParentFile().setWritable(true);
            dimensionFile.getParentFile().setWritable(true);
            instance.purge(ops);
            assertEquals(0, imagePath.listFiles().length);
            assertEquals(0, infoPath.listFiles().length);
        } finally {
            imageFile.getParentFile().setWritable(true);
            dimensionFile.getParentFile().setWritable(true);
        }
    }

    /* purgeExpired() */

    public void testPurgeExpired() throws Exception {
        Application.getConfiguration().setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);

        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);

        OperationList ops = TestUtil.newOperationList();
        instance.getImageFile(ops).createNewFile();
        instance.getDimensionFile(ops.getIdentifier()).createNewFile();

        Thread.sleep(2000);

        ops.setIdentifier(new Identifier("dogs"));
        instance.getImageFile(ops).createNewFile();
        instance.getDimensionFile(ops.getIdentifier()).createNewFile();

        instance.purgeExpired();
        assertEquals(1, imagePath.listFiles().length);
        assertEquals(1, infoPath.listFiles().length);
    }

    public void testPurgeExpiredFailureThrowsException() throws Exception {
        Application.getConfiguration().setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);

        OperationList ops = TestUtil.newOperationList();
        // create an unwritable image cache file
        File imageFile = instance.getImageFile(ops);
        File infoFile = instance.getDimensionFile(ops.getIdentifier());
        imageFile.createNewFile();
        infoFile.createNewFile();
        imageFile.getParentFile().setWritable(false);
        infoFile.getParentFile().setWritable(false);

        Thread.sleep(1500);

        try {
            try {
                instance.purgeExpired();
                fail("Expected exception");
            } catch (IOException e) {
                assertTrue(e.getMessage().startsWith("Unable to delete"));
            }
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
            instance.purgeExpired();
            assertEquals(0, imagePath.listFiles().length);
            assertEquals(0, infoPath.listFiles().length);
        } finally {
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
        }
    }

    /* getDimension(Identifier) */

    public void testGetDimensionWithZeroTtl() throws Exception {
        Identifier identifier = new Identifier("test");
        File file = new File(infoPath + File.separator + identifier + ".json");
        FileUtils.writeStringToFile(file, "50x50");

        ObjectMapper mapper = new ObjectMapper();
        FilesystemCache.ImageInfo info = new FilesystemCache.ImageInfo();
        info.width = 50;
        info.height = 50;
        mapper.writeValue(file, info);
        assertEquals(new Dimension(50, 50), instance.getDimension(identifier));
    }

    public void testGetDimensionWithNonZeroTtl() throws Exception {
        Application.getConfiguration().setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);
        Identifier identifier = new Identifier("test");
        File file = new File(infoPath + File.separator + identifier + ".json");
        ObjectMapper mapper = new ObjectMapper();
        FilesystemCache.ImageInfo info = new FilesystemCache.ImageInfo();
        info.width = 50;
        info.height = 50;
        mapper.writeValue(file, info);

        Thread.sleep(1100);
        assertNull(instance.getDimension(identifier));
    }

    /* getDimensionFile(Identifier) */

    public void testGetDimensionFile() {
        // TODO: write this
    }

    /* getImageFile(OperationList) */

    public void testGetImageFile() {
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
        OutputFormat format = OutputFormat.TIF;

        OperationList ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.add(filter);
        ops.setOutputFormat(format);

        final String search = "[^A-Za-z0-9._-]";
        final String replacement = "_";
        String expected = String.format("%s%simage%s%s_%s_%s_%s_%s.%s", pathname,
                File.separator,
                File.separator,
                identifier.toString().replaceAll(search, replacement),
                crop.toString().replaceAll(search, replacement),
                scale.toString().replaceAll(search, replacement),
                rotate.toString().replaceAll(search, replacement),
                filter.toString().toLowerCase(), format);
        assertEquals(new File(expected), instance.getImageFile(ops));
    }

    public void testGetImageFileWithNoOpOperations() {
        String pathname = Application.getConfiguration().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        Rotate rotate = new Rotate(0);
        OutputFormat format = OutputFormat.TIF;

        OperationList ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.setOutputFormat(format);

        final String search = "[^A-Za-z0-9._-]";
        final String replacement = "_";
        String expected = String.format("%s%simage%s%s.%s", pathname,
                File.separator,
                File.separator,
                identifier.toString().replaceAll(search, replacement), format);
        assertEquals(new File(expected), instance.getImageFile(ops));
    }

    /* getImageFiles(Identifier) */

    public void testGetImageFiles() throws Exception {
        Identifier identifier = new Identifier("dogs");

        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(identifier);
        instance.getImageFile(ops).createNewFile();
        ops.add(new Rotate(15));
        instance.getImageFile(ops).createNewFile();
        ops.add(Filter.GRAY);
        instance.getImageFile(ops).createNewFile();

        assertEquals(3, instance.getImageFiles(identifier).size());
    }

    /* getImageInputStream(OperationList) */

    public void testGetImageReadableChannelWithZeroTtl() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        assertNull(instance.getImageReadableChannel(ops));

        instance.getImageFile(ops).createNewFile();
        assertNotNull(instance.getImageReadableChannel(ops));
    }

    public void testGetImageReadableChannelWithNonzeroTtl() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        Application.getConfiguration().
                setProperty(FilesystemCache.TTL_CONFIG_KEY, 1);
        File cacheFile = instance.getImageFile(ops);
        cacheFile.createNewFile();
        assertNotNull(instance.getImageReadableChannel(ops));

        Thread.sleep(1100);
        assertNull(instance.getImageReadableChannel(ops));
        assertFalse(cacheFile.exists());
    }

    /* getImageWritableChannel(OperationList) */

    public void testGetImageWritableChannel() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        assertNotNull(instance.getImageWritableChannel(ops));
    }

    public void testImageWritableChannelCreatesFolder() throws IOException {
        FileUtils.deleteDirectory(imagePath);

        OperationList ops = TestUtil.newOperationList();
        instance.getImageWritableChannel(ops);
        assertTrue(imagePath.exists());
    }

    public void testGetCachedImageFileWithNoOpOperations() {
        String pathname = Application.getConfiguration().
                getString(FilesystemCache.PATHNAME_CONFIG_KEY);

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        Rotate rotate = new Rotate(0);
        OutputFormat format = OutputFormat.TIF;

        OperationList ops = new OperationList();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.setOutputFormat(format);

        final String search = "[^A-Za-z0-9._-]";
        final String replacement = "_";
        String expected = String.format("%s%simage%s%s.%s", pathname,
                File.separator,
                File.separator,
                identifier.toString().replaceAll(search, replacement), format);
        assertEquals(new File(expected), instance.getImageFile(ops));
    }

    /* putDimension(Identifier, Dimension) */

    public void testPutDimension() throws IOException {
        Identifier identifier = new Identifier("cats");
        Dimension dimension = new Dimension(52, 52);
        instance.putDimension(identifier, dimension);
        assertEquals(dimension, instance.getDimension(identifier));
    }

    public void testPutDimensionFailureThrowsException() throws IOException {
        final Identifier identifier = new Identifier("cats");
        final File cacheFile = instance.getDimensionFile(identifier);
        cacheFile.getParentFile().setWritable(false);
        try {
            try {
                instance.putDimension(identifier, new Dimension(52, 52));
                fail("Expected exception");
            } catch (IOException e) {
                assertTrue(e.getMessage().startsWith("Unable to create"));
            }
        } finally {
            cacheFile.getParentFile().setWritable(true);
        }
    }

}
