package edu.illinois.library.cantaloupe.resource.iiif.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static org.junit.Assert.*;

/**
 * <p>Functional test of conformance to the IIIF Image API 2.0 spec. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/2.0/#image-information">IIIF Image
 * API 2.0</a>
 */
public class Version2_0ConformanceTest extends ResourceTest {

    static final Identifier IMAGE =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    @Override
    protected String getEndpointPath() {
        return RestletApplication.IIIF_2_PATH;
    }

    /**
     * 2. "When the base URI is dereferenced, the interaction should result in
     * the Image Information document. It is recommended that the response be a
     * 303 status redirection to the Image Information document’s URI."
     */
    @Test
    public void testBaseURIReturnsImageInfoViaHttp303() throws Exception {
        client = newClient("/" + IMAGE);
        Response response = client.send();

        assertEquals(303, response.getStatus());
        assertEquals(getHTTPURI("/" + IMAGE + "/info.json").toString(),
                response.getHeaders().getFirstValue("Location"));
    }

    /**
     * 3. "All special characters (e.g. ? or #) [in an identifier] must be URI
     * encoded to avoid unpredictable client behaviors. The URI syntax relies
     * upon slash (/) separators so any slashes in the identifier must be URI
     * encoded (also called “percent encoded”).
     */
    @Test
    public void testIdentifierWithEncodedCharacters() throws Exception {
        // override the filesystem prefix to one folder level up so we can use
        // a slash in the identifier
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path path = Paths.get(cwd, "src", "test", "resources");
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                path + File.separator);

        final String identifier = "images%2F" + IMAGE;

