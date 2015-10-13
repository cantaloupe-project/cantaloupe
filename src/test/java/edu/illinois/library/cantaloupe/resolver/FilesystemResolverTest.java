package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.Identifier;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilesystemResolverTest extends TestCase {

    private static final Identifier IDENTIFIER = new Identifier("escher_lego.jpg");

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

    public void testGetFile() throws Exception {
        // present, readable file
        try {
            assertNotNull(instance.getFile(IDENTIFIER));
        } catch (FileNotFoundException e) {
            fail();
        }
        // missing file
        try {
            instance.getFile(new Identifier("bogus"));
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        } catch (IOException e) {
            fail("Expected FileNotFoundException");
        }
        // present, unreadable file
        // TODO: write this
    }

    public void testGetInputStream() throws Exception {
        try {
            assertNotNull(instance.getInputStream(IDENTIFIER));
        } catch (FileNotFoundException e) {
            fail();
        }

        try {
            assertNull(instance.getInputStream(new Identifier("bogus")));
            fail();
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    public void testGetPathname() {
        BaseConfiguration config = (BaseConfiguration) Application.getConfiguration();
        // with prefix
        config.setProperty("FilesystemResolver.path_prefix", "/prefix/");
        assertEquals("/prefix/id", instance.getPathname(new Identifier("id")));
        // with suffix
        config.setProperty("FilesystemResolver.path_suffix", "/suffix");
        assertEquals("/prefix/id/suffix", instance.getPathname(new Identifier("id")));
        // without prefix or suffix
        config.setProperty("FilesystemResolver.path_prefix", "");
        config.setProperty("FilesystemResolver.path_suffix", "");
        assertEquals("id", instance.getPathname(new Identifier("id")));
        // with path separator
        String separator = "CATS";
        config.setProperty("FilesystemResolver.path_separator", separator);
        assertEquals("1" + File.separator + "2" + File.separator + "3",
                instance.getPathname(new Identifier("1" + separator + "2" + separator + "3")));
    }

    public void testGetSanitizedIdentifier() {
        // test using / as file separator
        assertEquals("id/", instance.getSanitizedIdentifier("id/../", "/"));
        assertEquals("/id", instance.getSanitizedIdentifier("/../id", "/"));
        // test using \ as file separator
        assertEquals("id\\", instance.getSanitizedIdentifier("id\\..\\", "\\"));
        assertEquals("\\id", instance.getSanitizedIdentifier("\\..\\id", "\\"));
    }

    public void testGetSourceFormatWithExtensions() throws IOException {
        assertEquals(SourceFormat.BMP,
                instance.getSourceFormat(new Identifier("bla.bmp")));
        assertEquals(SourceFormat.GIF,
                instance.getSourceFormat(new Identifier("bla.gif")));
        assertEquals(SourceFormat.JP2,
                instance.getSourceFormat(new Identifier("bla.JP2")));
        assertEquals(SourceFormat.PDF,
                instance.getSourceFormat(new Identifier("bla.pdf")));
        assertEquals(SourceFormat.PNG,
                instance.getSourceFormat(new Identifier("bla.png")));
        assertEquals(SourceFormat.TIF,
                instance.getSourceFormat(new Identifier("bla.tif")));
        try {
            assertEquals(SourceFormat.UNKNOWN,
                    instance.getSourceFormat(new Identifier("bla.bogus")));
            fail("Expected exception");
        } catch (FileNotFoundException e) {
            // pass
        }
    }

    public void testGetSourceFormatByDetection() throws IOException {
        assertEquals(SourceFormat.BMP,
                instance.getSourceFormat(new Identifier("bmp")));
        assertEquals(SourceFormat.GIF,
                instance.getSourceFormat(new Identifier("gif")));
        assertEquals(SourceFormat.JP2,
                instance.getSourceFormat(new Identifier("jp2")));
        assertEquals(SourceFormat.JPG,
                instance.getSourceFormat(new Identifier("jpg")));
        assertEquals(SourceFormat.PDF,
                instance.getSourceFormat(new Identifier("pdf")));
        assertEquals(SourceFormat.PNG,
                instance.getSourceFormat(new Identifier("png")));
        assertEquals(SourceFormat.TIF,
                instance.getSourceFormat(new Identifier("tif")));
        assertEquals(SourceFormat.UNKNOWN,
                instance.getSourceFormat(new Identifier("txt")));
    }

}
