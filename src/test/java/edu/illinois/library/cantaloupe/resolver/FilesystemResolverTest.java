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
        Path fixturePath = Paths.get(cwd, "src", "test", "resources");
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("FilesystemResolver.path_prefix",
                        fixturePath + File.separator);
        Application.setConfiguration(config);

        instance = new FilesystemResolver();
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

    public void testGetPathname() {
        BaseConfiguration config = (BaseConfiguration) Application.getConfiguration();
        // with prefix
        config.setProperty("FilesystemResolver.path_prefix", "/prefix/");
        assertEquals("/prefix/id", instance.getPathname("id"));
        // with suffix
        config.setProperty("FilesystemResolver.path_suffix", "/suffix");
        assertEquals("/prefix/id/suffix", instance.getPathname("id"));
        // without prefix or suffix
        config.setProperty("FilesystemResolver.path_prefix", "");
        config.setProperty("FilesystemResolver.path_suffix", "");
        assertEquals("id", instance.getPathname("id"));
        // with path separator
        String separator = "CATS";
        config.setProperty("FilesystemResolver.path_separator", separator);
        assertEquals("1" + File.separator + "2" + File.separator + "3",
                instance.getPathname("1" + separator + "2" + separator + "3"));
    }

    public void testGetSourceFormatWithExtensions() {
        assertEquals(SourceFormat.BMP, instance.getSourceFormat("bla.bmp"));
        assertEquals(SourceFormat.GIF, instance.getSourceFormat("bla.gif"));
        assertEquals(SourceFormat.JP2, instance.getSourceFormat("bla.JP2"));
        assertEquals(SourceFormat.PDF, instance.getSourceFormat("bla.pdf"));
        assertEquals(SourceFormat.PNG, instance.getSourceFormat("bla.png"));
        assertEquals(SourceFormat.TIF, instance.getSourceFormat("bla.tif"));
        assertEquals(SourceFormat.UNKNOWN, instance.getSourceFormat("bla.bogus"));
    }

    public void testGetSourceFormatByDetection() {
        assertEquals(SourceFormat.BMP, instance.getSourceFormat("bmp"));
        assertEquals(SourceFormat.GIF, instance.getSourceFormat("gif"));
        assertEquals(SourceFormat.JP2, instance.getSourceFormat("jp2"));
        assertEquals(SourceFormat.JPG, instance.getSourceFormat("jpg"));
        assertEquals(SourceFormat.PDF, instance.getSourceFormat("pdf"));
        assertEquals(SourceFormat.PNG, instance.getSourceFormat("png"));
        assertEquals(SourceFormat.TIF, instance.getSourceFormat("tif"));
        assertEquals(SourceFormat.UNKNOWN, instance.getSourceFormat("txt"));
    }

}