        // image endpoint
        assertStatus(200, getHTTPURI("/" + identifier + "/full/full/0/default.jpg"));
        // information endpoint
        assertStatus(200, getHTTPURI("/" + identifier + "/info.json"));
    }

    /**
     * 4.1
     */
    @Test
    public void testFullRegion() throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/default.jpg");
        Response response = client.send();

        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(64, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

    /**
     * 4.1
     */
    @Test
    public void testAbsolutePixelRegion() throws Exception {
        client = newClient("/" + IMAGE + "/20,20,100,100/full/0/color.jpg");
        Response response = client.send();

        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(44, image.getWidth());
            assertEquals(36, image.getHeight());
        }
    }

    /**
     * 4.1
     */
    @Test
    public void testPercentageRegionWithIntegers() throws Exception {
        client = newClient("/" + IMAGE + "/pct:20,20,50,50/full/0/color.jpg");
        Response response = client.send();
        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(32, image.getWidth());
            assertEquals(28, image.getHeight());
        }
    }

    /**
     * 4.1
     */
    @Test
    public void testPercentageRegionWithFloats() throws Exception {
        client = newClient("/" + IMAGE + "/pct:20.2,20.6,50.2,50.6/full/0/color.jpg");
        Response response = client.send();
        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(32, image.getWidth());
            assertEquals(28, image.getHeight());
        }
    }

    /**
     * 4.1. "If the request specifies a region which extends beyond the
     * dimensions reported in the Image Information document, then the service
     * should return an image cropped at the image’s edge, rather than adding
     * empty space."
     */
    @Test
    public void testAbsolutePixelRegionLargerThanSource() throws Exception {
        client = newClient("/" + IMAGE + "/0,0,99999,99999/full/0/color.jpg");
        Response response = client.send();

        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(64, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

    /**
     * 4.1. "If the requested region’s height or width is zero ... then the
     * server should return a 400 status code."
     */
    @Test
    public void testZeroRegion() throws Exception {
        client = newClient("/" + IMAGE + "/0,0,0,0/full/0/default.jpg");
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    /**
     * 4.1. "If the requested region ... is entirely outside the bounds of the
     * reported dimensions, then the server should return a 400 status code."
     */
    @Test
    public void testXYRegionOutOfBounds() throws Exception {
        client = newClient("/" + IMAGE + "/99999,99999,50,50/full/0/default.jpg");
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    /**
     * The IIIF API Validator wants the server to return 400 for a bogus
     * (junk characters) region.
     */
    @Test
    public void testBogusRegion() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/ca%20ioU/full/0/default.jpg"));
    }

    /**
     * 4.2
     */
    @Test
    public void testFullSize() throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/color.jpg");
        Response response = client.send();

        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(64, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

    /**
     * 4.2. "The extracted region should be scaled so that its width is
     * exactly equal to w, and the height will be a calculated value that
     * maintains the aspect ratio of the extracted region."
     */
    @Test
    public void testSizeScaledToFitWidth() throws Exception {
        client = newClient("/" + IMAGE + "/full/50,/0/color.jpg");
        Response response = client.send();

        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(50, image.getWidth());
            assertEquals(44, image.getHeight());
        }
    }

    /**
     * 4.2. "The extracted region should be scaled so that its height is
     * exactly equal to h, and the width will be a calculated value that
     * maintains the aspect ratio of the extracted region."
     */
    @Test
    public void testSizeScaledToFitHeight() throws Exception {
        client = newClient("/" + IMAGE + "/full/,50/0/color.jpg");
        Response response = client.send();

        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(57, image.getWidth());
            assertEquals(50, image.getHeight());
        }
    }

    /**
     * 4.2. "The width and height of the returned image is scaled to n% of the
     * width and height of the extracted region. The aspect ratio of the
     * returned image is the same as that of the extracted region."
     */
    @Test
    public void testSizeScaledToPercent() throws Exception {
        client = newClient("/" + IMAGE + "/full/pct:50/0/color.jpg");
        Response response = client.send();

        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(32, image.getWidth());
            assertEquals(28, image.getHeight());
        }
    }

    /**
     * 4.2. "The width and height of the returned image are exactly w and h.
     * The aspect ratio of the returned image may be different than the
     * extracted region, resulting in a distorted image."
     */
    @Test
    public void testAbsoluteWidthAndHeight() throws Exception {
        client = newClient("/" + IMAGE + "/full/50,50/0/color.jpg");
        Response response = client.send();

        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(50, image.getWidth());
            assertEquals(50, image.getHeight());
        }
    }

    /**
     * 4.2. "The image content is scaled for the best fit such that the
     * resulting width and height are less than or equal to the requested
     * width and height. The exact scaling may be determined by the service
     * provider, based on characteristics including image quality and system
     * performance. The dimensions of the returned image content are
     * calculated to maintain the aspect ratio of the extracted region."
     */
    @Test
    public void testSizeScaledToFitInside() throws Exception {
        client = newClient("/" + IMAGE + "/full/20,20/0/default.jpg");
        Response response = client.send();

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(20, image.getWidth());
            assertEquals(20, image.getHeight());
        }
    }

    /**
     * 4.2. "If the resulting height or width is zero, then the server should
     * return a 400 (bad request) status code."
     */
    @Test
    public void testResultingWidthOrHeightIsZero() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/pct:0/15/color.jpg"));
        assertStatus(400, getHTTPURI("/wide.jpg/full/3,0/15/color.jpg"));
    }

    /**
     * IIIF Image API 2.0 doesn't say anything about an invalid size
     * parameter, so we will check for an HTTP 400.
     */
    @Test
    public void testInvalidSize() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/cats/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/cats,50/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/50,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/cats,/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/!cats,50/0/default.jpg"));
    }

    /**
     * 4.3. "The degrees of clockwise rotation from 0 up to 360."
     */
    @Test
    public void testRotation() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/15.5/color.jpg"));
    }

    /**
     * 4.3. "The image should be mirrored and then rotated as above."
     */
    @Test
    public void testMirroredRotation() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/!15/color.jpg"));
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     */
    @Test
    public void testNegativeRotation() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/full/-15/default.jpg"));
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     */
    @Test
    public void testGreaterThanFullRotation() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/full/4855/default.jpg"));
    }

    /**
     * 4.4. "The image is returned in full color."
     */
    @Test
    public void testColorQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg"));
    }

    /**
     * 4.4. "The image is returned in grayscale, where each pixel is black,
     * white or any shade of gray in between."
     */
    @Test
    public void testGrayQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/0/gray.jpg"));
    }

    /**
     * 4.4. "The image returned is bitonal, where each pixel is either black or
     * white."
     */
    @Test
    public void testBitonalQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/0/bitonal.jpg"));
    }

    /**
     * 4.4. "The image is returned using the server’s default quality (e.g.
     * color, gray or bitonal) for the image."
     */
    @Test
    public void testDefaultQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/0/default.jpg"));
    }

    /**
     * 4.4. "A quality value that is unsupported should result in a 400 status
     * code."
     */
    @Test
    public void testUnsupportedQuality() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/full/0/bogus.jpg"));
    }

    /**
     * 4.5
     */
    @Test
    public void testFormats() throws Exception {
        testFormat(Format.JPG);
        testFormat(Format.TIF);
        testFormat(Format.PNG);
        testFormat(Format.GIF);
        testFormat(Format.JP2);
        testFormat(Format.PDF);
        testFormat(Format.WEBP);
    }

    private void testFormat(Format outputFormat) throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/default." +
                outputFormat.getPreferredExtension());

        final Format sourceFormat = Format.inferFormat(IMAGE);
        final Processor processor = new ProcessorFactory().newProcessor(sourceFormat);
        final Set<Format> outputFormats = processor.getAvailableOutputFormats();

        // If the processor supports this SOURCE format
        if (!outputFormats.isEmpty()) {
            // If the processor supports this OUTPUT format
            if (outputFormats.contains(outputFormat)) {
                Response response = client.send();
                assertEquals(200, response.getStatus());
                assertEquals(outputFormat.getPreferredMediaType().toString(),
                        response.getHeaders().getFirstValue("Content-Type"));
            } else {
                try {
                    client.send();
                    fail("Expected exception");
                } catch (ResourceException e) {
                    assertEquals(415, e.getStatusCode());
                }
            }
        } else {
            try {
                client.send();
                fail("Expected exception");
            } catch (ResourceException e) {
                assertEquals(501, e.getStatusCode());
            }
        }
    }

    /**
     * 4.5
     */
    @Test
    public void testUnsupportedFormat() {
        assertStatus(415, getHTTPURI("/" + IMAGE + "/full/full/0/default.bogus"));
    }

    /**
     * 4.7. "When the client requests an image, the server may add a link
     * header to the response that indicates the canonical URI for that
     * request."
     */
    @Test
    public void testCanonicalUriLinkHeader() throws Exception {
        final String path = "/" + IMAGE + "/full/full/0/default.jpg";
        final URI uri = getHTTPURI(path);
        client = newClient(path);
        Response response = client.send();

        assertEquals("<" + uri + ">;rel=\"canonical\"",
                response.getHeaders().getFirstValue("Link"));
    }

    /**
     * 5. "The service must return this information about the image."
     */
    @Test
    public void testInformationRequest() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/info.json"));
    }

    /**
     * 5. "The content-type of the response must be either “application/json”,
     * (regular JSON), or “application/ld+json” (JSON-LD)."
     */
    @Test
    public void testInformationRequestContentType() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        Response response = client.send();

        assertEquals(200, response.getStatus());
        assertEquals("application/json;charset=utf-8",
                response.getHeaders().getFirstValue("Content-Type").replace(" ", "").toLowerCase());
    }

    /**
     * 5. "If the client explicitly wants the JSON-LD content-type, then it
     * must specify this in an Accept header, otherwise the server must return
     * the regular JSON content-type."
     */
    @Test
    public void testInformationRequestContentTypeJSONLD() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        client.getHeaders().set("Accept", "application/ld+json");
        Response response = client.send();
        assertEquals("application/ld+json; charset=UTF-8",
                response.getHeaders().getFirstValue("Content-Type"));

        client.getHeaders().set("Accept", "application/json");
        response = client.send();
        assertEquals("application/json;charset=utf-8",
                response.getHeaders().getFirstValue("Content-Type").replace(" ", "").toLowerCase());
    }

    /**
     * 5. "Servers should send the Access-Control-Allow-Origin header with the
     * value * in response to information requests."
     */
    @Test
    public void testInformationRequestCORSHeader() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        client.getHeaders().set("Origin", "*");

        Response response = client.send();
        assertEquals("*",
                response.getHeaders().getFirstValue("Access-Control-Allow-Origin"));
    }

    /**
     * 5.1
     */
    @Test
    public void testInformationRequestJSON() {
        // this will be tested in ImageInfoFactoryTest
    }

    /**
     * 5.1. "If any of formats, qualities, or supports have no additional
     * values beyond those specified in the referenced compliance level, then
     * the property should be omitted from the response rather than being
     * present with an empty list."
     */
    @Test
    public void testInformationRequestEmptyJSONProperties() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        Response response = client.send();
        assertFalse(response.getBodyAsString().contains("null"));
    }

    /**
     * 6. "The Image Information document must ... include a compliance level
     * URI as the first entry in the profile property."
     */
    @Test
    public void testComplianceLevel() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        Response response = client.send();
        String json = response.getBodyAsString();

        ObjectMapper mapper = new ObjectMapper();
        ImageInfo<?, ?> info = mapper.readValue(json, ImageInfo.class);
        List<?> profile = (List<?>) info.get("profile");
        assertEquals("http://iiif.io/api/image/2/level2.json",
                profile.get(0));
    }

}
