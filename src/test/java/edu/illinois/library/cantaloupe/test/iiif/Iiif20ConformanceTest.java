package edu.illinois.library.cantaloupe.test.iiif;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.ImageServerApplication;
import edu.illinois.library.cantaloupe.image.ImageInfo;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;
import org.junit.AfterClass;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>Functional test of the ImageMagickProcessor's conformance to the IIIF 2.0
 * API spec. Methods are implemented in the order of the assertions in the spec
 * document.</p>
 *
 * <p>This test suite only looks at HTTP responses; it doesn't try to analyze
 * the returned images for correctness.</p>
 *
 * @see <a href="http://iiif.io/api/image/2.0/#image-information">IIIF Image
 * API 2.0</a>
 */
public class Iiif20ConformanceTest extends TestCase {

    private static final String IMAGE = "escher_lego.jpg";
    private static final Integer PORT = 34852;

    private static Client client = new Client(new Context(), Protocol.HTTP);

    /**
     * Initializes the Restlet application
     */
    static { // TODO: why doesn't this code work in a @BeforeClass?
        try {
            Application.setConfiguration(getConfiguration());
            Application.start();
        } catch (Exception e) {
            fail("Failed to start the Restlet");
        }
    }

    public static BaseConfiguration getConfiguration() {
        BaseConfiguration config = new BaseConfiguration();
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path fixturePath = Paths.get(cwd, "src", "test", "resources");
            config.setProperty("print_stack_trace_on_error_pages", false);
            config.setProperty("http.port", PORT);
            config.setProperty("processor.fallback", "ImageIoProcessor");
            config.setProperty("resolver", "FilesystemResolver");
            config.setProperty("FilesystemResolver.path_prefix", fixturePath + File.separator);
        } catch (Exception e) {
            fail("Failed to get the configuration");
        }
        return config;
    }

    @AfterClass
    public void afterClass() throws Exception {
        Application.stop();
    }

    public void setUp() {
        BaseConfiguration config = getConfiguration();
        Application.setConfiguration(config);
    }

    /**
     * 2. "When the base URI is dereferenced, the interaction should result in
     * the Image Information document. It is recommended that the response be a
     * 303 status redirection to the Image Information document’s URI."
     *
     * @throws IOException
     */
    public void testBaseUriReturnsImageInfoViaHttp303() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE);
        client.setFollowingRedirects(false);
        client.get();
        assertEquals(Status.REDIRECTION_SEE_OTHER, client.getStatus());
        assertEquals(getBaseUri() + "/" + IMAGE + "/info.json",
                client.getLocationRef().toString());
    }

    /**
     * 3. "All special characters (e.g. ? or #) [in an identifier] must be URI
     * encoded to avoid unpredictable client behaviors. The URI syntax relies
     * upon slash (/) separators so any slashes in the identifier must be URI
     * encoded (also called “percent encoded”).
     *
     * @throws IOException
     */
    public void testIdentifierWithEncodedCharacters() throws IOException {
        // override the filesystem prefix to one folder level up so we can use
        // a slash in the identifier
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path path = Paths.get(cwd, "src", "test");
        BaseConfiguration config = getConfiguration();
        config.setProperty("FilesystemResolver.path_prefix", path + File.separator);
        Application.setConfiguration(config);

        String identifier = Reference.encode("resources/" + IMAGE);
        ClientResource client = getClientForUriPath("/" + identifier + "/info.json");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.1
     *
     * @throws IOException
     */
    public void testFullRegion() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.1
     *
     * @throws IOException
     */
    public void testAbsolutePixelRegion() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/20,20,100,100/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.1
     *
     * @throws IOException
     */
    public void testPercentageRegion() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/pct:20,20,50,50/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        client = getClientForUriPath("/" + IMAGE + "/pct:20.2,20.6,50.2,50.6/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.1. "If the request specifies a region which extends beyond the
     * dimensions reported in the Image Information document, then the service
     * should return an image cropped at the image’s edge, rather than adding
     * empty space."
     *
     * @throws IOException
     */
    public void testAbsolutePixelRegionLargerThanSource() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/0,0,99999,99999/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.1. "If the requested region’s height or width is zero, or if the
     * region is entirely outside the bounds of the reported dimensions, then
     * the server should return a 400 status code."
     *
     * @throws IOException
     */
    public void testPixelRegionOutOfBounds() throws IOException {
        // zero width/height
        ClientResource client = getClientForUriPath("/" + IMAGE + "/0,0,0,0/full/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }

        // x/y out of bounds
        client = getClientForUriPath("/" + IMAGE + "/99999,99999,50,50/full/0/default.jpg");
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
    public void testFullSize() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.2. "The extracted region should be scaled so that its width is
     * exactly equal to w, and the height will be a calculated value that
     * maintains the aspect ratio of the extracted region."
     *
     * @throws IOException
     */
    public void testSizeScaledToFitWidth() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/50,/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.2. "The extracted region should be scaled so that its height is
     * exactly equal to h, and the width will be a calculated value that
     * maintains the aspect ratio of the extracted region."
     *
     * @throws IOException
     */
    public void testSizeScaledToFitHeight() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/,50/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.2. "The width and height of the returned image is scaled to n% of the
     * width and height of the extracted region. The aspect ratio of the
     * returned image is the same as that of the extracted region."
     *
     * @throws IOException
     */
    public void testSizeScaledToPercent() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/pct:50/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.2. "The width and height of the returned image are exactly w and h.
     * The aspect ratio of the returned image may be different than the
     * extracted region, resulting in a distorted image."
     *
     * @throws IOException
     */
    public void testAbsoluteWidthAndHeight() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/50,50/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.2. "The image content is scaled for the best fit such that the
     * resulting width and height are less than or equal to the requested
     * width and height. The exact scaling may be determined by the service
     * provider, based on characteristics including image quality and system
     * performance. The dimensions of the returned image content are
     * calculated to maintain the aspect ratio of the extracted region."
     *
     * @throws IOException
     */
    public void testSizeScaledToFitInside() throws IOException {
        // TODO: write this
    }

    /**
     * 4.2. "If the resulting height or width is zero, then the server should
     * return a 400 (bad request) status code."
     *
     * @throws IOException
     */
    public void testResultingWidthOrHeightIsZero() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/pct:0/15/color.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }

        client = getClientForUriPath("/" + IMAGE + "/full/0,0/15/color.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * 4.3. "The degrees of clockwise rotation from 0 up to 360."
     *
     * @throws IOException
     */
    public void testRotation() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/15/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.3. "The image should be mirrored and then rotated as above."
     *
     * @throws IOException
     */
    public void testMirroredRotation() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/!15/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     *
     * @throws IOException
     */
    public void testInvalidRotation() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/-15/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }

        client = getClientForUriPath("/" + IMAGE + "/full/full/385/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * 4.4. "The image is returned in full color."
     *
     * @throws IOException
     */
    public void testColorQuality() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/0/color.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.4. "The image is returned in grayscale, where each pixel is black,
     * white or any shade of gray in between."
     *
     * @throws IOException
     */
    public void testGrayQuality() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/0/gray.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.4. "The image returned is bitonal, where each pixel is either black or
     * white."
     *
     * @throws IOException
     */
    public void testBitonalQuality() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/0/bitonal.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.4. "The image is returned using the server’s default quality (e.g.
     * color, gray or bitonal) for the image."
     *
     * @throws IOException
     */
    public void testDefaultQuality() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 4.4. "A quality value that is unsupported should result in a 400 status
     * code."
     *
     * @throws IOException
     */
    public void testUnsupportedQuality() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/0/bogus.jpg");
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
    public void testFormats() throws Exception {
        testFormat(OutputFormat.JPG);
        testFormat(OutputFormat.TIF);
        testFormat(OutputFormat.PNG);
        testFormat(OutputFormat.GIF);
        testFormat(OutputFormat.JP2);
        testFormat(OutputFormat.PDF);
        testFormat(OutputFormat.WEBP);
    }

    private void testFormat(OutputFormat format) throws Exception {
        ClientResource client = getClientForUriPath("/" + IMAGE +
                "/full/full/0/default." + format.getExtension());

        // does the current processor support this output format?
        SourceFormat sourceFormat = SourceFormat.getSourceFormat(IMAGE);
        Processor processor = ProcessorFactory.getProcessor(sourceFormat);
        if (processor.getAvailableOutputFormats(sourceFormat).contains(format)) {
            client.get();
            assertEquals(Status.SUCCESS_OK, client.getStatus());
            assertEquals(format.getMediaType(),
                    client.getResponse().getHeaders().getFirst("Content-type").getValue());
        } else {
            try {
                client.get();
                fail("Expected exception");
            } catch (ResourceException e) {
                assertEquals(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE, client.getStatus());
            }
        }
    }

    /**
     * 4.5
     *
     * @throws IOException
     */
    public void testUnsupportedFormat() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/0/default.bogus");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    /**
     * 4.7. "When the client requests an image, the server may add a link
     * header to the response that indicates the canonical URI for that
     * request."
     *
     * @throws IOException
     */
    public void testCanonicalUriLinkHeader() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals("<" + getBaseUri() + "/" + IMAGE +
                        "/full/full/0/default.jpg>;rel=\"canonical\"",
                client.getResponse().getHeaders().getFirst("Link").getValue());
    }

    /**
     * 5. "The service must return this information about the image."
     *
     * @throws IOException
     */
    public void testInformationRequest() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    /**
     * 5. "The content-type of the response must be either “application/json”,
     * (regular JSON), or “application/ld+json” (JSON-LD)."
     *
     * @throws IOException
     */
    public void testInformationRequestContentType() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.get();
        assertEquals("application/json; charset=UTF-8",
                client.getResponse().getHeaders().getFirst("Content-type").getValue());
    }

    /**
     * 5. "If the client explicitly wants the JSON-LD content-type, then it
     * must specify this in an Accept header, otherwise the server must return
     * the regular JSON content-type."
     *
     * @throws IOException
     */
    public void testInformationRequestContentTypeJsonLd() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.accept(new MediaType("application/ld+json"));
        client.get();
        assertEquals("application/ld+json; charset=UTF-8",
                client.getResponse().getHeaders().getFirst("Content-type").getValue());

        client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.accept(new MediaType("application/json"));
        client.get();
        assertEquals("application/json; charset=UTF-8",
                client.getResponse().getHeaders().getFirst("Content-type").getValue());
    }

    /**
     * 5. "If the regular JSON content-type is returned, then it is
     * recommended that the server provide a link header to the context
     * document. The syntax for the link header is below, and further
     * described in section 6.8 of the JSON-LD specification. If the client
     * requests “application/ld+json”, the link header may still be included."
     *
     * @throws IOException
     */
    public void testInformationRequestLinkHeaderToContextDocument()
            throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.get();
        assertEquals("<http://iiif.io/api/image/2/context.json>; " +
                        "rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\"",
                client.getResponse().getHeaders().getFirst("Link").getValue());
    }

    /**
     * 5. "Servers should send the Access-Control-Allow-Origin header with the
     * value * in response to information requests."
     *
     * @throws IOException
     */
    public void testInformationRequestCorsHeader() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.get();
        assertEquals("*", client.getResponse().getAccessControlAllowOrigin());
    }

    /**
     * 5.1
     *
     * @throws IOException
     */
    public void testInformationRequestJson() throws IOException {
        // this will be tested in the processor tests
    }

    /**
     * 5.1. "If any of formats, qualities, or supports have no additional
     * values beyond those specified in the referenced compliance level, then
     * the property should be omitted from the response rather than being
     * present with an empty list."
     *
     * @throws IOException
     */
    public void testInformationRequestEmptyJsonProperties() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        assertFalse(client.get().getText().contains("null"));
    }

    /**
     * 6. "The Image Information document must ... include a compliance level
     * URI as the first entry in the profile property."
     *
     * @throws IOException
     */
    public void testComplianceLevel() throws IOException {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        String json = client.get().getText();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://iiif.io/api/image/2/level2.json",
                info.getProfile().get(0));
    }

    private ClientResource getClientForUriPath(String path) {
        Reference url = new Reference(getBaseUri() + path);
        ClientResource resource = new ClientResource(url);
        resource.setNext(client);
        return resource;
    }

    private String getBaseUri() {
        return "http://localhost:" + PORT +
                ImageServerApplication.BASE_IIIF_PATH;
    }

}
