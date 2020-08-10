package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import edu.illinois.library.cantaloupe.resource.Route;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>Functional test of conformance to the IIIF Image API 3.0 spec. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/3.0/">IIIF Image API 3.0</a>
 */
public class Version3_0ConformanceTest extends ResourceTest {

    private static final Identifier IMAGE =
            new Identifier("jpg-rgb-64x56x8-baseline.jpg");

    @Override
    protected String getEndpointPath() {
        return Route.IIIF_3_PATH;
    }

    /**
     * 2. "When the base URI is dereferenced, the interaction should result in
     * the Image Information document. It is recommended that the response be a
     * 303 status redirection to the image information document’s URI."
     */
    @Test
    void testBaseURIReturnsImageInfoViaHttp303() throws Exception {
        client = newClient("/" + IMAGE);
        Response response = client.send();

        assertEquals(303, response.getStatus());
        assertEquals(getHTTPURI("/" + IMAGE + "/info.json").toString(),
                response.getHeaders().getFirstValue("Location"));
    }

    /**
     * 3. "All special characters (e.g. ? or #) must be URI encoded to avoid
     * unpredictable client behaviors. The URI syntax relies upon slash (/)
     * separators so any slashes in the identifier must be URI encoded (also
     * called “percent encoded”)."
     */
    @Test
    void testIdentifierWithEncodedCharacters() throws Exception {
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
        assertStatus(200, getHTTPURI("/" + identifier + "/full/max/0/default.jpg"));
        // information endpoint
        assertStatus(200, getHTTPURI("/" + identifier + "/info.json"));
    }

