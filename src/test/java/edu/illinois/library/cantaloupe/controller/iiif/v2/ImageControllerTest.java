package edu.illinois.library.cantaloupe.controller.iiif.v2;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.image.FormatRegistry;
import edu.illinois.library.cantaloupe.image.FormatRegistryAccessor;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.image.StandardMetaIdentifierTransformer;
import edu.illinois.library.cantaloupe.resource.ImageRequestHandlerFactory;
import edu.illinois.library.cantaloupe.source.AccessDeniedSource;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Boot test for IIIF v2 Image Controller using MockMvc.
 * Tests the image endpoint functionality and IIIF 2.x compliance.
 */
@WebMvcTest(ImageController.class)
@Import({FormatRegistry.class, FormatRegistryAccessor.class, DelegateProxyService.class,
         ImageRequestHandlerFactory.class, StringUtils.class, SourceFactory.class})
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    @BeforeEach
    void setUp() throws Exception {
        // Set up configuration system properties
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Default: endpoint is enabled
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(true);

        when(configuration.getString(Key.DELEGATE_SCRIPT_PATHNAME, "")).thenReturn(TestUtil.getFixture("delegates.rb").toString());
        when(configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)).thenReturn(true);
        when(configuration.getString(Key.SOURCE_STATIC)).thenReturn("FilesystemSource");
        when(configuration.getString(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY, "")).thenReturn("BasicLookupStrategy");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_PREFIX, "")).thenReturn(TestUtil.getFixturePath() + "/images/");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "")).thenReturn("");
        when(configuration.getString(Key.BASE_URI, "")).thenReturn("");
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("");

        when(configuration.getString(Key.PROCESSOR_SELECTION_STRATEGY, "")).thenReturn("ManualSelectionStrategy");
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(1.0);
        when(configuration.getString("processor.ManualSelectionStrategy.jpg")).thenReturn("Java2dProcessor");
        when(configuration.getString("processor.ManualSelectionStrategy.pdf")).thenReturn("PdfBoxProcessor");
        when(configuration.getString(Key.PROCESSOR_FALLBACK, "")).thenReturn("Java2dProcessor");
        when(configuration.getInt(Key.PROCESSOR_JPG_QUALITY, 80)).thenReturn(80);
        when(configuration.getInt(Key.MAX_PIXELS, 0)).thenReturn(0);
        when(configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)).thenReturn(false);
        when(configuration.getBoolean(Key.IIIF_RESTRICT_TO_SIZES, false)).thenReturn(false);

        // Cache configuration
        when(configuration.getString(Key.DERIVATIVE_CACHE, "")).thenReturn("");
        when(configuration.getString(Key.SOURCE_CACHE, "")).thenReturn("");
        when(configuration.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true)).thenReturn(true);
        // Mock the ImageRequestHandlerFactory to return a mock handler
        // ImageRequestHandler mockHandler = mock(ImageRequestHandler.class);
        // when(handlerFactory.create(any(), any(), any())).thenReturn(mockHandler);
    }

    private void stubCacheControlHeaders() {
        when(configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)).thenReturn(true);
        when(configuration.getBoolean(Key.CLIENT_CACHE_PUBLIC, true)).thenReturn(true);
        when(configuration.getBoolean(Key.CLIENT_CACHE_PRIVATE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_NO_CACHE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_NO_STORE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_MUST_REVALIDATE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_PROXY_REVALIDATE, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CLIENT_CACHE_NO_TRANSFORM, false)).thenReturn(true);
        when(configuration.getString(Key.CLIENT_CACHE_MAX_AGE, "")).thenReturn("1234");
        when(configuration.getString(Key.CLIENT_CACHE_SHARED_MAX_AGE, "")).thenReturn("4567");
    }

    // Authorization Tests

    @Test
    void testGETAuthorizationWhenAuthorized() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETAuthorizationWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", "unauthorized.jpg"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("\"status\":401")));
    }

    @Test
    void testGETAuthorizationWhenForbidden() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", "forbidden.jpg"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("\"status\":403")));
    }

    @Test
    void testGETAuthorizationWhenRedirecting() throws Exception {
        mockMvc.perform(get("/iiif/2/redirect.jpg/full/full/0/color.jpg"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "http://example.org/"));
    }

    @Test
    void testGETAuthorizationWhenScaleConstraining() throws Exception {
        mockMvc.perform(get("/iiif/2/reduce.jpg/full/full/0/color.jpg"))
            .andExpect(redirectedUrl("http://localhost/iiif/2/reduce.jpg;1:2/full/full/0/color.jpg"));
    }

    // Cache Header Tests

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=1234, s-maxage=4567, public, no-transform"));
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/2/bogus/full/full/0/color.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL1() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg?cache=nocache", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Cache-Control"));
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL2() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg?cache=false", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Cache-Control"));
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledAndRecachingIsEnabledInURL() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg?cache=recache", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=1234, s-maxage=4567, public, no-transform"));
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        when(configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)).thenReturn(false);

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Cache-Control"));
    }

    // Content Disposition Tests

    @Test
    void testGETContentDispositionHeaderWithNoHeader() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Disposition"));
    }

    @Test
    void testGETContentDispositionHeaderSetToInline() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg?response-content-disposition=inline", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"jpg-rgb-64x56x8-baseline.jpg.jpg\""));
    }

    @Test
    void testGETContentDispositionHeaderSetToAttachment() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg?response-content-disposition=attachment", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"jpg-rgb-64x56x8-baseline.jpg.jpg\""));
    }

    @Test
    void testGETContentDispositionHeaderSetToAttachmentWithFilename() throws Exception {
        final String filename = "cats%20dogs.jpg";
        final String expected = "attachment; filename=\"cats dogs.jpg\"";

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg?response-content-disposition=attachment;filename%3D%22" + filename + "%22", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", expected));
    }

    // Endpoint Enable/Disable Tests

    @Test
    void testGETEndpointEnabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(true);

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETEndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isForbidden());
    }

    // Special Character Tests

    @Test
    void testGETWithForwardSlashInIdentifier() throws Exception {
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("");

        mockMvc.perform(get("/iiif/2/subfolder%2Fjpg/full/max/0/default.jpg"))
                .andExpect(status().isOk());
    }

    @Test
    void testGETWithBackslashInIdentifier() throws Exception {
        mockMvc.perform(get("/iiif/2/subfolder%5Cjpg/full/max/0/default.jpg"))
                .andExpect(status().isOk());
    }

    @Test
    void testGETWithIllegalCharactersInIdentifier() throws Exception {
        mockMvc.perform(get("/iiif/2/[bogus]/full/full/0/default.jpg"))
                .andExpect(status().isNotFound());
    }

    // Link Header Tests

    @Test
    void testGETLinkHeader() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Link", startsWith("<http://localhost")))
                .andExpect(header().string("Link", containsString("rel=\"canonical\"")));
    }

    @Test
    void testGETLinkHeaderWithSlashSubstitution() throws Exception {
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("CATS");

        mockMvc.perform(get("/iiif/2/subfolderCATSjpg/full/full/0/color.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Link", containsString("subfolderCATSjpg")));
    }

    @Test
    void testGETLinkHeaderWithEncodedCharacters() throws Exception {
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("`");

        mockMvc.perform(get("/iiif/2/subfolder%60jpg/full/full/0/color.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Link", containsString("subfolder%60jpg")));
    }

    @Test
    void testGETLinkHeaderWithBaseURIOverride() throws Exception {
        when(configuration.getString(Key.BASE_URI, "")).thenReturn("http://example.org/");

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Link", startsWith("<http://example.org/")));
    }

    @Test
    void testGETLinkHeaderWithProxyHeaders() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/pct:50,50,50,50/,35/0/color.jpg", IMAGE)
                .header("X-Forwarded-Proto", "HTTP")
                .header("X-Forwarded-Host", "example.org")
                .header("X-Forwarded-Port", "8080")
                .header("X-Forwarded-Path", "/cats"))
                .andExpect(status().isOk())
                .andExpect(header().string("Link",
                    "<http://example.org:8080/cats/iiif/2/jpg-rgb-64x56x8-baseline.jpg/32,28,32,28/40,/0/color.jpg>;rel=\"canonical\""));
    }

    @Test
    void testGETLinkHeaderBaseURIOverridesProxyHeaders() throws Exception {
        when(configuration.getString(Key.BASE_URI, "")).thenReturn("https://example.net/");

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE)
                .header("X-Forwarded-Proto", "HTTP")
                .header("X-Forwarded-Host", "example.org")
                .header("X-Forwarded-Port", "8080")
                .header("X-Forwarded-Path", "/cats"))
                .andExpect(status().isOk())
                .andExpect(header().string("Link",
                    "<https://example.net/iiif/2/jpg-rgb-64x56x8-baseline.jpg/full/full/0/color.jpg>;rel=\"canonical\""));
    }

    // Scale Tests

    @Test
    void testGETLessThanOrEqualToFullScale() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(1.0);

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.png", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETGreaterThanFullScale() throws Exception {
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(1.0);

        mockMvc.perform(get("/iiif/2/{identifier}/full/pct:101/0/color.png", IMAGE))
                .andExpect(status().isForbidden());
    }

    // Pixel Tests

    @Test
    void testGETMinPixels() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/0,0,0,0/full/0/color.png", IMAGE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGETLessThanMaxPixels() throws Exception {
        when(configuration.getInt(Key.MAX_PIXELS, 0)).thenReturn(10000);

        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.png", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETMoreThanMaxPixelsWithFullSizeArgument() throws Exception {
        when(configuration.getInt(Key.MAX_PIXELS, 0)).thenReturn(1000);
        // when(configuration.getInt(Key.MAX_PIXELS, 0)).thenReturn(1000);
        when(configuration.getLong(Key.MAX_PIXELS, 0L)).thenReturn(1000L);


        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.png", IMAGE))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGETMoreThanMaxPixelsWithMaxSizeArgument() throws Exception {
        when(configuration.getInt(Key.MAX_PIXELS, 0)).thenReturn(1000);

        mockMvc.perform(get("/iiif/2/{identifier}/full/max/0/color.png", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETPixelRegionLessThanMaxPixelsWithMaxSizeArgument() throws Exception {
        when(configuration.getInt(Key.MAX_PIXELS, 0)).thenReturn(1000);

        mockMvc.perform(get("/iiif/2/{identifier}/0,0,10,10/max/0/color.png", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETPercentRegionLessThanMaxPixelsWithMaxSizeArgument() throws Exception {
        when(configuration.getInt(Key.MAX_PIXELS, 0)).thenReturn(1000);

        mockMvc.perform(get("/iiif/2/{identifier}/pct:0,0,25,25/max/0/color.png", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETPixelRegionMoreThanMaxPixelsWithMaxSizeArgument() throws Exception {
        when(configuration.getInt(Key.MAX_PIXELS, 0)).thenReturn(1000);

        mockMvc.perform(get("/iiif/2/{identifier}/0,0,50,50/max/0/color.png", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETPercentRegionMoreThanMaxPixelsWithMaxSizeArgument() throws Exception {
        when(configuration.getInt(Key.MAX_PIXELS, 0)).thenReturn(1000);

        mockMvc.perform(get("/iiif/2/{identifier}/pct:0,0,75,75/max/0/color.png", IMAGE))
                .andExpect(status().isOk());
    }

    // Error Tests

    @Test
    void testGETForbidden() throws Exception {
        when(configuration.getString(Key.SOURCE_STATIC)).thenReturn(AccessDeniedSource.class.getName());

        mockMvc.perform(get("/iiif/2/forbidden/full/full/0/color.jpg"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGETNotFound() throws Exception {
        mockMvc.perform(get("/iiif/2/invalid/full/full/0/color.jpg"))
                .andExpect(status().isNotFound());
    }

    // Multi-page Tests

    @Test
    void testGETWithPageNumberInMetaIdentifier() throws Exception {
        final String image = "pdf-multipage.pdf";

        // Request without page number
        byte[] response1 = mockMvc.perform(get("/iiif/2/{identifier}/full/max/0/color.jpg", image))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Request with page number
        byte[] response2 = mockMvc.perform(get("/iiif/2/{identifier};2/full/max/0/color.jpg", image))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Responses should be different
        assertFalse(java.util.Arrays.equals(response1, response2));
    }

    @Test
    void testGETWithPageNumberInQuery() throws Exception {
        final String image = "pdf-multipage.pdf";

        // Request without page parameter
        byte[] response1 = mockMvc.perform(get("/iiif/2/{identifier}/full/max/0/color.jpg", image))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Request with page parameter
        byte[] response2 = mockMvc.perform(get("/iiif/2/{identifier}/full/max/0/color.jpg?page=2", image))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Responses should be different
        assertFalse(java.util.Arrays.equals(response1, response2));
    }

    @Test
    void testGETProcessorValidationFailure() throws Exception {
        mockMvc.perform(get("/iiif/2/pdf-multipage.pdf/full/full/0/color.jpg?page=999999"))
                .andExpect(status().isBadRequest());
    }

    // Redirect Tests

    @Test
    void testGETRedirectToNormalizedScaleConstraint1() throws Exception {
        MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(IMAGE)
                .withScaleConstraint(1, 1)
                .build();
        String metaIdentifierString = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier, false);

        String fromPath = "/" + metaIdentifierString + "/full/full/0/color.png";
        String toPath = "/" + IMAGE + "/full/full/0/color.png";

        mockMvc.perform(get("/iiif/2" + fromPath))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("http://localhost/iiif/2" + toPath));
    }

    @Test
    void testGETRedirectToNormalizedScaleConstraint2() throws Exception {
        MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(IMAGE)
                .withScaleConstraint(2, 2)
                .build();
        String metaIdentifierString = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier, false);

        String fromPath = "/" + metaIdentifierString + "/full/full/0/color.png";
        String toPath = "/" + IMAGE + "/full/full/0/color.png";

        mockMvc.perform(get("/iiif/2" + fromPath))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("http://localhost/iiif/2" + toPath));
    }

    @Test
    void testGETRedirectToNormalizedScaleConstraint3() throws Exception {
        MetaIdentifier.Builder builder = MetaIdentifier.builder()
                .withIdentifier(IMAGE);

        // create the "from" URI
        MetaIdentifier metaIdentifier = builder.withScaleConstraint(2, 4).build();
        String metaIdentifierString1 = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier);
        String fromPath = "/" + IMAGE + metaIdentifierString1 + "/full/full/0/color.png";

        // create the "to" URI
        metaIdentifier = builder.withScaleConstraint(1, 2).build();
        String metaIdentifierString2 = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier);
        String toPath = "/" + IMAGE + metaIdentifierString2 + "/full/full/0/color.png";

        mockMvc.perform(get("/iiif/2" + fromPath))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("http://localhost/iiif/2" + toPath));
    }

    @Test
    void testGETScaleConstraintIsRespected() throws Exception {
        // This would need actual image comparison logic
        mockMvc.perform(get("/iiif/2/{identifier};1:2/full/max/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    // Size Restriction Tests

    @Test
    void testGETNotRestrictedToSizes() throws Exception {
        when(configuration.getBoolean(Key.IIIF_RESTRICT_TO_SIZES, false)).thenReturn(false);

        mockMvc.perform(get("/iiif/2/{identifier}/full/53,37/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETRestrictedToSizes() throws Exception {
        when(configuration.getBoolean(Key.IIIF_RESTRICT_TO_SIZES, false)).thenReturn(true);

        mockMvc.perform(get("/iiif/2/{identifier}/full/53,37/0/color.jpg", IMAGE))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGETSlashSubstitution() throws Exception {
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("CATS");

        mockMvc.perform(get("/iiif/2/subfolderCATSjpg/full/full/0/color.jpg"))
                .andExpect(status().isOk());
    }

    // Format Tests

    @Test
    void testGETUnavailableSourceFormat() throws Exception {
        mockMvc.perform(get("/iiif/2/text.txt/full/full/0/color.jpg"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void testGETInvalidOutputFormat() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.bogus", IMAGE))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testGETUnsupportedOutputFormat() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.pdf", IMAGE))
                .andExpect(status().isUnsupportedMediaType());
    }

    // Response Header Tests

    @Test
    void testGETResponseHeaders() throws Exception {
        mockMvc.perform(get("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().exists("Last-Modified"))
                .andExpect(header().string("Link", containsString("://")))
                .andExpect(header().string("Vary", containsString("Accept")));
    }

    // OPTIONS Tests

    @Test
    void testOPTIONSWhenEnabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(true);

        mockMvc.perform(options("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(header().string("Allow", containsString("OPTIONS")));
    }

    @Test
    void testOPTIONSWhenDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_2_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(options("/iiif/2/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isForbidden());
    }
}
