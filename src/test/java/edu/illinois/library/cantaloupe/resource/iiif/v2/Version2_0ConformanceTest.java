package edu.illinois.library.cantaloupe.resource.iiif.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.controller.iiif.v2.IdentifierController;
import edu.illinois.library.cantaloupe.controller.iiif.v2.ImageController;
import edu.illinois.library.cantaloupe.controller.iiif.v2.InformationController;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.FormatRegistry;
import edu.illinois.library.cantaloupe.image.FormatRegistryAccessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resource.ImageRequestHandlerFactory;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandlerFactory;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>Functional test of conformance to the IIIF Image API 2.0 spec using MockMvc. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/2.0/#image-information">IIIF Image
 * API 2.0</a>
 */
@WebMvcTest({ImageController.class, InformationController.class, IdentifierController.class})
@Import({FormatRegistry.class, FormatRegistryAccessor.class, DelegateProxyService.class,
         InformationRequestHandlerFactory.class, ImageRequestHandlerFactory.class, StringUtils.class,
         SourceFactory.class})
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
public class Version2_0ConformanceTest {

    protected static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected Configuration configuration;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();

        // Set up configuration system properties
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Mock the default configuration similar to ResourceTest.setUp()
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(true);
        when(configuration.getDouble(Key.MAX_SCALE, 0)).thenReturn(0.0);
        when(configuration.getBoolean(Key.ADMIN_ENABLED, false)).thenReturn(true);
        when(configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)).thenReturn(true);
        when(configuration.getString(Key.DELEGATE_SCRIPT_PATHNAME, "")).thenReturn(TestUtil.getFixture("delegates.rb").toString());
        when(configuration.getString(Key.PROCESSOR_SELECTION_STRATEGY, "")).thenReturn("ManualSelectionStrategy");
        when(configuration.getString("processor.ManualSelectionStrategy.jpg")).thenReturn("Java2dProcessor");
        when(configuration.getString("processor.ManualSelectionStrategy.pdf")).thenReturn("PdfBoxProcessor");
        when(configuration.getString(Key.PROCESSOR_FALLBACK, "")).thenReturn("Java2dProcessor");
        when(configuration.getString(Key.SOURCE_STATIC)).thenReturn("FilesystemSource");
        when(configuration.getString(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY, "")).thenReturn("BasicLookupStrategy");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_PREFIX, "")).thenReturn(TestUtil.getFixturePath() + "/images/");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "")).thenReturn("");
        when(configuration.getString(Key.BASE_URI, "")).thenReturn("");
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("");
        when(configuration.getString(Key.DERIVATIVE_CACHE, "")).thenReturn("");
        when(configuration.getString(Key.SOURCE_CACHE, "")).thenReturn("");
        when(configuration.getInt(Key.PROCESSOR_JPG_QUALITY, 80)).thenReturn(80);
        when(configuration.getString(Key.PROCESSOR_TIF_COMPRESSION, "LZW")).thenReturn("LZW");

    }

    /**
     * 2. "When the base URI is dereferenced, the interaction should result in
     * the Image Information document. It is recommended that the response be a
     * 303 status redirection to the Image Information document's URI."
     */
    @Test
    void testBaseURIReturnsImageInfoViaHttp303() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}", IMAGE))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/iiif/2/" + IMAGE + "/info.json"));
    }

    /**
     * 3. "All special characters (e.g. ? or #) [in an identifier] must be URI
     * encoded to avoid unpredictable client behaviors. The URI syntax relies
     * upon slash (/) separators so any slashes in the identifier must be URI
     * encoded (also called "percent encoded").
     */
    @Test
    void testIdentifierWithEncodedCharacters() throws Exception {
        // override the filesystem prefix to one folder level up so we can use
        // a slash in the identifier
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path path = Paths.get(cwd, "src", "test", "resources");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_PREFIX, "")).thenReturn(path + File.separator);

        final String identifier = "images%2F" + IMAGE;

        // image endpoint
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/default.jpg", identifier))
                .andExpect(status().isOk());

        // information endpoint
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", identifier))
                .andExpect(status().isOk());
    }

    /**
     * 4.1
     */
    @Test
    void testFullRegion() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(64, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

    /**
     * 4.1
     */
    @Test
    void testAbsolutePixelRegion() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/20,20,100,100/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
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
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/pct:20,20,50,50/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
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
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/pct:20.2,20.6,50.2,50.6/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(32, image.getWidth());
            assertEquals(28, image.getHeight());
        }
    }

    /**
     * 4.1. "If the request specifies a region which extends beyond the
     * dimensions reported in the Image Information document, then the service
     * should return an image cropped at the image's edge, rather than adding
     * empty space."
     */
    @Test
    void testAbsolutePixelRegionLargerThanSource() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/0,0,99999,99999/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(64, image.getWidth());
            assertEquals(56, image.getHeight());
        }
    }

    /**
     * 4.1. "If the requested region's height or width is zero ... then the
     * server should return a 400 status code."
     */
    @Test
    void testZeroRegion() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/0,0,0,0/full/0/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.1. "If the requested region ... is entirely outside the bounds of the
     * reported dimensions, then the server should return a 400 status code."
     */
    @Test
    void testXYRegionOutOfBounds() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/99999,99999,50,50/full/0/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * The IIIF API Validator wants the server to return 400 for a bogus
     * (junk characters) region.
     */
    @Test
    void testBogusRegion() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/ca%20ioU/full/0/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.2
     */
    @Test
    void testFullSize() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
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
    void testSizeScaledToFitWidth() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/full/50,/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
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
    void testSizeScaledToFitHeight() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/full/,50/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
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
    void testSizeScaledToPercent() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/full/pct:50/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
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
    void testAbsoluteWidthAndHeight() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/full/50,50/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
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
    void testSizeScaledToFitInside() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/full/!20,20/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(20, image.getWidth());
            assertEquals(18, image.getHeight());
        }
    }

    /**
     * 4.2. "If the resulting height or width is zero, then the server should
     * return a 400 (bad request) status code."
     */
    @Test
    void testResultingWidthOrHeightIsZero() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/pct:0/15/color.jpg", IMAGE))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/iiif/2/wide.jpg/full/3,0/15/color.jpg"))
                .andExpect(status().isBadRequest());
    }

    /**
     * IIIF Image API 2.0 doesn't say anything about an invalid size
     * parameter, so we will check for an HTTP 400.
     */
    @Test
    void testInvalidSize() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/cats/0/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/iiif/2/{identifier}/full/cats,50/0/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/iiif/2/{identifier}/full/50,cats/0/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/iiif/2/{identifier}/full/cats,/0/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/iiif/2/{identifier}/full/,cats/0/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/iiif/2/{identifier}/full/!cats,50/0/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.3. "The degrees of clockwise rotation from 0 up to 360."
     */
    @Test
    void testRotation() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/15.5/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.3. "The image should be mirrored and then rotated as above."
     */
    @Test
    void testMirroredRotation() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/!15/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     */
    @Test
    void testNegativeRotation() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/-15/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.3. "A rotation value that is out of range or unsupported should result
     * in a 400 status code."
     */
    @Test
    void testGreaterThanFullRotation() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/4855/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.4. "The image is returned in full color."
     */
    @Test
    void testColorQuality() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.4. "The image is returned in grayscale, where each pixel is black,
     * white or any shade of gray in between."
     */
    @Test
    void testGrayQuality() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/gray.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.4. "The image returned is bitonal, where each pixel is either black or
     * white."
     */
    @Test
    void testBitonalQuality() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/bitonal.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.4. "The image is returned using the server's default quality (e.g.
     * color, gray or bitonal) for the image."
     */
    @Test
    void testDefaultQuality() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/default.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.4. "A quality value that is unsupported should result in a 400 status
     * code."
     */
    @Test
    void testUnsupportedQuality() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/bogus.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.5
     */
    @Test
    void testFormats() throws Exception {
        testFormat(Format.get("jpg"));
        testFormat(Format.get("tif"));
        testFormat(Format.get("png"));
        testFormat(Format.get("gif"));
        testFormat(Format.get("jp2"));
        testFormat(Format.get("pdf"));
        testFormat(Format.get("webp"));
    }

    private void testFormat(Format outputFormat) throws Exception {
        final Format sourceFormat = Format.inferFormat(IMAGE);
        final Processor processor = new ProcessorFactory(configuration).newProcessor(sourceFormat);
        final Set<Format> outputFormats = processor.getAvailableOutputFormats();

        String url = "/iiif/2/" + IMAGE + "/full/full/0/default." + outputFormat.getPreferredExtension();

        // If the processor supports this SOURCE format
        if (!outputFormats.isEmpty()) {
            // If the processor supports this OUTPUT format
            if (outputFormats.contains(outputFormat)) {
                MvcResult result = mockMvc.perform(get(url))
                        .andExpect(status().isOk())
                        .andReturn();
                assertEquals(outputFormat.getPreferredMediaType().toString(),
                        result.getResponse().getContentType());
            } else {
                mockMvc.perform(get(url))
                        .andExpect(status().isUnsupportedMediaType());
            }
        } else {
            mockMvc.perform(get(url))
                    .andExpect(status().isNotImplemented());
        }
    }

    /**
     * 4.5
     */
    @Test
    void testUnsupportedFormat() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/default.bogus", IMAGE))
                .andExpect(status().isUnsupportedMediaType());
    }

    /**
     * 4.7. "When the client requests an image, the server may add a link
     * header to the response that indicates the canonical URI for that
     * request."
     */
    @Test
    void testCanonicalUriLinkHeader() throws Exception {
        final String path = "/iiif/2/" + IMAGE + "/pct:50,50,50,50/,50/0/default.jpg";
        final String expectedURI = "/iiif/2/" + IMAGE + "/32,28,32,28/57,/0/default.jpg";

        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(header().string("Link", "<http://localhost" + expectedURI + ">;rel=\"canonical\""));
    }

    /**
     * 5. "The service must return this information about the image."
     */
    @Test
    void testInformationRequest() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 5. "The content-type of the response must be either "application/json",
     * (regular JSON), or "application/ld+json" (JSON-LD)."
     */
    @Test
    void testInformationRequestContentType() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"));
    }

    /**
     * 5. "If the client explicitly wants the JSON-LD content-type, then it
     * must specify this in an Accept header, otherwise the server must return
     * the regular JSON content-type."
     */
    @Test
    void testInformationRequestContentTypeJSONLD() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE)
                .header("Accept", "application/ld+json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/ld+json;charset=UTF-8"));

        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE)
                .header("Accept", "application/json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"));
    }

    /**
     * 5. "Servers should send the Access-Control-Allow-Origin header with the
     * value * in response to information requests."
     */
    @Test
    void testInformationRequestCORSHeader() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    /**
     * 5.1
     */
    @Test
    void testInformationRequestJSON() {
        // this will be tested in InformationFactoryTest
    }

    /**
     * 5.1. "If any of formats, qualities, or supports have no additional
     * values beyond those specified in the referenced compliance level, then
     * the property should be omitted from the response rather than being
     * present with an empty list."
     */
    @Test
    void testInformationRequestEmptyJSONProperties() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertFalse(json.contains("null"));
    }

    /**
     * 6. "The Image Information document must ... include a compliance level
     * URI as the first entry in the profile property."
     */
    @Test
    void testComplianceLevel() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/2/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Information<?, ?> info = objectMapper.readValue(json, Information.class);
        List<?> profile = (List<?>) info.get("profile");
        assertEquals("http://iiif.io/api/image/2/level2.json", profile.get(0));
    }

}
