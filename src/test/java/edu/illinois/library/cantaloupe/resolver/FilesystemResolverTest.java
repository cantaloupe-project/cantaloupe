package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilesystemResolverTest extends TestCase {

    private static final String FILE = "escher_lego.jpg";

    FilesystemResolver instance;

    public void setUp() throws IOException {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path fixturePath = Paths.get(cwd, "src", "test", "java", "edu",
                "illinois", "library", "cantaloupe", "test", "fixtures");
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("FilesystemResolver.path_prefix",
                        fixturePath + File.separator);
        Application.setConfiguration(config);

        instance = new FilesystemResolver();
    }

    public void testResolve() {
        assertNotNull(instance.resolve(FILE));
        assertNull(instance.resolve("bogus"));
    }

    public void testGetPath() {
        BaseConfiguration config = (BaseConfiguration) Application.getConfiguration();
        // with prefix
        config.setProperty("FilesystemResolver.path_prefix", "/prefix/");
        assertEquals("/prefix/id", instance.getPath("id"));
        // with suffix
        config.setProperty("FilesystemResolver.path_suffix", "/suffix");
        assertEquals("/prefix/id/suffix", instance.getPath("id"));
    }

}
