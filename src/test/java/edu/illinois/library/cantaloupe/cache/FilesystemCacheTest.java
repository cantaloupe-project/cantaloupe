package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.request.Parameters;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class FilesystemCacheTest extends TestCase {

    File fixturePath;
    FilesystemCache instance;

    public void setUp() throws IOException {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        fixturePath = Paths.get(cwd, "src", "test", "resources", "cache").toFile();
        FileUtils.deleteDirectory(fixturePath);
        fixturePath.mkdirs();

        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("FilesystemCache.pathname", fixturePath.toString());
        config.setProperty("FilesystemCache.ttl_seconds", 0);
        Application.setConfiguration(config);

        instance = new FilesystemCache();
    }

    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(fixturePath);
    }

    public void testGetWithZeroTtl() throws Exception {
        String identifier = "cats";
        String region = "full";
        String size = "full";
        String rotation = "0";
        String quality = "color";
        String format = "tif";
        Parameters params = new Parameters(identifier, region, size, rotation,
                quality, format);
        assertNull(instance.get(params));

        instance.getCacheFile(params).createNewFile();
        assertTrue(instance.get(params) instanceof FileInputStream);
    }

    public void testGetWithNonzeroTtl() throws Exception {
        String identifier = "cats";
        String region = "full";
        String size = "full";
        String rotation = "0";
        String quality = "color";
        String format = "tif";
        Parameters params = new Parameters(identifier, region, size, rotation,
                quality, format);

        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);
        File cacheFile = instance.getCacheFile(params);
        cacheFile.createNewFile();
        assertTrue(instance.get(params) instanceof FileInputStream);
        Thread.sleep(1100);
        assertNull(instance.get(params));
        assertFalse(cacheFile.exists());
    }

    public void testGetOutputStream() throws Exception {
        // TODO: write this
    }

    public void testOutputStreamCreatesFolder() {
        // TODO: write this
    }

    public void testGetCacheFile() {
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
        String expected = String.format("%s%s%s_%s_%s_%s_%s.%s", pathname,
                File.separator,
                identifier.replaceAll(search, replacement),
                region.replaceAll(search, replacement),
                size.replaceAll(search, replacement),
                rotation.replaceAll(search, replacement),
                quality, format);
        assertEquals(new File(expected), instance.getCacheFile(params));
    }

}