    /**
     * 4.1
     */
    @Test
    void testFullRegion() throws Exception {
        client = newClient("/" + IMAGE + "/full/max/0/default.jpg");
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
    void testSquareRegion() throws Exception {
        client = newClient("/" + IMAGE + "/square/max/0/default.jpg");
        Response response = client.send();
        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(56, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

    /**
     * 4.1
     */
    @Test
    void testAbsolutePixelRegion() throws Exception {
        client = newClient("/" + IMAGE + "/20,20,100,100/max/0/color.jpg");
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
    void testPercentageRegionWithIntegers() throws Exception {
        client = newClient("/" + IMAGE + "/pct:20,20,50,50/max/0/color.jpg");
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
    void testPercentageRegionWithFloats() throws Exception {
        client = newClient("/" + IMAGE + "/pct:20.2,20.6,50.2,50.6/max/0/color.jpg");
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
    void testAbsolutePixelRegionLargerThanSource() throws Exception {
        client = newClient("/" + IMAGE + "/0,0,99999,99999/max/0/color.jpg");
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
    void testZeroRegion() {
        client = newClient("/" + IMAGE + "/0,0,0,0/max/0/default.jpg");
        ResourceException e = assertThrows(
                ResourceException.class,
                () -> client.send());
        assertEquals(400, e.getStatusCode());
    }

    /**
     * 4.1. "If the requested region ... is entirely outside the bounds of the
     * reported dimensions, then the server should return a 400 status code."
     */
    @Test
    void testXYRegionOutOfBounds() {
        client = newClient("/" + IMAGE + "/99999,99999,50,50/max/0/default.jpg");
        ResourceException e = assertThrows(
                ResourceException.class,
                () -> client.send());
        assertEquals(400, e.getStatusCode());
    }

    /**
     * The IIIF Image API Validator wants the server to return 400 for a bogus
     * (junk characters) region.
     */
    @Test
    void testBogusRegion() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/ca%20ioU/max/0/default.jpg"));
    }

    /**
     * 4.2. (max) "The extracted region is returned at the maximum size
     * available, but will not be upscaled. The resulting image will have the
     * pixel dimensions of the extracted region, unless it is constrained to a
     * smaller size by maxWidth, maxHeight, or maxArea."
     */
    @Test
    void testMaxSize() throws Exception {
        client = newClient("/" + IMAGE + "/full/max/0/color.jpg");
        Response response = client.send();
        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(64, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

    /**
     * 4.2. (^max) "The extracted region is scaled to the maximum size
     * permitted by maxWidth, maxHeight, or maxArea. ... If the resulting
     * dimensions are greater than the pixel width and height of the extracted
     * region, the extracted region is upscaled."
     */
    @Test
    void testMaxSizeWithUpscaling() throws Exception {
        final int maxScale = 2;
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, maxScale);

        client = newClient("/" + IMAGE + "/full/%5Emax/0/color.jpg");
        Response response = client.send();
        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(64 * maxScale, image.getWidth());
            assertEquals(56 * maxScale, image.getHeight());
        }
    }

    /**
     * 4.2. (w,) "The extracted region should be scaled so that its width is
     * exactly equal to w."
     */
    @Test
    void testSizeDownscaledToFitWidth() throws Exception {
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
     * 4.2. (w,) "The value of w must not be greater than the width of the
     * extracted region."
     */
    @Test
    void testSizeDownscaledToFitWidthWithIllegalArgument() {
        client = newClient("/" + IMAGE + "/full/100,/0/color.jpg");
        ResourceException e = assertThrows(
                ResourceException.class,
                () -> client.send());
        assertEquals(400, e.getStatusCode());
    }

    /**
     * 4.2. (^w,) "The extracted region should be scaled so that the width of
     * the returned image is exactly equal to w. If w is greater than the pixel
     * width of the extracted region, the extracted region is upscaled."
     */
    @Test
    void testSizeUpscaledToFitWidth() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 999);

        client = newClient("/" + IMAGE + "/full/%5E100,/0/color.jpg");
        Response response = client.send();
        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(100, image.getWidth());
            assertEquals(88, image.getHeight());
        }
    }

    /**
     * 4.2. (^w,) "Requests for sizes prefixed with ^ that require upscaling
     * should result in a 501 (Not Implemented) status code if the server does
     * not support upscaling."
     *
     * Note that because supporting upscaling is not an on/off switch, but
     * rather a continuum based on {@link Key#MAX_SCALE}, this implementation
     * returns 400 in this situation instead of 501.
     */
    @Test
    void testSizeUpscaledToFitWidthWithoutServerSupport() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 1.0);

        client = newClient("/" + IMAGE + "/full/%5E100,/0/color.jpg");
        ResourceException e = assertThrows(
                ResourceException.class,
                () -> client.send());
        assertEquals(400, e.getStatusCode());
    }

    /**
     * 4.2. (,h) "The extracted region should be scaled so that its height is
     * exactly equal to h."
     */
    @Test
    void testSizeDownscaledToFitHeight() throws Exception {
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
     * 4.2. (,h) "The value of h must not be greater than the height of the
     * extracted region."
     */
    @Test
    void testSizeDownscaledToFitHeightWithIllegalArgument() {
        client = newClient("/" + IMAGE + "/full/,100/0/color.jpg");
        ResourceException e = assertThrows(ResourceException.class,
                () -> client.send());
        assertEquals(400, e.getStatusCode());
    }

    /**
     * 4.2. (^,h) "The extracted region should be scaled so that the height of
     * the returned image is exactly equal to h. If h is greater than the pixel
     * height of the extracted region, the extracted region is upscaled."
     */
    @Test
    void testSizeUpscaledToFitHeight() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 999);

        client = newClient("/" + IMAGE + "/full/%5E,100/0/color.jpg");
        Response response = client.send();
        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(114, image.getWidth());
            assertEquals(100, image.getHeight());
        }
    }

