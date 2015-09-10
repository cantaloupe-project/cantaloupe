package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
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

    public void testGetFile() {
        try {
            assertNotNull(instance.getFile(FILE));
        } catch (FileNotFoundException e) {
            fail();
        }

        try {
            instance.getFile("bogus");
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    public void testGetInputStream() throws Exception {
        try {
            assertNotNull(instance.getInputStream(FILE));
        } catch (FileNotFoundException e) {
            fail();
        }

        try {
            assertNull(instance.getInputStream("bogus"));
            fail();
        } catch (FileNotFoundException e) {
            // pass
        }
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

    public void testGetSourceFormat() {
        assertEquals(SourceFormat.JPG, instance.getSourceFormat("image.jpg"));
        assertEquals(SourceFormat.UNKNOWN, instance.getSourceFormat("image.bogus"));
        assertEquals(SourceFormat.UNKNOWN, instance.getSourceFormat("image"));
    }

}
