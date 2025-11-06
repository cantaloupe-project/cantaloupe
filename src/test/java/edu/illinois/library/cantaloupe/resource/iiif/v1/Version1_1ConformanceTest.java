package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.controller.iiif.v1.IdentifierController;
import edu.illinois.library.cantaloupe.controller.iiif.v1.ImageController;
import edu.illinois.library.cantaloupe.controller.iiif.v1.InformationController;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>Functional test of conformance to the IIIF Image API 1.1 spec using MockMvc. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/1.1/#image-info-request">IIIF Image
 * API 1.1</a>
 */
@WebMvcTest({ImageController.class, InformationController.class, IdentifierController.class})
@Import({FormatRegistry.class, FormatRegistryAccessor.class, DelegateProxyService.class,
         InformationRequestHandlerFactory.class, ImageRequestHandlerFactory.class, StringUtils.class,
         SourceFactory.class})

public class Version1_1ConformanceTest {

    protected static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected Configuration configuration;

    @BeforeEach
    void setUp() throws Exception {
        // Set up configuration system properties
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Mock the default configuration similar to ResourceTest.setUp()
        when(configuration.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, true)).thenReturn(true);
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
     * 2.2. "It is recommended that if the image's base URI is dereferenced,
     * then the client should either redirect to the information request using
     * a 303 status code (see Section 6.1), or return the same result."
     */
    @Test
    void testBaseURIReturnsImageInfoViaHttp303() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}", IMAGE))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/iiif/1/" + IMAGE + "/info.json"));
    }

    /**
     * 3. "the identifier MUST be expressed as a string. All special characters
     * (e.g. ? or #) MUST be URI encoded to avoid unpredictable client
     * behaviors. The URL syntax relies upon slash (/) separators so any
     * slashes in the identifier MUST be URI encoded (aka. percent-encoded,
     * replace / with %2F )."
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
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/native.jpg", identifier))
                .andExpect(status().isOk());

        // information endpoint
        mockMvc.perform(get("/iiif/1/{identifier}/info.json", identifier))
                .andExpect(status().isOk());
    }

    /**
     * 4.1
     */
    @Test
    void testFullRegion() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/native.jpg", IMAGE))
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
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/20,20,100,100/full/0/color.jpg", IMAGE))
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
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/pct:20,20,50,50/full/0/color.jpg", IMAGE))
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
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/pct:20.2,20.6,50.2,50.6/full/0/color.jpg", IMAGE))
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
     * dimensions of the source image, then the service should return an image
     * cropped at the boundary of the source image."
     */
    @Test
    void testAbsolutePixelRegionLargerThanSource() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/0,0,99999,99999/full/0/color.jpg", IMAGE))
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
     * server MUST return a 400 (bad request) status code."
     */
    @Test
    void testZeroRegion() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/0,0,0,0/full/0/native.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.1. "If the region is entirely outside the bounds of the source image,
     * then the server MUST return a 400 (bad request) status code."
     */
    @Test
    void testXYRegionOutOfBounds() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/99999,99999,50,50/full/0/native.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.2
     */
    @Test
    void testFullSize() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
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
     * maintains the aspect ratio of the requested region."
     */
    @Test
    void testSizeScaledToFitWidth() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/full/50,/0/color.jpg", IMAGE))
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
     * maintains the aspect ratio of the requested region."
     */
    @Test
    void testSizeScaledToFitHeight() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/full/,50/0/color.jpg", IMAGE))
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
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/full/pct:50/0/color.jpg", IMAGE))
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
     * The aspect ratio of the returned image MAY be different than the
     * extracted region, resulting in a distorted image."
     */
    @Test
    void testAbsoluteWidthAndHeight() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/full/50,50/0/color.jpg", IMAGE))
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
     * resulting width and height are less than or equal to the requested width
     * and height. The exact scaling MAY be determined by the service provider,
     * based on characteristics including image quality and system performance.
     * The dimensions of the returned image content are calculated to maintain
     * the aspect ratio of the extracted region."
     */
    @Test
    void testSizeScaledToFitInside() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/full/!20,20/0/native.jpg", IMAGE))
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
     * 4.2. "If the resulting height or width is zero, then the server MUST
     * return a 400 (bad request) status code."
     */
    @Test
    void testResultingWidthOrHeightIsZero() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/pct:0/15/color.jpg", IMAGE))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/iiif/1/wide.jpg/full/3,0/15/color.jpg"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.2. "If the size parameter is syntactically invalid, the server should
     * return a 400 status code."
     */
    @Test
    void testInvalidSize() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/cats/0/native.jpg", IMAGE))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/iiif/1/{identifier}/full/cats,50/0/native.jpg", IMAGE))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/iiif/1/{identifier}/full/50,cats/0/native.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.3. "The rotation value represents the number of degrees of clockwise
     * rotation from the original, and may be any floating point number from 0
     * to 360. Initially most services will only support 0, 90, 180 or 270 as
     * valid values."
     */
    @Test
    void testRotation() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/15.5/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * IIIF Image API 1.1 doesn't say anything about a negative rotation
     * parameter, so we will check for an HTTP 400.
     */
    @Test
    void testNegativeRotation() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/-15/native.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * IIIF Image API 1.1 doesn't say anything about a >360-degree rotation
     * parameter, so we will check for an HTTP 400.
     */
    @Test
    void testGreaterThanFullRotation() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/4855/native.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.4. "The image is returned at an unspecified bit-depth."
     */
    @Test
    void testNativeQuality() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/native.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.4. "The image is returned in full color, typically using 24 bits per
     * pixel."
     */
    @Test
    void testColorQuality() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.4. "The image is returned in greyscale, where each pixel is black,
     * white or any degree of grey in between, typically using 8 bits per
     * pixel."
     */
    @Test
    void testGreyQuality() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/grey.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.4. "The image returned is bitonal, where each pixel is either black or
     * white, using 1 bit per pixel when the format permits."
     */
    @Test
    void testBitonalQuality() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/bitonal.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * The IIIF Image API 1.1 doesn't say anything about unsupported qualities,
     * so we will check for an HTTP 400.
     */
    @Test
    void testUnsupportedQuality() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/bogus.jpg", IMAGE))
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
    }

    private void testFormat(Format outputFormat) throws Exception {
        final Format sourceFormat = Format.inferFormat(IMAGE);
        try (Processor processor = new ProcessorFactory(configuration).newProcessor(sourceFormat)) {
            final Set<Format> outputFormats = processor.getAvailableOutputFormats();

            // If the processor supports this SOURCE format
            if (!outputFormats.isEmpty()) {
                // If the processor supports this OUTPUT format
                if (outputFormats.contains(outputFormat)) {
                    MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/native.{format}",
                                                          IMAGE, outputFormat.getPreferredExtension()))
                            .andExpect(status().isOk())
                            .andReturn();

                    assertEquals(outputFormat.getPreferredMediaType().toString(),
                            result.getResponse().getContentType());
                } else {
                    mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/native.{format}",
                                      IMAGE, outputFormat.getPreferredExtension()))
                            .andExpect(status().isUnsupportedMediaType());
                }
            } else {
                mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/native.{format}",
                                  IMAGE, outputFormat.getPreferredExtension()))
                        .andExpect(status().isNotImplemented());
            }
        }
    }

    /**
     * 4.5
     */
    @Test
    void testUnsupportedFormat() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/native.bogus", IMAGE))
                .andExpect(status().isUnsupportedMediaType());
    }

    /**
     * 4.5 "If the format is not specified in the URI, then the server SHOULD
     * use the HTTP Accept header to determine the client's preferences for the
     * format. The server may either do 200 (return the representation in the
     * response) or 30x (redirect to the correct URI with a format extension)
     * style content negotiation."
     */
    @Test
    void testFormatInAcceptHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/native", IMAGE)
                                          .header("Accept", "image/png"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("image/png", result.getResponse().getContentType());
    }

    /**
     * 4.5 "If neither [format in URL or in Accept header] are given, then the
     * server should use a default format of its own choosing."
     */
    @Test
    void testNoFormat() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/native", IMAGE)
                                          .header("Accept", "*/*"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("image/jpeg", result.getResponse().getContentType());
    }

    /**
     * 5. "The service MUST return technical information about the requested
     * image in the JSON format."
     */
    @Test
    void testInformationRequest() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 5. "The content-type of the response must be either "application/json",
     * (regular JSON), or "application/ld+json" (JSON-LD)."
     */
    @Test
    void testInformationRequestContentType() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/1/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertTrue("application/json;charset=utf-8".equalsIgnoreCase(
                contentType.replace(" ", "").toLowerCase()));
    }

    /**
     * 5.
     */
    @Test
    void testInformationRequestJSON() {
        // this will be tested in InformationFactoryTest
    }

    /**
     * 6.2 "Requests are limited to 1024 characters."
     */
    @Test
    void testURITooLong() throws Exception {
        // information endpoint
        String uriStr = "/iiif/1/" + IMAGE + "/info.json?bogus=";
        uriStr = org.apache.commons.lang3.StringUtils.rightPad(uriStr, 1025, "a");
        mockMvc.perform(get(uriStr))
                .andExpect(status().isRequestUriTooLong());

        // image endpoint
        uriStr = "/iiif/1/" + IMAGE + "/full/full/0/native.jpg?bogus=";
        uriStr = org.apache.commons.lang3.StringUtils.rightPad(uriStr, 1025, "a");
        mockMvc.perform(get(uriStr))
                .andExpect(status().isRequestUriTooLong());
    }

    /**
     * 8. "A service should specify on all responses the extent to which the
     * API is supported. This is done by including an HTTP Link header
     * (RFC5988) entry pointing to the description of the highest level of
     * conformance of which ALL of the requirements are met."
     */
    @Test
    void testComplianceLevelLinkHeaderInInformationResponse() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/info.json", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Link",
                    "<http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2>;rel=\"profile\";"));
    }

    /**
     * 8. "A service should specify on all responses the extent to which the
     * API is supported. This is done by including an HTTP Link header
     * (RFC5988) entry pointing to the description of the highest level of
     * conformance of which ALL of the requirements are met."
     */
    @Test
    void testComplianceLevelLinkHeaderInImageResponse() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/native.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Link",
                    "<http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2>;rel=\"profile\";"));
    }

}
