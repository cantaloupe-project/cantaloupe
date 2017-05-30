package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

    private String getBaseUri() {
        return "http://localhost:" + PORT + WebApplication.IIIF_1_PATH;
    }

    /**
     * 2.2. "It is recommended that if the image’s base URI is dereferenced,
     * then the client should either redirect to the information request using
     * a 303 status code (see Section 6.1), or return the same result."
     *
     * @throws IOException
     */
    @Test
    public void testBaseUriReturnsImageInfoViaHttp303() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE);
        client.setFollowingRedirects(false);
        client.get();
        assertEquals(Status.REDIRECTION_SEE_OTHER, client.getStatus());
        assertEquals(getBaseUri() + "/" + IMAGE + "/info.json",
                client.getLocationRef().toString());
    }

    /**
     * 3. "the identifier MUST be expressed as a string. All special characters
     * (e.g. ? or #) MUST be URI encoded to avoid unpredictable client
     * behaviors. The URL syntax relies upon slash (/) separators so any
     * slashes in the identifier MUST be URI encoded (aka. percent-encoded,
     * replace / with %2F )."
     *
     * @throws IOException
     */
    @Test
    public void testIdentifierWithEncodedCharacters() throws IOException {
        // override the filesystem prefix to one folder level up so we can use
        // a slash in the identifier
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path path = Paths.get(cwd, "src", "test", "resources");
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX,
                path + File.separator);

        // image endpoint
        String identifier = Reference.encode("images/" + IMAGE);
        ClientResource client = getClientForUriPath("/iiif/1/" + identifier + "/full/full/0/native.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        // information endpoint
        client = getClientForUriPath("/iiif/1/" + identifier + "/info.json");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.1
     *
     * @throws IOException
     */
    @Test
    public void testFullRegion() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/native.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    /**
     * 4.1
     *
     * @throws IOException
     */
    @Test
    public void testAbsolutePixelRegion() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/20,20,100,100/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(44, image.getWidth());
        assertEquals(36, image.getHeight());
    }

    /**
     * 4.1
     *
     * @throws IOException
     */
    @Test
    public void testPercentageRegion() throws IOException {
        // with ints
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/pct:20,20,50,50/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(32, image.getWidth());
        assertEquals(28, image.getHeight());

        // with floats
        client = getClientForUriPath("/iiif/1/" + IMAGE + "/pct:20.2,20.6,50.2,50.6/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        rep = client.getResponseEntity();
        image = ImageIO.read(rep.getStream());
        assertEquals(32, image.getWidth());
        assertEquals(28, image.getHeight());
    }

    /**
     * 4.1. "If the request specifies a region which extends beyond the
     * dimensions of the source image, then the service should return an image
     * cropped at the boundary of the source image."
     *
     * @throws IOException
     */
    @Test
    public void testAbsolutePixelRegionLargerThanSource() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/0,0,99999,99999/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    /**
     * 4.1. "If the requested region's height or width is zero, or if the
     * region is entirely outside the bounds of the source image, then the
     * server MUST return a 400 (bad request) status code."
     *
     * @throws IOException
     */
    @Test
    public void testPixelRegionOutOfBounds() throws IOException {
        // zero width/height
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/0,0,0,0/full/0/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }

        // x/y out of bounds
        client = getClientForUriPath("/iiif/1/" + IMAGE + "/99999,99999,50,50/full/0/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * 4.2
     *
     * @throws IOException
     */
    @Test
    public void testFullSize() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
    }

    /**
     * 4.2. "The extracted region should be scaled so that its width is
     * exactly equal to w, and the height will be a calculated value that
     * maintains the aspect ratio of the requested region."
     *
     * @throws IOException
     */
    @Test
    public void testSizeScaledToFitWidth() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/50,/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(50, image.getWidth());
        assertEquals(44, image.getHeight());
    }

    /**
     * 4.2. "The extracted region should be scaled so that its height is
     * exactly equal to h, and the width will be a calculated value that
     * maintains the aspect ratio of the requested region."
     *
     * @throws IOException
     */
    @Test
    public void testSizeScaledToFitHeight() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/,50/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(57, image.getWidth());
        assertEquals(50, image.getHeight());
    }

    /**
     * 4.2. "The width and height of the returned image is scaled to n% of the
     * width and height of the extracted region. The aspect ratio of the
     * returned image is the same as that of the extracted region."
     *
     * @throws IOException
     */
    @Test
    public void testSizeScaledToPercent() throws IOException {
        ClientResource client = getClientForUriPath(
                "/iiif/1/" + IMAGE + "/full/pct:50/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(32, image.getWidth());
        assertEquals(28, image.getHeight());
    }

    /**
     * 4.2. "The width and height of the returned image are exactly w and h.
     * The aspect ratio of the returned image MAY be different than the
     * extracted region, resulting in a distorted image."
     *
     * @throws IOException
     */
    @Test
    public void testAbsoluteWidthAndHeight() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/50,50/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(50, image.getWidth());
        assertEquals(50, image.getHeight());
    }

    /**
     * 4.2. "The image content is scaled for the best fit such that the
     * resulting width and height are less than or equal to the requested width
     * and height. The exact scaling MAY be determined by the service provider,
     * based on characteristics including image quality and system performance.
     * The dimensions of the returned image content are calculated to maintain
     * the aspect ratio of the extracted region."
     *
     * @throws IOException
     */
    @Test
    public void testSizeScaledToFitInside() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/20,20/0/native.jpg");
        client.get();
        Representation rep = client.getResponseEntity();
        BufferedImage image = ImageIO.read(rep.getStream());
        assertEquals(20, image.getWidth());
        assertEquals(20, image.getHeight());
    }

    /**
     * 4.2. "If the resulting height or width is zero, then the server MUST
     * return a 400 (bad request) status code."
     *
     * @throws IOException
     */
    @Test
    public void testResultingWidthOrHeightIsZero() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/pct:0/15/color.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }

        client = getClientForUriPath("/iiif/1/wide.jpg/full/3,0/15/color.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * IIIF Image API 1.1 doesn't say anything about an invalid size
     * parameter, so we will check for an HTTP 400.
     *
     * @throws IOException
     */
    @Test
    public void testInvalidSize() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/cats/0/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }

        client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/cats,50/0/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }

        client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/50,cats/0/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }

        client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/cats,/0/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }

        client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/,cats/0/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }

        client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/!cats,50/0/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        } finally {
            client.release();
        }
    }

    /**
     * 4.3. "The rotation value represents the number of degrees of clockwise
     * rotation from the original, and may be any floating point number from 0
     * to 360. Initially most services will only support 0, 90, 180 or 270 as
     * valid values."
     *
     * @throws IOException
     */
    @Test
    public void testRotation() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/15.5/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * IIIF Image API 1.1 doesn't say anything about an invalid rotation
     * parameter, so we will check for an HTTP 400.
     *
     * @throws IOException
     */
    @Test
    public void testInvalidRotation() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/-15/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }

        client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/385/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * 4.4. "The image is returned at an unspecified bit-depth."
     *
     * @throws IOException
     */
    @Test
    public void testNativeQuality() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/native.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.4. "The image is returned in full color, typically using 24 bits per
     * pixel."
     *
     * @throws IOException
     */
    @Test
    public void testColorQuality() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.4. "The image is returned in greyscale, where each pixel is black,
     * white or any degree of grey in between, typically using 8 bits per
     * pixel."
     *
     * @throws IOException
     */
    @Test
    public void testGrayQuality() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/gray.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.4. "The image returned is bitonal, where each pixel is either black or
     * white, using 1 bit per pixel when the format permits."
     *
     * @throws IOException
     */
    @Test
    public void testBitonalQuality() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/bitonal.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * The IIIF Image API 1.1 doesn't say anything about unsupported qualities,
     * so we will check for an HTTP 400.
     *
     * @throws IOException
     */
    @Test
    public void testUnsupportedQuality() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/bogus.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * 4.5
     *
     * @throws IOException
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

    private void testFormat(Format format) throws Exception {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE +
                "/full/full/0/native." + format.getPreferredExtension());

        // does the current processor support this output format?
        Format sourceFormat = Format.inferFormat(IMAGE);
        Processor processor = ProcessorFactory.getProcessor(sourceFormat);
        if (processor.getAvailableOutputFormats().contains(format)) {
            client.get();
            assertEquals(Status.SUCCESS_OK, client.getStatus());
            assertEquals(format.getPreferredMediaType().toString(),
                    client.getResponse().getHeaders().getFirst("Content-Type").getValue());
        } else {
            try {
                client.get();
                fail("Expected exception");
            } catch (ResourceException e) {
                assertEquals(Status.SERVER_ERROR_INTERNAL, client.getStatus());
            }
        }
    }

    /**
     * 4.5
     *
     * @throws IOException
     */
    @Test
    public void testUnsupportedFormat() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/native.bogus");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * 4.5 "If the format is not specified in the URI, then the server SHOULD
     * use the HTTP Accept header to determine the client’s preferences for the
     * format. The server may either do 200 (return the representation in the
     * response) or 30x (redirect to the correct URI with a format extension)
     * style content negotiation."
     */
    @Test
    public void testFormatInAcceptHeader() {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/native");
        client.accept(MediaType.IMAGE_PNG);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        assertEquals(MediaType.IMAGE_PNG.toString(),
                client.getResponse().getHeaders().getFirst("Content-Type").getValue());
    }

    /**
     * 4.5 "If neither [format in URL or in Accept header] are given, then the
     * server should use a default format of its own choosing."
     */
    @Test
    public void testNoFormat() {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/native");
        client.accept(MediaType.ALL);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        // TODO: this is kind of brittle
        List<String> mediaTypes = new ArrayList<>();
        mediaTypes.add("image/gif");
        mediaTypes.add("image/jpeg");
        mediaTypes.add("image/png");
        mediaTypes.add("image/tiff");
        assertTrue(mediaTypes.contains(
                client.getResponse().getHeaders().getFirst("Content-Type").getValue()));
    }

    /**
     * 5. "The service MUST return technical information about the requested
     * image in the JSON format."
     *
     * @throws IOException
     */
    @Test
    public void testInformationRequest() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/info.json");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 5. "The content-type of the response must be either “application/json”,
     * (regular JSON), or “application/ld+json” (JSON-LD)."
     *
     * @throws IOException
     */
    @Test
    public void testInformationRequestContentType() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/info.json");
        client.get();
        assertEquals("application/json;charset=utf-8",
                client.getResponse().getHeaders().getFirst("Content-Type").
                        getValue().replace(" ", "").toLowerCase());
    }

    /**
     * 5.
     *
     * @throws IOException
     */
    @Test
    public void testInformationRequestJson() throws IOException {
        // this will be tested in InformationResourceTest
    }

    /**
     * 6.2 "Requests are limited to 1024 characters."
     *
     * @throws IOException
     */
    @Test
    public void testUriTooLong() throws IOException {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/info.json?bogus=");
        Reference uri = client.getReference();
        String uriStr = StringUtils.rightPad(uri.toString(), 1025, "a");
        client.setReference(new Reference(uriStr));

        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_REQUEST_URI_TOO_LONG,
                    client.getStatus());
        }

        client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/native.jpg?bogus=");
        uri = client.getReference();
        uriStr = StringUtils.rightPad(uri.toString(), 1025, "a");
        client.setReference(new Reference(uriStr));

        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_REQUEST_URI_TOO_LONG,
                    client.getStatus());
        }
    }

    /**
     * 8. "A service should specify on all responses the extent to which the
     * API is supported. This is done by including an HTTP Link header
     * (RFC5988) entry pointing to the description of the highest level of
     * conformance of which ALL of the requirements are met."
     */
    @Test
    public void testComplianceLevelLinkHeader() {
        ClientResource client = getClientForUriPath("/iiif/1/" + IMAGE + "/info.json");
        client.get();
        assertEquals("<http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2>;rel=\"profile\";",
                client.getResponse().getHeaders().getFirst("Link").getValue());

        client = getClientForUriPath("/iiif/1/" + IMAGE + "/full/full/0/native.jpg");
        client.get();
        assertEquals("<http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2>;rel=\"profile\";",
                client.getResponse().getHeaders().getFirst("Link").getValue());
    }

}
