package edu.illinois.library.cantaloupe.resource.iiif.v3;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.controller.iiif.v3.IdentifierController;
import edu.illinois.library.cantaloupe.controller.iiif.v3.ImageController;
import edu.illinois.library.cantaloupe.controller.iiif.v3.InformationController;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.image.FormatRegistry;
import edu.illinois.library.cantaloupe.image.FormatRegistryAccessor;
import edu.illinois.library.cantaloupe.resource.ImageRequestHandlerFactory;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandlerFactory;
import edu.illinois.library.cantaloupe.source.SourceFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.StringUtils;

/**
 * <p>Functional test of conformance to the IIIF Image API 3.0 spec using MockMvc. Methods
 * are implemented in the order of the assertions in the spec document.</p>
 *
 * @see <a href="http://iiif.io/api/image/3.0/">IIIF Image API 3.0</a>
 */
@WebMvcTest({ImageController.class, InformationController.class, IdentifierController.class})
@Import({FormatRegistry.class, FormatRegistryAccessor.class, DelegateProxyService.class,
         InformationRequestHandlerFactory.class, ImageRequestHandlerFactory.class, StringUtils.class,
         SourceFactory.class})
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
public class Version3_0ConformanceTest {

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();

        // Set up configuration system properties
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Mock the default configuration similar to ResourceTest.setUp()
        when(configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)).thenReturn(true);
        when(configuration.getDouble(Key.MAX_SCALE, 0)).thenReturn(0.0);
        when(configuration.getBoolean(Key.ADMIN_ENABLED, false)).thenReturn(true);
        when(configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)).thenReturn(true);
        when(configuration.getString(Key.DELEGATE_SCRIPT_PATHNAME, "")).thenReturn(TestUtil.getFixture("delegates.rb").toString());
        when(configuration.getString(Key.PROCESSOR_SELECTION_STRATEGY, "")).thenReturn("ManualSelectionStrategy");
        when(configuration.getString("processor.ManualSelectionStrategy.jpg")).thenReturn("Java2dProcessor");
        when(configuration.getString("processor.ManualSelectionStrategy.pdf")).thenReturn("PdfBoxProcessor");
        when(configuration.getString(Key.PROCESSOR_FALLBACK, "")).thenReturn("Java2dProcessor");
        when(configuration.getInt(Key.PROCESSOR_JPG_QUALITY, 80)).thenReturn(80);
        when(configuration.getString(Key.SOURCE_STATIC)).thenReturn("FilesystemSource");
        when(configuration.getString(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY, "")).thenReturn("BasicLookupStrategy");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_PREFIX, "")).thenReturn(TestUtil.getFixturePath() + "/images/");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "")).thenReturn("");
        when(configuration.getString(Key.BASE_URI, "")).thenReturn("");
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("");
        when(configuration.getString(Key.DERIVATIVE_CACHE, "")).thenReturn("");
        when(configuration.getString(Key.SOURCE_CACHE, "")).thenReturn("");
    }

    /**
     * 2. "When the base URI is dereferenced, the interaction should result in
     * the Image Information document. It is recommended that the response be a
     * 303 status redirection to the image information document's URI."
     */
    @Test
    void testBaseURIReturnsImageInfoViaHttp303() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}", IMAGE))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/iiif/3/" + IMAGE + "/info.json"));
    }

    /**
     * 3. "All special characters (e.g. ? or #) must be URI encoded to avoid
     * unpredictable client behaviors. The URI syntax relies upon slash (/)
     * separators so any slashes in the identifier must be URI encoded (also
     * called "percent encoded")."
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
        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", identifier))
                .andExpect(status().isOk());

        // information endpoint
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andExpect(status().isOk());
    }

    /**
     * 4.1
     */
    @Test
    void testFullRegion() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", IMAGE))
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
    void testSquareRegion() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/square/max/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
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
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/20,20,40,35/max/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(40, image.getWidth());
            assertEquals(35, image.getHeight());
        }
    }

    /**
     * 4.1
     */
    @Test
    void testPercentageRegion() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/pct:20,20,50,50/max/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            // 50% of 64x56 = 32x28
            assertEquals(32, image.getWidth());
            assertEquals(28, image.getHeight());
        }
    }

    /**
     * 4.2: max
     */
    @Test
    void testMaxSize() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", IMAGE))
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
     * 4.2.1: max with upscaling disallowed (MaxScale)
     */
    @Test
    void testMaxSizeWithUpscaling() throws Exception {
        final int maxScale = 2;
        when(configuration.getDouble(Key.MAX_SCALE, 0.0)).thenReturn((double) maxScale);

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", IMAGE))
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
     * 4.2: ^max
     */
    @Test
    void testMaxSizeUpscaled() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 0.0)).thenReturn(999.0);

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/^max/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            // Should be upscaled
            assertTrue(image.getWidth() >= 64);
            assertTrue(image.getHeight() >= 56);
        }
    }

    /**
     * 4.2.2: w,
     */
    @Test
    void testSizeScaledToFitWidth() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/32,/0/default.jpg", IMAGE))
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
     * 4.2.2: ^w,
     */
    @Test
    void testSizeUpscaledToFitWidth() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 0.0)).thenReturn(999.0);

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/^100,/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(100, image.getWidth());
            assertEquals(88, image.getHeight()); // Maintains aspect ratio
        }
    }

    /**
     * 4.2.2: ^w, without server support
     *
     * Implementation note: {@link Key#MAX_SCALE} is what this implementation
     * reads as "does not support upscaling." A ceiling of 1.0 or lower means
     * no upscaling is on offer, so a caret size that needs upscaling gets 501;
     * a ceiling of 0 means no ceiling, and nothing is rejected here at all.
     * Above 1.0, upscaling is supported and advertised through the {@code
     * sizeUpscaling} feature of 5.7, and a caret size that merely overshoots
     * the ceiling is an ordinary client error, so it gets 400 instead; see
     * {@link #testUpscaledSizesOverCeilingWhenServerSupportsUpscaling()}.
     */
    @Test
    void testSizeUpscaledToFitWidthWithoutServerSupport() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(1.0);

        mockMvc.perform(get("/iiif/3/{identifier}/full/^100,/0/default.jpg", IMAGE))
                .andExpect(status().isNotImplemented());
    }

    /**
     * 4.2.3: ,h
     */
    @Test
    void testSizeScaledToFitHeight() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/,30/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(34, image.getWidth()); // Maintains aspect ratio
            assertEquals(30, image.getHeight());
        }
    }

    /**
     * 4.2.3: ^,h
     */
    @Test
    void testSizeUpscaledToFitHeight() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 0.0)).thenReturn(999.0);

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/^,100/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(114, image.getWidth()); // Maintains aspect ratio
            assertEquals(100, image.getHeight());
        }
    }

    /**
     * 4.2.3: ^,h without server support
     *
     * Implementation note: {@link Key#MAX_SCALE} is what this implementation
     * reads as "does not support upscaling." A ceiling of 1.0 or lower means
     * no upscaling is on offer, so a caret size that needs upscaling gets 501;
     * a ceiling of 0 means no ceiling, and nothing is rejected here at all.
     * Above 1.0, upscaling is supported and advertised through the {@code
     * sizeUpscaling} feature of 5.7, and a caret size that merely overshoots
     * the ceiling is an ordinary client error, so it gets 400 instead; see
     * {@link #testUpscaledSizesOverCeilingWhenServerSupportsUpscaling()}.
     */
    @Test
    void testSizeUpscaledToFitHeightWithoutServerSupport() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(1.0);

        mockMvc.perform(get("/iiif/3/{identifier}/full/^,80/0/default.jpg", IMAGE))
                .andExpect(status().isNotImplemented());
    }

    /**
     * 4.2.4: pct:n
     */
    @Test
    void testSizeScaledToPercent() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/pct:50/0/default.jpg", IMAGE))
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
     * 4.2.4: pct:n (upscaled)
     */
    @Test
    void testSizeUpscaledToPercent() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 0.0)).thenReturn(999.0);

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/^pct:110/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(70, image.getWidth());
            assertEquals(62, image.getHeight());
        }
    }

    /**
     * 4.2.4: pct:n (upscaled) without server support
     *
     * Implementation note: {@link Key#MAX_SCALE} is what this implementation
     * reads as "does not support upscaling." A ceiling of 1.0 or lower means
     * no upscaling is on offer, so a caret size that needs upscaling gets 501;
     * a ceiling of 0 means no ceiling, and nothing is rejected here at all.
     * Above 1.0, upscaling is supported and advertised through the {@code
     * sizeUpscaling} feature of 5.7, and a caret size that merely overshoots
     * the ceiling is an ordinary client error, so it gets 400 instead; see
     * {@link #testUpscaledSizesOverCeilingWhenServerSupportsUpscaling()}.
     */
    @Test
    void testSizeUpscaledToPercentWithoutServerSupport() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(1.0);

        mockMvc.perform(get("/iiif/3/{identifier}/full/^pct:150/0/default.jpg", IMAGE))
                .andExpect(status().isNotImplemented());
    }

    /**
     * 4.2.5: w,h
     */
    @Test
    void testSizeScaledToAbsoluteWidthAndHeight() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/30,20/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(30, image.getWidth());
            assertEquals(20, image.getHeight());
        }
    }

    /**
     * 4.2.5: ^w,h
     */
    @Test
    void testUpscaleToAbsoluteWidthAndHeight() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 0.0)).thenReturn(999.0);

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/^100,80/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(100, image.getWidth());
            assertEquals(80, image.getHeight());
        }
    }

    /**
     * 4.2.5: ^w,h without server support
     *
     * Implementation note: {@link Key#MAX_SCALE} is what this implementation
     * reads as "does not support upscaling." A ceiling of 1.0 or lower means
     * no upscaling is on offer, so a caret size that needs upscaling gets 501;
     * a ceiling of 0 means no ceiling, and nothing is rejected here at all.
     * Above 1.0, upscaling is supported and advertised through the {@code
     * sizeUpscaling} feature of 5.7, and a caret size that merely overshoots
     * the ceiling is an ordinary client error, so it gets 400 instead; see
     * {@link #testUpscaledSizesOverCeilingWhenServerSupportsUpscaling()}.
     */
    @Test
    void testUpscaleToAbsoluteWidthAndHeightWithoutServerSupport() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(1.0);

        mockMvc.perform(get("/iiif/3/{identifier}/full/^100,80/0/default.jpg", IMAGE))
                .andExpect(status().isNotImplemented());
    }

    /**
     * 4.2.6: !w,h
     */
    @Test
    void testSizeScaledToFitInside() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/!50,50/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            // Should fit inside 50x50 while maintaining aspect ratio
            assertTrue(image.getWidth() <= 50);
            assertTrue(image.getHeight() <= 50);
        }
    }

    /**
     * 4.2.6: ^!w,h
     */
    @Test
    void testSizeUpscaledToFitInside() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 0.0)).thenReturn(999.0);

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/^!150,150/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            // Should be upscaled to fit inside 150x150 while maintaining aspect ratio
            assertTrue(image.getWidth() <= 150);
            assertTrue(image.getHeight() <= 150);
            assertTrue(image.getWidth() > 64 || image.getHeight() > 56); // Should be upscaled
        }
    }

    /**
     * 4.2.6: ^!w,h without server support
     *
     * Implementation note: {@link Key#MAX_SCALE} is what this implementation
     * reads as "does not support upscaling." A ceiling of 1.0 or lower means
     * no upscaling is on offer, so a caret size that needs upscaling gets 501;
     * a ceiling of 0 means no ceiling, and nothing is rejected here at all.
     * Above 1.0, upscaling is supported and advertised through the {@code
     * sizeUpscaling} feature of 5.7, and a caret size that merely overshoots
     * the ceiling is an ordinary client error, so it gets 400 instead; see
     * {@link #testUpscaledSizesOverCeilingWhenServerSupportsUpscaling()}.
     */
    @Test
    void testSizeUpscaledToFitInsideWithoutServerSupport() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(1.0);

        mockMvc.perform(get("/iiif/3/{identifier}/full/^!150,150/0/default.jpg", IMAGE))
                .andExpect(status().isNotImplemented());
    }

    /**
     * 4.2.6: !w,h with a box larger than the source. !w,h carries "not larger
     * than the extracted region" in its own definition, so the box is clamped
     * to source size rather than rejected.
     */
    @Test
    void testSizeScaledToFitInsideWhenBoxExceedsSource() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/!300,300/0/default.jpg", IMAGE))
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
     * 4.2.6: !w,h The ceiling in the !w,h definition is "the extracted
     * region," not the full image, so a box larger than a requested region is
     * clamped to that region rather than to the full source. The source is
     * 64&times;56 and the region is its top-left quarter.
     */
    @Test
    void testSizeScaledToFitInsideWhenBoxExceedsRegion() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/0,0,32,28/!600,600/0/default.jpg", IMAGE))
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
     * 4.2.6: ^!w,h The caret form drops "the extracted region" from the
     * ceiling, so the same region-scoped request upscales to the box instead
     * of being clamped to the region. Aspect ratio is preserved, so a
     * 32&times;28 region fills 600&times;525 rather than 600&times;600.
     */
    @Test
    void testSizeUpscaledToFitInsideWhenBoxExceedsRegion() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(999.0);

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/0,0,32,28/^!600,600/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            assertEquals(600, image.getWidth());
            assertEquals(525, image.getHeight());
        }
    }

    /**
     * The other side of the 501 boundary drawn by the {@code
     * WithoutServerSupport} tests above. With {@link Key#MAX_SCALE} above 1.0
     * the server does support upscaling and says so via {@code sizeUpscaling}
     * in the information response, so a caret size that asks for more than the
     * ceiling allows is a client error rather than an unimplemented feature.
     * The source is 64&times;56 and every size here asks for 2&times;, which
     * overshoots a ceiling of 1.5.
     */
    @Test
    void testUpscaledSizesOverCeilingWhenServerSupportsUpscaling() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(1.5);

        for (String size : new String[] {
                "^128,", "^,112", "^pct:200", "^128,112", "^!128,112"}) {
            mockMvc.perform(get("/iiif/3/{identifier}/full/" + size + "/0/default.jpg", IMAGE))
                    .andExpect(status().isBadRequest());
        }
    }

    /**
     * A {@link Key#MAX_SCALE} below 1.0 can reject a caret size that would not
     * have upscaled anything. Nothing about upscaling is unimplemented in that
     * case, so it is a 400 rather than a 501. The source is 64&times;56 and
     * {@code ^pct:60} is a downscale, but it still overshoots a 0.5 ceiling.
     */
    @Test
    void testUpscaledSizeOverCeilingBelowFullScaleWithoutUpscaling() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(0.5);

        mockMvc.perform(get("/iiif/3/{identifier}/full/^pct:60/0/default.jpg", IMAGE))
                .andExpect(status().isBadRequest());
    }

    /**
     * 4.3.1: 0 degrees
     */
    @Test
    void testRotation0() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.3.2: 90 degrees
     */
    @Test
    void testRotation90() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/max/90/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            // 90 degree rotation should swap width and height
            assertEquals(56, image.getWidth());
            assertEquals(64, image.getHeight());
        }
    }

    /**
     * 4.3.3: 180 degrees
     */
    @Test
    void testRotation180() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/max/180/default.jpg", IMAGE))
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
     * 4.3.4: 270 degrees
     */
    @Test
    void testRotation270() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/full/max/270/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] imageBytes = result.getResponse().getContentAsByteArray();
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            // 270 degree rotation should swap width and height
            assertEquals(56, image.getWidth());
            assertEquals(64, image.getHeight());
        }
    }

    /**
     * 4.3.5: Mirroring (!n)
     */
    @Test
    void testMirroring() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/full/max/!0/default.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.4.1: default quality
     */
    @Test
    void testQualityDefault() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.4.2: color quality
     */
    @Test
    void testQualityColor() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.4.3: gray quality
     */
    @Test
    void testQualityGray() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/gray.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.4.4: bitonal quality
     */
    @Test
    void testQualityBitonal() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/bitonal.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    /**
     * 4.5.1: jpg format
     */
    @Test
    void testFormatJPG() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"));
    }

    /**
     * 4.5.2: png format
     */
    @Test
    void testFormatPNG() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.png", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    /**
     * 5.1: Information Request CORS header
     */
    @Test
    void testInformationRequestCORSHeader() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    /**
     * 5.2: Information Request JSON structure
     */
    @Test
    void testInformationRequestJSON() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/ld+json")))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Verify IIIF v3 structure
        assertEquals("http://iiif.io/api/image/3/context.json", json.get("@context").asText());
        assertEquals("ImageService3", json.get("type").asText());
        assertEquals("http://iiif.io/api/image", json.get("protocol").asText());
        assertEquals("level2", json.get("profile").asText());

        // Verify required properties exist
        assertNotNull(json.get("width"), "width should be present");
        assertNotNull(json.get("height"), "height should be present");
        assertNotNull(json.get("id"), "id should be present");
    }
}
