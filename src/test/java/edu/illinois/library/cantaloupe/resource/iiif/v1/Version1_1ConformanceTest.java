package edu.illinois.library.cantaloupe.resource.iiif.v1;

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
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static org.junit.Assert.*;

/**
 * <p>Functional test of conformance to the IIIF Image API 1.1 spec. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/1.1/#image-info-request">IIIF Image
 * API 1.1</a>
 */
public class Version1_1ConformanceTest extends ResourceTest {

    private static final Identifier IMAGE =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    @Override
    protected String getEndpointPath() {
        return RestletApplication.IIIF_1_PATH;
    }

    /**
     * 2.2. "It is recommended that if the image’s base URI is dereferenced,
     * then the client should either redirect to the information request using
     * a 303 status code (see Section 6.1), or return the same result."
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
     * 3. "the identifier MUST be expressed as a string. All special characters
     * (e.g. ? or #) MUST be URI encoded to avoid unpredictable client
     * behaviors. The URL syntax relies upon slash (/) separators so any
     * slashes in the identifier MUST be URI encoded (aka. percent-encoded,
     * replace / with %2F )."
     */
    @Test
    public void testIdentifierWithEncodedCharacters() throws Exception {
        // override the filesystem prefix to one folder level up so we can use
        // a slash in the identifier
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path path = Paths.get(cwd, "src", "test", "resources");
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX,
                path + File.separator);

        final String identifier = "images%2F" + IMAGE;