    /**
     * 4.2. (^,h) "Requests for sizes prefixed with ^ that require upscaling
     * should result in a 501 (Not Implemented) status code if the server does
     * not support upscaling."
     *
     * Note that because supporting upscaling is not an on/off switch, but
     * rather a continuum based on {@link Key#MAX_SCALE}, this implementation
     * returns 400 in this situation instead of 501.
     */
    @Test
    void testSizeUpscaledToFitHeightWithoutServerSupport() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 1.0);

        client = newClient("/" + IMAGE + "/full/%5E,100/0/color.jpg");
        ResourceException e = assertThrows(
                ResourceException.class,
                () -> client.send());
        assertEquals(400, e.getStatusCode());
    }

    /**
     * 4.2. (pct:n) "The width and height of the returned image is scaled to n
     * percent of the width and height of the extracted region."
     */
    @Test
    void testSizeDownscaledToPercent() throws Exception {
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
     * 4.2. (pct:n) "The width and height of the returned image is scaled to n
     * percent of the width and height of the extracted region. The value of n
     * must not be greater than 100."
     */
    @Test
    void testSizeToPercentWithIllegalArgument() {
        client = newClient("/" + IMAGE + "/full/pct:110/0/color.jpg");
        ResourceException e = assertThrows(ResourceException.class,
                () -> client.send());
        assertEquals(400, e.getStatusCode());
    }

    /**
     * 4.2. (^pct:n) "The width and height of the returned image is scaled to n
     * percent of the width and height of the extracted region. For values of n
     * greater than 100, the extracted region is upscaled."
     */
    @Test
    void testSizeUpscaledToPercent() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 999);

        client = newClient("/" + IMAGE + "/full/%5Epct:110/0/color.jpg");
        Response response = client.send();
        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(70, image.getWidth());
            assertEquals(62, image.getHeight());
        }
    }

    /**
     * 4.2. (^pct:n) "Requests for sizes prefixed with ^ that require upscaling
     * should result in a 501 (Not Implemented) status code if the server does
     * not support upscaling."
     *
     * Note that because supporting upscaling is not an on/off switch, but
     * rather a continuum based on {@link Key#MAX_SCALE}, this implementation
     * returns 400 in this situation instead of 501.
     */
    @Test
    void testSizeUpscaledToPercentWithoutServerSupport() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 1.0);

        client = newClient("/" + IMAGE + "/full/%5Epct:110/0/color.jpg");
        ResourceException e = assertThrows(
                ResourceException.class,
                () -> client.send());
        assertEquals(400, e.getStatusCode());
    }

    /**
     * 4.2. (w,h) "The width and height of the returned image are exactly w and
     * h. The aspect ratio of the returned image may be significantly different
     * than the extracted region, resulting in a distorted image."
     */
    @Test
    void testAbsoluteWidthAndHeight() throws Exception {
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
     * 4.2. (w,h) "The values of w and h must not be greater than the
     * corresponding pixel dimensions of the extracted region."
     */
    @Test
    void testAbsoluteWidthAndHeightWithIllegalWidth() {
        client = newClient("/" + IMAGE + "/full/100,20/0/color.jpg");

        ResourceException e = assertThrows(
                ResourceException.class,
                () -> client.send());
        assertEquals(400, e.getStatusCode());
    }

    /**
     * 4.2. (w,h) "The values of w and h must not be greater than the
     * corresponding pixel dimensions of the extracted region."
     */
    @Test
    void testAbsoluteWidthAndHeightWithIllegalHeight() {
        client = newClient("/" + IMAGE + "/full/20,100/0/color.jpg");
        ResourceException e = assertThrows(
                ResourceException.class,
                () -> client.send());
        assertEquals(400, e.getStatusCode());
    }

    /**
     * 4.2. (^w,h) "The width and height of the returned image are exactly w
     * and h. The aspect ratio of the returned image may be significantly
     * different than the extracted region, resulting in a distorted image. If
     * w and/or h are greater than the corresponding pixel dimensions of the
     * extracted region, the extracted region is upscaled."
     */
    @Test
    void testUpscaleToAbsoluteWidthAndHeight() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 999);

        client = newClient("/" + IMAGE + "/full/%5E100,100/0/color.jpg");
        Response response = client.send();
        assertEquals(200, response.getStatus());

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(100, image.getWidth());
            assertEquals(100, image.getHeight());
        }
    }

    /**
     * 4.2. (^w,h) "Requests for sizes prefixed with ^ that require upscaling
     * should result in a 501 (Not Implemented) status code if the server does
     * not support upscaling."
     *
     * Note that because supporting upscaling is not an on/off switch, but
     * rather a continuum based on {@link Key#MAX_SCALE}, this implementation
     * returns 400 in this situation instead of 501.
     */
    @Test
    void testUpscaleToAbsoluteWidthAndHeightWithoutServerSupport() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 1.0);

        client = newClient("/" + IMAGE + "/full/%5E100,100/0/color.jpg");
        ResourceException e = assertThrows(
                ResourceException.class,
                () -> client.send());
        assertEquals(400, e.getStatusCode());
    }

    /**
     * 4.2. (!w,h) "The extracted region is scaled so that the width and height
     * of the returned image are not greater than w and h, while maintaining
     * the aspect ratio. The returned image must be as large as possible but
     * not larger than the extracted region, w or h, or server-imposed limits."
     */
    @Test
    void testSizeDownscaledToFitInside() throws Exception {
        client = newClient("/" + IMAGE + "/full/!30,30/0/default.jpg");
        Response response = client.send();

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(30, image.getWidth());
            assertEquals(26, image.getHeight());
        }
    }

    /**
     * 4.2. (^!w,h) "The extracted region is scaled so that the width and
     * height of the returned image are not greater than w and h, while
     * maintaining the aspect ratio. The returned image must be as large as
     * possible but not larger than w, h, or server-imposed limits."
     */
    @Test
    void testSizeUpscaledToFitInside() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 999);

        client = newClient("/" + IMAGE + "/full/%5E!100,100/0/default.jpg");
        Response response = client.send();

        try (InputStream is = new ByteArrayInputStream(response.getBody())) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(100, image.getWidth());
            assertEquals(88, image.getHeight());
        }
    }

    /**
     * 4.2. (^!w,h) "Requests for sizes prefixed with ^ that require upscaling
     * should result in a 501 (Not Implemented) status code if the server does
     * not support upscaling."
     *
     * Note that because supporting upscaling is not an on/off switch, but
     * rather a continuum based on {@link Key#MAX_SCALE}, this implementation
     * returns 200 in this situation instead of 501.
     */
    @Test
    void testSizeUpscaledToFitInsideWithoutServerSupport() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.MAX_SCALE, 1.0);
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/%5E!150,150/0/color.jpg"));
    }

    /**
     * 4.2. "For all requests the pixel dimensions of the scaled region must
     * not be less than 1 pixel or greater than the server-imposed limits.
     * Requests that would generate images of these sizes are errors that
     * should result in a 400 (Bad Request) status code."
     */
    @Test
    void testResultingWidthOrHeightIsZero() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/pct:0/15/color.jpg"));
        assertStatus(400, getHTTPURI("/wide.jpg/full/3,0/15/color.jpg"));
    }

    /**
     * 4.2. "... a 400 (Bad Request) status code should be returned in response
     * to other client request syntax errors."
     */
    @Test
    void testInvalidSize() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/cats/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/cats,50/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/%5Ecats,50/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/50,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/%5E50,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/cats,/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/%5Ecats,/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/%5E,cats/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/!cats,50/0/default.jpg"));
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/%5E!cats,50/0/default.jpg"));
    }

    /**
     * 4.3. "The degrees of clockwise rotation from 0 up to 360."
     */
    @Test
    void testRotation() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/max/0/color.jpg"));
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/max/15.5/color.jpg"));
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/max/360/color.jpg"));
    }

    /**
     * 4.3. "The image should be mirrored and then rotated as above."
     */
    @Test
    void testMirroredRotation() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/max/!15/color.jpg"));
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     */
    @Test
    void testNegativeRotation() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/max/-15/default.jpg"));
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     */
    @Test
    void testGreaterThanFullRotation() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/max/4855/default.jpg"));
    }

    /**
     * 4.4. "The image is returned in full color."
     */
    @Test
    void testColorQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/max/0/color.jpg"));
    }

    /**
     * 4.4. "The image is returned in grayscale, where each pixel is black,
     * white or any shade of gray in between."
     */
    @Test
    void testGrayQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/max/0/gray.jpg"));
    }

    /**
     * 4.4. "The image returned is bitonal, where each pixel is either black or
     * white."
     */
    @Test
    void testBitonalQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/max/0/bitonal.jpg"));
    }

    /**
     * 4.4. "The image is returned using the server’s default quality (e.g.
     * color, gray or bitonal) for the image."
     */
    @Test
    void testDefaultQuality() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/max/0/default.jpg"));
    }

    /**
     * 4.4. "A quality value that is unsupported should result in a 400 status
     * code."
     */
    @Test
    void testUnsupportedQuality() {
        assertStatus(400, getHTTPURI("/" + IMAGE + "/full/max/0/bogus.jpg"));
    }

    /**
     * 4.5
     */
    @Test
    void testFormats() throws Exception {
        testFormat(Format.JPG);
        testFormat(Format.TIF);
        testFormat(Format.PNG);
        testFormat(Format.GIF);
        testFormat(Format.JP2);
        testFormat(Format.PDF);
        testFormat(Format.WEBP);
    }

    private void testFormat(Format outputFormat) throws Exception {
        client = newClient("/" + IMAGE + "/full/max/0/default." +
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
    void testUnsupportedFormat() {
        assertStatus(415, getHTTPURI("/" + IMAGE + "/full/max/0/default.bogus"));
    }

    /**
     * 4.7
     */
    @Test
    void testCanonicalURIInLinkHeader() throws Exception {
        final String path        = "/" + IMAGE + "/pct:50,50,50,50/,50/15/gray.jpg";
        final URI uri            = getHTTPURI(path);
        final String uriStr      = uri.toString();
        final String expectedURI = uriStr.substring(0,
                uriStr.indexOf(IMAGE.toString()) + IMAGE.toString().length()) +
                "/32,28,32,28/57,50/15/gray.jpg";
        client = newClient(path);
        Response response = client.send();

        assertEquals("<" + expectedURI + ">;rel=\"canonical\"",
                response.getHeaders().getFirstValue("Link"));
    }

    /**
     * 5. "Servers must support requests for image information."
     */
    @Test
    void testInformationRequest() {
        assertStatus(200, getHTTPURI("/" + IMAGE + "/info.json"));
    }

    /**
     * 5.1. "If the server receives a request with an Accept header, it should
     * respond following the rules of content negotiation. Note that content
     * types provided in the Accept header of the request may include
     * parameters, for example profile or charset."
     */
    @Test
    void testInformationRequestContentTypeWithAcceptHeader() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        client.getHeaders().set("Accept", "application/ld+json");
        Response response = client.send();
        assertEquals("application/ld+json;charset=UTF-8;profile=\"http://iiif.io/api/image/3/context.json\"",
                response.getHeaders().getFirstValue("Content-Type"));

        client.getHeaders().set("Accept", "application/json");
        response = client.send();
        assertTrue("application/json;charset=UTF-8;profile=\"http://iiif.io/api/image/3/context.json\"".equalsIgnoreCase(
                response.getHeaders().getFirstValue("Content-Type").replace(" ", "")));
    }

    /**
     * 5.1. "If the request does not include an Accept header, the HTTP
     * Content-Type header of the response should have the value
     * application/ld+json (JSON-LD) with the profile parameter given as the
     * context document."
     */
    @Test
    void testInformationRequestContentTypeWithoutAcceptHeader() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        Response response = client.send();

        assertEquals(200, response.getStatus());
        assertTrue("application/ld+json;charset=utf-8;profile=\"http://iiif.io/api/image/3/context.json\"".equalsIgnoreCase(
                response.getHeaders().getFirstValue("Content-Type").replace(" ", "").toLowerCase()));
    }

    /**
     * 5.1. "Servers should support CORS on image information responses."
     */
    @Test
    void testInformationRequestCORSHeader() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");

        Response response = client.send();
        assertEquals("*",
                response.getHeaders().getFirstValue("Access-Control-Allow-Origin"));
    }

    /**
     * 5.2
     */
    @Test
    void testInformationRequestJSON() {
        // this is tested in ImageInfoFactoryTest
    }

}
