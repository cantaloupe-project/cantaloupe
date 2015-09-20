package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.request.Parameters;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

import java.io.File;

public class FilesystemCacheTest extends TestCase {

    FilesystemCache instance;

    public void setUp() {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("FilesystemCache.pathname", "/pathname");
        Application.setConfiguration(config);
        instance = new FilesystemCache();
    }

    public void testGet() throws Exception {
        // TODO: write this
    }

    public void testGetOutputStream() throws Exception {
        // TODO: write this
    }

    public void testGetCacheFile() {
        String pathname = Application.getConfiguration().
                getString("FilesystemCache.pathname");
        String identifier = "cats";
        String region = "full";
        String size = "full";
        String rotation = "0";
        String quality = "color";
        String format = "tif";
        Parameters params = new Parameters(identifier, region, size, rotation,
                quality, format);
        String expected = String.format("%s%s%s_%s_%s_%s_%s.%s", pathname,
                File.separator, identifier, region, size, rotation, quality,
                format);
        assertEquals(new File(expected), instance.getCacheFile(params));
    }

}