        // image endpoint
        assertStatus(200, getHTTPURI("/" + identifier + "/full/full/0/native.jpg"));
        // information endpoint
        assertStatus(200, getHTTPURI("/" + identifier + "/info.json"));
    }

    /**
     * 4.1
     */
    @Test
    public void testFullRegion() throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/native.jpg");
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
     * dimensions of the source image, then the service should return an image
     * cropped at the boundary of the source image."
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
     * 4.1. "If the requested region's height or width is zero ... then the
     * server MUST return a 400 (bad request) status code."
     */
    @Test
    public void testZeroRegion() throws Exception {
        client = newClient("/" + IMAGE + "/0,0,0,0/full/0/native.jpg");
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    /**
     * 4.1. "If the region is entirely outside the bounds of the source image,
     * then the server MUST return a 400 (bad request) status code."
     */
    @Test
    public void testXYRegionOutOfBounds() throws Exception {
        client = newClient("/" + IMAGE + "/99999,99999,50,50/full/0/native.jpg");
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(400, e.getStatusCode());
        }
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
     * maintains the aspect ratio of the requested region."
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
     * maintains the aspect ratio of the requested region."
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
     * The aspect ratio of the returned image MAY be different than the
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
     * resulting width and height are less than or equal to the requested width
     * and height. The exact scaling MAY be determined by the service provider,
     * based on characteristics including image quality and system performance.
     * The dimensions of the returned image content are calculated to maintain
     * the aspect ratio of the extracted region."
     */
    @Test
    public void testSizeScaledToFitInside() throws Exception {
        client = newClient("/" + IMAGE + "/full/20,20/0/native.jpg");
        Response response = client.send();

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(20, image.getWidth());
            assertEquals(20, image.getHeight());
        }
    }

    /**
     * 4.2. "If the resulting height or width is zero, then the server MUST
     * return a 400 (bad request) status code."
     */
    @Test
    public void testResultingWidthOrHeightIsZero() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/pct:0/15/color.jpg"));
        assertStatus(400, getHTTPURI("/wide.jpg/full/3,0/15/color.jpg"));
    }

    /**
     * IIIF Image API 1.1 doesn't say anything about an invalid size
     * parameter, so we will check for an HTTP 400.
     */
    @Test
    public void testInvalidSize() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/cats/0/native.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/cats,50/0/native.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/50,cats/0/native.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/cats,/0/native.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/,cats/0/native.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/!cats,50/0/native.jpg"));
    }

    /**
     * 4.3. "The rotation value represents the number of degrees of clockwise
     * rotation from the original, and may be any floating point number from 0
     * to 360. Initially most services will only support 0, 90, 180 or 270 as
     * valid values."
     */
    @Test
    public void testRotation() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/15.5/color.jpg"));
    }

    /**
     * IIIF Image API 1.1 doesn't say anything about a negative rotation
     * parameter, so we will check for an HTTP 400.
     */
    @Test
    public void testNegativeRotation() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/full/-15/native.jpg"));
    }

    /**
     * IIIF Image API 1.1 doesn't say anything about a >360-degree rotation
     * parameter, so we will check for an HTTP 400.
     */
    @Test
    public void testGreaterThanFullRotation() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/full/4855/native.jpg"));
    }

    /**
     * 4.4. "The image is returned at an unspecified bit-depth."
     */
    @Test
    public void testNativeQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/0/native.jpg"));
    }

    /**
     * 4.4. "The image is returned in full color, typically using 24 bits per
     * pixel."
     */
    @Test
    public void testColorQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg"));
    }

    /**
     * 4.4. "The image is returned in greyscale, where each pixel is black,
     * white or any degree of grey in between, typically using 8 bits per
     * pixel."
     */
    @Test
    public void testGrayQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/0/gray.jpg"));
    }

    /**
     * 4.4. "The image returned is bitonal, where each pixel is either black or
     * white, using 1 bit per pixel when the format permits."
     */
    @Test
    public void testBitonalQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/0/bitonal.jpg"));
    }

    /**
     * The IIIF Image API 1.1 doesn't say anything about unsupported qualities,
     * so we will check for an HTTP 400.
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
    }

    private void testFormat(Format outputFormat) throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/native." +
                outputFormat.getPreferredExtension());

        final Format sourceFormat = Format.inferFormat(IMAGE);
        try (Processor processor = new ProcessorFactory().newProcessor(sourceFormat)) {
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
    }

    /**
     * 4.5
     */
    @Test
    public void testUnsupportedFormat() {
        assertStatus(415, getHTTPURI("/" + IMAGE + "/full/full/0/native.bogus"));
    }

    /**
     * 4.5 "If the format is not specified in the URI, then the server SHOULD
     * use the HTTP Accept header to determine the client’s preferences for the
     * format. The server may either do 200 (return the representation in the
     * response) or 30x (redirect to the correct URI with a format extension)
     * style content negotiation."
     */
    @Test
    public void testFormatInAcceptHeader() throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/native");
        client.getHeaders().set("Accept", "image/png");
        Response response = client.send();

        assertEquals(200, response.getStatus());
        assertEquals("image/png",
                response.getHeaders().getFirstValue("Content-Type"));
    }

    /**
     * 4.5 "If neither [format in URL or in Accept header] are given, then the
     * server should use a default format of its own choosing."
     */
    @Test
    public void testNoFormat() throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/native");
        client.getHeaders().set("Accept", "*/*");
        Response response = client.send();

        assertEquals(200, response.getStatus());
        assertEquals("image/jpeg",
                response.getHeaders().getFirstValue("Content-Type"));
    }

    /**
     * 5. "The service MUST return technical information about the requested
     * image in the JSON format."
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
     * 5.
     */
    @Test
    public void testInformationRequestJSON() {
        // this will be tested in ImageInfoFactoryTest
    }

    /**
     * 6.2 "Requests are limited to 1024 characters."
     */
    @Test
    public void testURITooLong() {
        // information endpoint
        String uriStr = "/" + IMAGE + "/info.json?bogus=";
        uriStr = StringUtils.rightPad(uriStr, 1025, "a");
        assertStatus(414, getHTTPURI(uriStr));

        // image endpoint
        uriStr = "/" + IMAGE + "/full/full/0/native.jpg?bogus=";
        uriStr = StringUtils.rightPad(uriStr, 1025, "a");
        assertStatus(414, getHTTPURI(uriStr));
    }

    /**
     * 8. "A service should specify on all responses the extent to which the
     * API is supported. This is done by including an HTTP Link header
     * (RFC5988) entry pointing to the description of the highest level of
     * conformance of which ALL of the requirements are met."
     */
    @Test
    public void testComplianceLevelLinkHeaderInInformationResponse()
            throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        Response response = client.send();
        assertEquals("<http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2>;rel=\"profile\";",
                response.getHeaders().getFirstValue("Link"));
    }

    /**
     * 8. "A service should specify on all responses the extent to which the
     * API is supported. This is done by including an HTTP Link header
     * (RFC5988) entry pointing to the description of the highest level of
     * conformance of which ALL of the requirements are met."
     */
    @Test
    public void testComplianceLevelLinkHeaderInImageResponse()
            throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/native.jpg");
        Response response = client.send();
        assertEquals("<http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2>;rel=\"profile\";",
                response.getHeaders().getFirstValue("Link"));
    }

}
