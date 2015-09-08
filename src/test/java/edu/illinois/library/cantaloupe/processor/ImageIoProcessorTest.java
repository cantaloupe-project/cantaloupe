package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Quality;
import junit.framework.TestCase;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImageIoProcessorTest extends TestCase {

    ImageIoProcessor instance;

    public void setUp() {
        instance = new ImageIoProcessor();
    }

    public void testGetAvailableOutputFormats() {
        Set<OutputFormat> expectedFormats = ImageIoProcessor.
                getAvailableOutputFormats().get(SourceFormat.JPG);
        assertEquals(expectedFormats,
                instance.getAvailableOutputFormats(SourceFormat.JPG));
    }

    public void testGetAvailableOutputFormatsForUnsupportedSourceFormat() {
        Set<OutputFormat> expectedFormats = new HashSet<OutputFormat>();
        assertEquals(expectedFormats,
                instance.getAvailableOutputFormats(SourceFormat.UNKNOWN));
    }

    public void testGetImageInfoWithFile() throws Exception {
        // get an ImageInfo representing an image file
        File file = getFixture("escher_lego.jpg");
        SourceFormat sourceFormat = SourceFormat.JPG;
        String baseUri = "http://example.org/base/";
        ImageInfo info = instance.getImageInfo(file, sourceFormat, baseUri);
        testGetImageInfo(info, baseUri);
    }

    public void testGetImageInfoWithInputStream() throws Exception {
        // get an ImageInfo representing an image file
        File file = getFixture("escher_lego.jpg");
        InputStream is = new FileInputStream(file);
        SourceFormat sourceFormat = SourceFormat.JPG;
        String baseUri = "http://example.org/base/";
        ImageInfo info = instance.getImageInfo(is, sourceFormat, baseUri);
        testGetImageInfo(info, baseUri);
    }

    private void testGetImageInfo(ImageInfo info, String baseUri)
            throws Exception {
        assertEquals("http://iiif.io/api/image/2/context.json", info.getContext());
        assertEquals(baseUri, info.getId());
        assertEquals("http://iiif.io/api/image", info.getProtocol());
        assertEquals(594, (int) info.getWidth());
        assertEquals(522, (int) info.getHeight());

        List<Map<String, Integer>> sizes = info.getSizes();
        assertNull(sizes);

        List<Map<String,Object>> tiles = info.getTiles();
        assertNull(tiles);

        List<Object> profile = info.getProfile();
        assertEquals("http://iiif.io/api/image/2/level2.json", profile.get(0));

        Set<String> actualFormats = (Set<String>)((Map)profile.get(1)).get("formats");

        Set<OutputFormat> outputFormats = ImageIoProcessor.
                getAvailableOutputFormats().get(SourceFormat.JPG);
        Set<String> expectedFormats = new HashSet<String>();
        for (OutputFormat format : outputFormats) {
            expectedFormats.add(format.getExtension());
        }
        assertEquals(expectedFormats, actualFormats);

        Set<String> actualQualities = (Set<String>)((Map)profile.get(1)).get("qualities");
        Set<String> expectedQualities = new HashSet<String>();
        for (Quality quality : Quality.values()) {
            expectedQualities.add(quality.toString().toLowerCase());
        }
        assertEquals(expectedQualities, actualQualities);

        Set<String> actualSupports = (Set<String>)((Map)profile.get(1)).get("supports");
        Set<String> expectedSupports = new HashSet<String>();
        expectedSupports.add("baseUriRedirect");
        expectedSupports.add("canonicalLinkHeader");
        expectedSupports.add("cors");
        expectedSupports.add("jsonldMediaType");
        expectedSupports.add("mirroring");
        expectedSupports.add("regionByPx");
        expectedSupports.add("rotationArbitrary");
        expectedSupports.add("rotationBy90s");
        expectedSupports.add("sizeAboveFull");
        expectedSupports.add("sizeByWhListed");
        expectedSupports.add("sizeByForcedWh");
        expectedSupports.add("sizeByH");
        expectedSupports.add("sizeByPct");
        expectedSupports.add("sizeByW");
        expectedSupports.add("sizeWh");
        assertEquals(expectedSupports, actualSupports);
    }

    public void testGetSupportedSourceFormats() {
        // TODO: write this
    }

    public void testProcess() {
        // This is not easily testable in code, so will have to be tested by
        // human eyes.
    }

    private File getFixture(String filename) throws IOException {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                "illinois", "library", "cantaloupe", "test", "fixtures");
        return new File(testPath + File.separator + filename);
    }

}
