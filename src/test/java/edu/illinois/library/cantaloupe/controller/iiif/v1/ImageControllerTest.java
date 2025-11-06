package edu.illinois.library.cantaloupe.controller.iiif.v1;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.image.FormatRegistry;
import edu.illinois.library.cantaloupe.image.FormatRegistryAccessor;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Boot test for IIIF v1 Image Controller using MockMvc.
 * Tests the image endpoint functionality and IIIF 1.x compliance.
 */
@WebMvcTest(ImageController.class)
@Import({FormatRegistry.class, FormatRegistryAccessor.class, DelegateProxyService.class,
         ImageRequestHandlerFactory.class, StringUtils.class, SourceFactory.class})
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
        when(configuration.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, true)).thenReturn(true);

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
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETAuthorizationWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", "unauthorized.jpg"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("\"status\":401")));
    }

    @Test
    void testGETAuthorizationWhenForbidden() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", "forbidden.jpg"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("\"status\":403")));
    }

    @Test
    void testGETAuthorizationWhenNotAuthorizedWhenAccessingCachedResource() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", "forbidden.jpg"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("\"status\":403")));
    }

    @Test
    void testGETAuthorizationWhenRedirecting() throws Exception {
        mockMvc.perform(get("/iiif/1/redirect.jpg/full/full/0/color.jpg"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "http://example.org/"));
    }

    @Test
    void testGETAuthorizationWhenScaleConstraining() throws Exception {
        mockMvc.perform(get("/iiif/1/reduce.jpg/full/full/0/color.jpg"))
            .andExpect(redirectedUrl("http://localhost/iiif/1/reduce.jpg;1:2/full/full/0/color.jpg"));
    }

    // Cache Header Tests

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=1234, s-maxage=4567, public, no-transform"));
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/1/bogus/full/full/0/color.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL1() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg?cache=nocache", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Cache-Control"));
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL2() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg?cache=false", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Cache-Control"));
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledAndRecachingIsEnabledInURL() throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg?cache=recache", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=1234, s-maxage=4567, public, no-transform"));
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        when(configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)).thenReturn(false);

        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Cache-Control"));
    }

    @Test
    void testGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied1() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.png?cache=nocache", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied2() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.png?cache=false", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.png?cache=recache", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/10,10,50,40/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled() {
        // noop
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/10,10,50,40/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled() {
        // noop
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/10,10,50,40/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled() {
        // noop
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/10,10,50,40/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled() {
        // noop
    }

    // Content Disposition Tests

    @Test
    void testGETContentDispositionHeaderWithNoHeader() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Disposition"));
    }

    @Test
    void testGETContentDispositionHeaderSetToInline() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg?response-content-disposition=inline", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"jpg-rgb-64x56x8-baseline.jpg.jpg\""));
    }

    @Test
    void testGETContentDispositionHeaderSetToAttachment() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg?response-content-disposition=attachment", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"jpg-rgb-64x56x8-baseline.jpg.jpg\""));
    }

    @Test
    void testGETContentDispositionHeaderSetToAttachmentWithFilename() throws Exception {
        final String filename = "cats%20dogs.jpg";
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg?response-content-disposition=attachment;filename={filename}", IMAGE, filename))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"cats dogs.jpg\""));
    }

    // Endpoint Tests

    @Test
    void testGETWithEndpointEnabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, true)).thenReturn(true);

        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETWithEndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGETWithForwardSlashInIdentifier() throws Exception {
        mockMvc.perform(get("/iiif/1/subfolder%2Fjpg/full/full/0/color.jpg"))
                .andExpect(status().isOk());
    }

    @Test
    void testGETWithBackslashInIdentifier() throws Exception {
        mockMvc.perform(get("/iiif/1/subfolder%5Cjpg/full/full/0/color.jpg"))
                .andExpect(status().isOk());
    }

    @Test
    void testGETWithIllegalCharactersInIdentifier() throws Exception {
        mockMvc.perform(get("/iiif/1/[bogus]/full/full/0/color.jpg"))
                .andExpect(status().isNotFound());
    }

    // Scale Tests

    @Test
    void testGETLessThanOrEqualToFullScale() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.png", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETGreaterThanFullScale() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/pct:101/0/color.png", IMAGE))
                .andExpect(status().isForbidden());
    }

    // Pixel Tests

    @Test
    void testGETMinPixels() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/0,0,0,0/full/0/color.png", IMAGE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGETLessThanMaxPixels() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.png", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETMoreThanMaxPixels() throws Exception {
        when(configuration.getLong(Key.MAX_PIXELS, 0L)).thenReturn(1000L);
        when(configuration.getInt(Key.MAX_PIXELS, 0)).thenReturn(1000);

        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.png", IMAGE))
                .andExpect(status().isForbidden());
    }

    // Error Tests

    @Test
    void testGETForbidden() throws Exception {
        when(configuration.getString(Key.SOURCE_STATIC)).thenReturn(AccessDeniedSource.class.getName());

        mockMvc.perform(get("/iiif/1/forbidden/full/full/0/color.jpg"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGETNotFound() throws Exception {
        mockMvc.perform(get("/iiif/1/invalid/full/full/0/color.jpg"))
                .andExpect(status().isNotFound());
    }

    // Page Tests

    @Test
    void testGETWithPageNumberInMetaIdentifier() throws Exception {
        final String image = "pdf-multipage.pdf";
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", image))
                .andExpect(status().isOk());
        mockMvc.perform(get("/iiif/1/{identifier};2/full/full/0/color.jpg", image))
                .andExpect(status().isOk());
    }

    @Test
    void testGETWithPageNumberInQuery() throws Exception {
        final String image = "pdf-multipage.pdf";
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", image))
                .andExpect(status().isOk());
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg?page=2", image))
                .andExpect(status().isOk());
    }

    @Test
    void testGETProcessorValidationFailure() throws Exception {
        mockMvc.perform(get("/iiif/1/pdf-multipage.pdf/full/full/0/color.jpg?page=999999"))
                .andExpect(status().isBadRequest());
    }

    // Cache Purge Tests

    @Test
    void testGETPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse() throws Exception {
        mockMvc.perform(get("/iiif/1/missing/full/full/0/color.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGETPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue() throws Exception {
        mockMvc.perform(get("/iiif/1/missing/full/full/0/color.jpg"))
                .andExpect(status().isNotFound());
    }

    // Recovery Tests

    @Test
    void testGETRecoveryFromDerivativeCacheNewDerivativeImageInputStreamException() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETRecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.png", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETRecoveryFromIncorrectSourceFormat() throws Exception {
        mockMvc.perform(get("/iiif/1/jpg-incorrect-extension.png/full/full/0/color.jpg"))
                .andExpect(status().isOk());
    }

    // Scale Constraint Redirect Tests

    @Test
    void testGETRedirectToNormalizedScaleConstraint1() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier};1:2/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETRedirectToNormalizedScaleConstraint2() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", "jpg-rgb-64x56x8-baseline.jpg;2:2"))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("http://localhost/iiif/1/" + IMAGE + "/full/full/0/color.jpg"));
    }

    @Test
    void testGETRedirectToNormalizedScaleConstraint3() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier};2:4/full/full/0/color.jpg", IMAGE))
                .andExpect(redirectedUrl("http://localhost/iiif/1/" + IMAGE + ";1:2/full/full/0/color.jpg"));
    }

    // Source Access Tests

    @Test
    void testGETSourceCheckAccessNotCalledWithSourceCacheHit() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    @Test
    void testGETSourceGetSourceFormatNotCalledWithSourceCacheHit() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk());
    }

    // @Test
    // void testGETSourceProcessorCompatibility() throws Exception {
    // TODO: Unclear how to implement this HTTP_SOURCE without starting a server.
    //     when(configuration.getString(Key.SOURCE_STATIC)).thenReturn("HttpSource");
    //     when(configuration.getString(Key.HTTPSOURCE_LOOKUP_STRATEGY)).thenReturn("BasicLookupStrategy");
    //     when(configuration.getString(Key.HTTPSOURCE_URL_PREFIX)).thenReturn(appServerHost + ":" + appServerPort + "/");
    //     when(configuration.getString("processor.jp2")).thenReturn("OpenJpegProcessor");
    //     mockMvc.perform(get("/iiif/1/{identifier}/full/max/0/color.jpg", "jp2"))
    //             .andExpect(status().isInternalServerError());
    // }

    @Test
    void testGETSlashSubstitution() throws Exception {
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("CATS");

        mockMvc.perform(get("/iiif/1/subfolderCATSjpg/full/full/0/color.jpg"))
                .andExpect(status().isOk());
    }

    // Format Tests

    @Test
    void testGETUnavailableSourceFormat() throws Exception {
        mockMvc.perform(get("/iiif/1/text.txt/full/full/0/color.jpg"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void testGETInvalidOutputFormat() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.bogus", IMAGE))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testGETUnsupportedOutputFormat() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.pdf", IMAGE))
                .andExpect(status().isUnsupportedMediaType());
    }

    // Response Header Tests

    @Test
    void testGETResponseHeaders() throws Exception {
        mockMvc.perform(get("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Link", startsWith("<http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2>;rel=\"profile\"")));
    }

    // OPTIONS Tests

    @Test
    void testOPTIONSWhenEnabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, true)).thenReturn(true);

        mockMvc.perform(options("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,OPTIONS"));
    }

    @Test
    void testOPTIONSWhenDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_1_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(options("/iiif/1/{identifier}/full/full/0/color.jpg", IMAGE))
                .andExpect(status().isForbidden());
    }
}
