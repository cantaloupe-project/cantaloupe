package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.request.Parameters;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.io.FileUtils;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class FilesystemCacheTest extends TestCase {

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
        imagePath.mkdirs();
        infoPath.mkdirs();

        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("FilesystemCache.pathname", fixturePath.toString());
        config.setProperty("FilesystemCache.ttl_seconds", 0);
        Application.setConfiguration(config);

        instance = new FilesystemCache();
    }

    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(fixturePath);
    }

    public void testFlush() throws Exception {
        Parameters params = new Parameters("cats", "full", "full", "0",
                "default", "jpg");
        instance.getCachedImageFile(params).createNewFile();
        instance.getCachedInfoFile(params.getIdentifier()).createNewFile();

        params = new Parameters("dogs", "full", "full", "15", "gray", "jpg");
        instance.getCachedImageFile(params).createNewFile();
        instance.getCachedInfoFile(params.getIdentifier()).createNewFile();

        instance.flush();
        assertEquals(0, imagePath.listFiles().length);
        assertEquals(0, infoPath.listFiles().length);
    }

    public void testFlushWithParameters() throws Exception {
        Parameters params = new Parameters("cats", "full", "full", "0",
                "default", "jpg");
        instance.getCachedImageFile(params).createNewFile();
        instance.getCachedInfoFile(params.getIdentifier()).createNewFile();
        instance.flush(params);
        assertEquals(0, imagePath.listFiles().length);
        assertEquals(0, infoPath.listFiles().length);
    }

    public void testFlushExpired() throws Exception {
        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);

        Parameters params = new Parameters("cats", "full", "full", "0",
                "default", "jpg");
        instance.getCachedImageFile(params).createNewFile();
        instance.getCachedInfoFile(params.getIdentifier()).createNewFile();

        Thread.sleep(2000);

        params = new Parameters("dogs", "full", "full", "0", "default", "jpg");
        instance.getCachedImageFile(params).createNewFile();
        instance.getCachedInfoFile(params.getIdentifier()).createNewFile();

        instance.flushExpired();
        assertEquals(1, imagePath.listFiles().length);
        assertEquals(1, infoPath.listFiles().length);
    }

    public void testGetDimensionWithZeroTtl() throws Exception {
        String identifier = "test";
        File file = new File(infoPath + File.separator + identifier + ".json");
        FileUtils.writeStringToFile(file, "50x50");

        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = new ImageInfo();
        info.setWidth(50);
        info.setHeight(50);
        mapper.writeValue(file, info);
        assertEquals(new Dimension(50, 50), instance.getDimension(identifier));
    }

    public void testGetDimensionWithNonZeroTtl() throws Exception {
        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);
        String identifier = "test";
        File file = new File(infoPath + File.separator + identifier + ".json");
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = new ImageInfo();
        info.setWidth(50);
        info.setHeight(50);
        mapper.writeValue(file, info);

        Thread.sleep(1100);
        assertNull(instance.getDimension(identifier));
    }

    public void testGetImageInputStreamWithZeroTtl() throws Exception {
        Parameters params = new Parameters("cats", "full", "full", "0",
                "default", "jpg");
        assertNull(instance.getImageInputStream(params));

        instance.getCachedImageFile(params).createNewFile();
        assertTrue(instance.getImageInputStream(params) instanceof FileInputStream);
    }

    public void testGetImageInputStreamWithNonzeroTtl() throws Exception {
        Parameters params = new Parameters("cats", "full", "full", "0",
                "default", "jpg");
        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);
        File cacheFile = instance.getCachedImageFile(params);
        cacheFile.createNewFile();
        assertTrue(instance.getImageInputStream(params) instanceof FileInputStream);
        Thread.sleep(1100);
        assertNull(instance.getImageInputStream(params));
        assertFalse(cacheFile.exists());
    }

    public void testGetImageOutputStream() throws Exception {
        Parameters params = new Parameters("cats", "full", "full", "0",
                "default", "jpg");
        assertTrue(instance.getImageOutputStream(params) instanceof FileOutputStream);
    }

    public void testImageOutputStreamCreatesFolder() throws IOException {
        FileUtils.deleteDirectory(imagePath);
        Parameters params = new Parameters("cats", "full", "full", "0",
                "default", "jpg");
        instance.getImageOutputStream(params);
        assertTrue(imagePath.exists());
    }

    public void testGetCachedImageFile() {
        String pathname = Application.getConfiguration().
                getString("FilesystemCache.pathname");
        String identifier = "cats_~!@#$%^&*()";
        String region = "0,0,50,50";
        String size = "pct:90.5";
        String rotation = "!10";
        String quality = "color";
        String format = "tif";
        Parameters params = new Parameters(identifier, region, size, rotation,
                quality, format);
        final String search = "[^A-Za-z0-9._-]";
        final String replacement = "";
        String expected = String.format("%s%simage%s%s_%s_%s_%s_%s.%s", pathname,
                File.separator,
                File.separator,
                identifier.replaceAll(search, replacement),
                region.replaceAll(search, replacement),
                size.replaceAll(search, replacement),
                rotation.replaceAll(search, replacement),
                quality, format);
        assertEquals(new File(expected), instance.getCachedImageFile(params));
    }

    public void testPutDimension() throws IOException {
        String identifier = "cats";
        Dimension dimension = new Dimension(52, 52);
        instance.putDimension(identifier, dimension);
        assertEquals(dimension, instance.getDimension(identifier));
    }

}
