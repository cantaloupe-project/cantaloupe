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
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
        config.setProperty("FilesystemCache.pathname", fixturePath.toString());
        config.setProperty("FilesystemCache.ttl_seconds", 0);
        Application.setConfiguration(config);

        instance = new FilesystemCache();
    }

    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(fixturePath);
    }

    /* flush() */

    public void testFlush() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        instance.getImageFile(ops).createNewFile();
        instance.getDimensionFile(ops.getIdentifier()).createNewFile();

        ops.setIdentifier(new Identifier("dogs"));
        ops.add(new Rotate(15));
        instance.getImageFile(ops).createNewFile();
        instance.getDimensionFile(ops.getIdentifier()).createNewFile();

        instance.flush();
        assertEquals(0, imagePath.listFiles().length);
        assertEquals(0, infoPath.listFiles().length);
    }

    public void testFlushFailureThrowsException() throws Exception {
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
                instance.flush();
                fail("Expected exception");
            } catch (IOException e) {
                assertTrue(e.getMessage().startsWith("Unable to delete"));
            }
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
            instance.flush();
            assertEquals(0, imagePath.listFiles().length);
            assertEquals(0, infoPath.listFiles().length);
        } finally {
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
        }
    }

    /* flush(OperationsList) */

    public void testFlushWithOperationList() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        instance.getImageFile(ops).createNewFile();
        instance.getDimensionFile(ops.getIdentifier()).createNewFile();
        instance.flush(ops);
        assertEquals(0, imagePath.listFiles().length);
        assertEquals(0, infoPath.listFiles().length);
    }

    public void testFlushWithOperationListFailureThrowsException()
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
                instance.flush(ops);
                fail("Expected exception");
            } catch (IOException e) {
                assertTrue(e.getMessage().startsWith("Unable to delete"));
            }
            imageFile.getParentFile().setWritable(true);
            dimensionFile.getParentFile().setWritable(true);
            instance.flush(ops);
            assertEquals(0, imagePath.listFiles().length);
            assertEquals(0, infoPath.listFiles().length);
        } finally {
            imageFile.getParentFile().setWritable(true);
            dimensionFile.getParentFile().setWritable(true);
        }
    }

    /* flushExpired() */

    public void testFlushExpired() throws Exception {
        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);

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

        instance.flushExpired();
        assertEquals(1, imagePath.listFiles().length);
        assertEquals(1, infoPath.listFiles().length);
    }

    public void testFlushExpiredFailureThrowsException() throws Exception {
        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);

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
                instance.flushExpired();
                fail("Expected exception");
            } catch (IOException e) {
                assertTrue(e.getMessage().startsWith("Unable to delete"));
            }
            imageFile.getParentFile().setWritable(true);
            infoFile.getParentFile().setWritable(true);
            instance.flushExpired();
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
        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);
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

    /* getImageInputStream(OperationList) */

    public void testGetImageInputStreamWithZeroTtl() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        assertNull(instance.getImageInputStream(ops));

        instance.getImageFile(ops).createNewFile();
        assertTrue(instance.getImageInputStream(ops) instanceof FileInputStream);
    }

    public void testGetImageInputStreamWithNonzeroTtl() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);
        File cacheFile = instance.getImageFile(ops);
        cacheFile.createNewFile();
        assertTrue(instance.getImageInputStream(ops) instanceof FileInputStream);

        Thread.sleep(1100);
        assertNull(instance.getImageInputStream(ops));
        assertFalse(cacheFile.exists());
    }

    /* getImageOutputStream(OperationList) */

    public void testGetImageOutputStream() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        assertTrue(instance.getImageOutputStream(ops) instanceof FileOutputStream);
    }

    public void testImageOutputStreamCreatesFolder() throws IOException {
        FileUtils.deleteDirectory(imagePath);

        OperationList ops = TestUtil.newOperationList();
        instance.getImageOutputStream(ops);
        assertTrue(imagePath.exists());
    }

    /* getImageFile(OperationList) */

    public void testGetImageFile() {
        String pathname = Application.getConfiguration().
                getString("FilesystemCache.pathname");

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Crop crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.905f);
        Rotate rotate = new Rotate(10);
        Filter filter = Filter.NONE;
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
