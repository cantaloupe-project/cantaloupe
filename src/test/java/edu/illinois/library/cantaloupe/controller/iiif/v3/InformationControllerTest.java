package edu.illinois.library.cantaloupe.controller.iiif.v3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.MockBrokenDerivativeInputStreamCache;
import edu.illinois.library.cantaloupe.cache.MockBrokenDerivativeOutputStreamCache;
import edu.illinois.library.cantaloupe.cache.SourceCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.image.StandardMetaIdentifierTransformer;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandlerFactory;
import edu.illinois.library.cantaloupe.resource.iiif.ImageAPIResourceTester.NotCheckingAccessSource;
import edu.illinois.library.cantaloupe.resource.iiif.ImageAPIResourceTester.NotReadingSourceFormatSource;
import edu.illinois.library.cantaloupe.source.AccessDeniedSource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.StringUtils;
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

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Boot test for IIIF v3 Information Controller.
 * Tests the info.json endpoint functionality and IIIF compliance.
 * Note: These tests may fail if image sources are not properly configured.
 */
@WebMvcTest(InformationController.class)
@Import({InformationRequestHandlerFactory.class,
         DelegateProxyService.class,
         StringUtils.class,
        //  FormatRegistry.class,
        //  FormatRegistryAccessor.class,
        })
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
class InformationControllerTest {
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

        when(configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)).thenReturn(true);
        when(configuration.getString(Key.DELEGATE_SCRIPT_PATHNAME, "")).thenReturn(TestUtil.getFixture("delegates.rb").toString());
        when(configuration.getBoolean(Key.DELEGATE_SCRIPT_ENABLED, false)).thenReturn(true);
        when(configuration.getString(Key.SOURCE_STATIC)).thenReturn("FilesystemSource");
        when(configuration.getString(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY, "")).thenReturn("BasicLookupStrategy");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_PREFIX, "")).thenReturn(TestUtil.getFixturePath() + "/images/");
        when(configuration.getString(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "")).thenReturn("");
        when(configuration.getString(Key.BASE_URI, "")).thenReturn("");
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("");
        when(configuration.getString(Key.SOURCE_CACHE, "")).thenReturn("");

        // Additional configuration needed for real ProcessorFactory
        when(configuration.getString(Key.PROCESSOR_SELECTION_STRATEGY)).thenReturn("ManualSelectionStrategy");
        when(configuration.getString(Key.PROCESSOR_SELECTION_STRATEGY, "")).thenReturn("ManualSelectionStrategy");
        when(configuration.getString("processor.ManualSelectionStrategy.jpg")).thenReturn("Java2dProcessor");
        when(configuration.getString("processor.ManualSelectionStrategy.pdf")).thenReturn("PdfBoxProcessor");
        when(configuration.getString(Key.PROCESSOR_FALLBACK, "")).thenReturn("Java2dProcessor");
        when(configuration.getDouble(Key.MAX_SCALE, 1.0)).thenReturn(1.0);
        when(configuration.getLong(Key.MAX_PIXELS, 0L)).thenReturn(0L);
        when(configuration.getInt(Key.IIIF_MIN_SIZE, 1)).thenReturn(1);
        when(configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)).thenReturn(false);
        when(configuration.getBoolean(Key.IIIF_RESTRICT_TO_SIZES, false)).thenReturn(false);

        // Cache configuration
        when(configuration.getString(Key.DERIVATIVE_CACHE, "")).thenReturn("");
        when(configuration.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true)).thenReturn(true);


    }

    private Info createMockInfo() {
        return Info.builder()
            .withSize(800, 600)
            .withFormat(Format.get("jpg"))
            .build();
    }

    @Test
    void testGetInformation_ValidIdentifier() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, OPTIONS"))
                .andReturn();

        // The response may be successful (200) with real image info or error (4xx/5xx) if no image source
        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Success case - verify IIIF v3 structure
        assertEquals("http://iiif.io/api/image/3/context.json", json.get("@context").asText());
        assertEquals("ImageService3", json.get("type").asText());
        assertEquals("http://iiif.io/api/image", json.get("protocol").asText());
        assertEquals("level2", json.get("profile").asText());

        // Verify required properties exist
        assertNotNull(json.get("width"), "width should be present");
        assertNotNull(json.get("height"), "height should be present");
        assertTrue(json.get("width").isInt(), "width should be integer");
        assertTrue(json.get("height").isInt(), "height should be integer");

        // Verify ID contains the identifier
        String id = json.get("id").asText();
        assertEquals("http://localhost/iiif/3/"+IMAGE, id);
    }

    @Test
    void testGetInformation_ContentNegotiation_JSON() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE)
                .accept("application/json"))
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andExpect(header().string("Content-Type", containsString("profile=\"http://iiif.io/api/image/3/context.json\"")))
                .andReturn();

        // Content type should be JSON-LD by default (not JSON) since we updated the controller
        assertTrue(result.getResponse().getContentType().contains("application/ld+json"));
    }

    @Test
    void testGetInformation_ContentNegotiation_JSONLD() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE)
                .accept("application/ld+json"))
                .andExpect(header().string("Content-Type", containsString("application/ld+json")))
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andExpect(header().string("Content-Type", containsString("profile=\"http://iiif.io/api/image/3/context.json\"")));
    }

    @Test
    void testGetInformation_DefaultContentType() throws Exception {
        // Without Accept header, should default to JSON-LD
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE))
                .andExpect(header().string("Content-Type", containsString("application/ld+json")));
    }

    @Test
    void testGetInformation_URIConstruction() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE)
                .header("Host", "example.com:8080")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "cdn.example.com"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        String id = json.get("id").asText();
        assertEquals("https://cdn.example.com/iiif/3/jpg-rgb-64x56x8-baseline.jpg", id);
    }

    @Test
    void testGetInformation_EndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(get("/iiif/3/test-image/info.json"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("This endpoint is disabled")));
    }

    @Test
    void testOptionsInformation() throws Exception {
        mockMvc.perform(options("/iiif/3/test-image/info.json"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, OPTIONS"));
    }

    @Test
    void testOptionsInformation_EndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(options("/iiif/3/test-image/info.json"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetInformation_IIIFCompliantResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Verify all required IIIF v3 properties are present
        assertNotNull(json.get("@context"), "@context is required");
        assertNotNull(json.get("id"), "id is required");
        assertNotNull(json.get("type"), "type is required");
        assertNotNull(json.get("protocol"), "protocol is required");
        assertNotNull(json.get("profile"), "profile is required");

        // Verify correct IIIF v3 values
        assertEquals("http://iiif.io/api/image/3/context.json", json.get("@context").asText());
        assertEquals("ImageService3", json.get("type").asText());
        assertEquals("http://iiif.io/api/image", json.get("protocol").asText());

        // verify dimension properties
        assertNotNull(json.get("width"), "width is required");
        assertNotNull(json.get("height"), "height is required");
        assertTrue(json.get("width").isInt(), "width should be integer");
        assertTrue(json.get("height").isInt(), "height should be integer");
    }

    @Test
    void testGetInformation_IdentifierWithSlashes() throws Exception {
        // Test identifier that contains encoded slashes
        String identifier = "subfolder%2Fjpg";

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        assertTrue(json.get("id").asText().contains(identifier));
    }

    @Test
    void testGetInformation_AuthorizationWhenUnauthorized() throws Exception {
        String identifier = "unauthorized.jpg";

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();

        assertEquals("{\"@context\":\"http://iiif.io/api/image/3/context.json\","+
                "\"id\":\"http://localhost/iiif/3/unauthorized.jpg\"," +
                "\"type\":\"ImageService3\"," +
                "\"protocol\":\"http://iiif.io/api/image\"," +
                "\"profile\":\"level2\"," +
                "\"status\":401," +
                "\"message\":\"Unauthorized\"," +
                "\"attribution\":\"Copyright My Great Organization. All rights reserved.\"," +
                "\"license\":\"http://example.org/license.html\"," +
                "\"service\":{" +
                    "\"@context\":\"http://iiif.io/api/annex/services/physdim/1/context.json\"," +
                    "\"profile\":\"http://iiif.io/api/annex/services/physdim\"," +
                    "\"physicalScale\":0.0025," +
                    "\"physicalUnits\":\"in\"}" +
                "}", responseBody);
    }


    @Test
    void testGetInformation_AuthorizationWhenForbidden() throws Exception {
        String identifier = "forbidden.jpg";

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("\"status\":403")));
    }

    @Test
    void testGetInformation_AuthorizationWhenScaleConstraining() throws Exception {
        String identifier = "reduce.jpg";

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andExpect(redirectedUrl("http://localhost/iiif/3/reduce.jpg;1:2/info.json"));
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

    @Test
    void testGetInformation_CacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
            throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE))
                .andExpect(header().string("Cache-Control", "max-age=1234, s-maxage=4567, public, no-transform"));
    }

    @Test
    void testGetInformation_CacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable()
            throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", "bogus"))
            .andExpect(status().isNotFound())
            .andExpect(header().string("Cache-Control", "no-cache, must-revalidate"));
    }

    /**
     * Tests that there is no Cache-Control header returned when
     * cache.client.enabled = true but a cache=false argument is present in the
     * URL query.
     */
    @Test
    void testGetInformation_CacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL()
            throws Exception {
        stubCacheControlHeaders();

        mockMvc.perform(get("/iiif/3/{identifier}/info.json?cache=false", IMAGE))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Cache-Control"));
    }

    @Test
    void testGetInformation_CacheHeadersWhenClientCachingIsDisabled() throws Exception {
        when(configuration.getBoolean(Key.CLIENT_CACHE_ENABLED, false)).thenReturn(false);

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Cache-Control"));
    }

    // @Test
    // void testGetInformation_CachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied1()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=nocache");
    //     tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    // }

    // @Test
    // void testGetInformation_CachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied2()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=false");
    //     tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    // }

    // @Test
    // void testGetInformation_CachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied()
    //         throws Exception {
    //     assumeFalse(SystemUtils.IS_OS_WINDOWS); // TODO: why does this fail in Windows?

    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=recache");
    //     tester.testCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied(uri);
    // }

    // @Test
    // void testGetInformation_CacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGetInformation_CacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGetInformation_CacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGetInformation_CacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGetInformation_CacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGetInformation_CacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGetInformation_CacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGetInformation_CacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }


    @Test
    void testGetInformation_WithForwardSlashInIdentifier() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", "subfolder%2Fjpg"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetInformation_WithBackslashInIdentifier() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", "subfolder%5Cjpg"))
            .andExpect(status().isOk());
    }

    @Test
    void testGetInformation_WithIllegalCharactersInIdentifier() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", "[bogus]"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetInformation_AccessDeniedSource() throws Exception {
        when(configuration.getString(Key.SOURCE_STATIC)).thenReturn(AccessDeniedSource.class.getName());

        mockMvc.perform(get("/iiif/3/forbidden/info.json"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Forbidden")));
    }

    @Test
    void testGetInformation_NotFound() throws Exception {
        mockMvc.perform(get("/iiif/3/invalid/info.json"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetInformation_WithPageNumberInMetaIdentifier() throws Exception {
        final String image = "pdf-multipage.pdf";
        MvcResult result1 = mockMvc.perform(get("/iiif/3/{identifier}/info.json", image))
                .andReturn();
        MvcResult result2 = mockMvc.perform(get("/iiif/3/{identifier}/info.json", image + ";2"))
                .andReturn();

        assertNotEquals(result1.getResponse().getContentAsString(),
                        result2.getResponse().getContentAsString());
    }

    @Test
    void testGetInformation_WithPageNumberInQuery() throws Exception {
        final String image = "pdf-multipage.pdf";
        MvcResult result1 = mockMvc.perform(get("/iiif/3/{identifier}/info.json", image))
                .andReturn();
        MvcResult result2 = mockMvc.perform(get("/iiif/3/{identifier}/info.json?page=2", image))
                .andReturn();

        assertNotEquals(result1.getResponse().getContentAsString(),
                        result2.getResponse().getContentAsString());
    }

    // @Test
    // void testGetInformation_PurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(uri);
    // }

    // @Test
    // void testGetInformation_PurgeFromCacheWhenSourceIsMissingAndOptionIsTrue()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(uri);
    // }

    @Test
    void testGetInformation_RecoveryFromDerivativeCacheNewDerivativeImageInputStreamException()
            throws Exception {
        when(configuration.getBoolean(Key.DERIVATIVE_CACHE_ENABLED, false)).thenReturn(true);
        when(configuration.getString(Key.DERIVATIVE_CACHE, "")).thenReturn(MockBrokenDerivativeInputStreamCache.class.getSimpleName());
        when(configuration.getBoolean(Key.INFO_CACHE_ENABLED, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true)).thenReturn(false);

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE))
            .andExpect(status().isOk());
    }

    @Test
    void testGetInformation_RecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException()
            throws Exception {
        when(configuration.getBoolean(Key.DERIVATIVE_CACHE_ENABLED, false)).thenReturn(true);
        when(configuration.getString(Key.DERIVATIVE_CACHE, "")).thenReturn(MockBrokenDerivativeOutputStreamCache.class.getSimpleName());
        when(configuration.getBoolean(Key.INFO_CACHE_ENABLED, false)).thenReturn(false);
        when(configuration.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true)).thenReturn(false);

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE))
            .andExpect(status().isOk());
    }

    @Test
    void testGetInformation_RecoveryFromIncorrectSourceFormat() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", "jpg-incorrect-extension.png"));
    }

    /**
     * Tests that a scale constraint of {@literal 1:1} is redirected to no
     * scale constraint.
     * @throws Exception
     */
    @Test
    void testGetInformation_RedirectToNormalizedScaleConstraint1() throws Exception {
        MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(IMAGE)
                .withScaleConstraint(1, 1)
                .build();
        String metaIdentifierString = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier, false);

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", metaIdentifierString))
                .andExpect(redirectedUrl("http://localhost/iiif/3/" + IMAGE + "/info.json"));
    }

    /**
     * Tests that a scale constraint of {@literal 2:2} is redirected to no
     * scale constraint.
     * @throws Exception
     */
    @Test
    void testGetInformation_RedirectToNormalizedScaleConstraint2() throws Exception {
        MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(IMAGE)
                .withScaleConstraint(2, 2)
                .build();
        String metaIdentifierString = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier, false);

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", metaIdentifierString))
                .andExpect(redirectedUrl("http://localhost/iiif/3/" + IMAGE + "/info.json"));
    }


    /**
     * Tests that a scale constraint of {@literal 2:4} is redirected to
     * {@literal 1:2}.
     * @throws Exception
     */
    @Test
    void testGetInformation_RedirectToNormalizedScaleConstraint3() throws Exception {
        MetaIdentifier.Builder builder = MetaIdentifier.builder()
                .withIdentifier(IMAGE);
        // create the "from" URI
        MetaIdentifier metaIdentifier = builder
                .withScaleConstraint(2, 4)
                .build();
        String metaIdentifierString =
                new StandardMetaIdentifierTransformer().serialize(metaIdentifier);

        // create the "to" URI
        metaIdentifier = builder.withScaleConstraint(1, 2).build();
        String expectedIdentifierString =
                new StandardMetaIdentifierTransformer().serialize(metaIdentifier);

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", metaIdentifierString))
            .andExpect(redirectedUrl("http://localhost/iiif/3/" + expectedIdentifierString + "/info.json"));
    }

    @Test
    void testGetInformation_ScaleConstraintIsRespected() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json",  IMAGE + ";1:2")).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        assertEquals(32, json.get("width").asInt());
        assertEquals(28, json.get("height").asInt());
    }

    @Test
    void testGetInformation_SourceCheckAccessNotCalledWithSourceCacheHit()
            throws Exception {
        when(configuration.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true)).thenReturn(false);
        when(configuration.getString(Key.SOURCE_STATIC)).thenReturn(NotCheckingAccessSource.class.getName());
        when(configuration.getString(Key.SOURCE_CACHE, "")).thenReturn("FilesystemCache");
        when(configuration.getLong(Key.SOURCE_CACHE_TTL, 0)).thenReturn((long) 10);
        when(configuration.getString(Key.FILESYSTEMCACHE_PATHNAME, "")).thenReturn(Files.createTempDirectory("test").toString());
        when(configuration.getString(Key.PROCESSOR_FALLBACK)).thenReturn("Java2dProcessor");

        // Put an image in the source cache.
        Path image = TestUtil.getImage("jpg");

        CacheFactory cacheFactory = new CacheFactory(configuration);
        SourceCache sourceCache = cacheFactory.getSourceCache().get();

        Identifier identifier = new Identifier(IMAGE);
        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(image, os);
        }
        mockMvc.perform(get("/iiif/3/{identifier}/info.json",  IMAGE))
            .andExpect(status().isOk());
    }

    @Test
    void testGetInformation_SourceGetSourceFormatNotCalledWithSourceCacheHit() throws Exception {
        when(configuration.getBoolean(Key.CACHE_SERVER_RESOLVE_FIRST, true)).thenReturn(false);
        when(configuration.getString(Key.SOURCE_STATIC)).thenReturn(NotReadingSourceFormatSource.class.getName());
        when(configuration.getString(Key.SOURCE_CACHE, "")).thenReturn("FilesystemCache");
        when(configuration.getLong(Key.SOURCE_CACHE_TTL, 0)).thenReturn((long) 10);
        when(configuration.getString(Key.FILESYSTEMCACHE_PATHNAME, "")).thenReturn(Files.createTempDirectory("test").toString());
        when(configuration.getString(Key.PROCESSOR_FALLBACK)).thenReturn("Java2dProcessor");

        // Put an image in the source cache.
        Path image = TestUtil.getImage("jpg");

        CacheFactory cacheFactory = new CacheFactory(configuration);
        SourceCache sourceCache = cacheFactory.getSourceCache().get();

        Identifier identifier = new Identifier(IMAGE);
        try (OutputStream os = sourceCache.newSourceImageOutputStream(identifier)) {
            Files.copy(image, os);
        }
        mockMvc.perform(get("/iiif/3/{identifier}/info.json",  IMAGE))
            .andExpect(status().isOk());
    }

    @Test
    void testGetInformation_UnavailableSourceFormat() throws Exception {
        mockMvc.perform(get("/iiif/3/{identifier}/info.json",  "text.txt"))
            .andExpect(status().is(501));
    }

    @Test
    void testGetInformation_SlashSubstitution() throws Exception {
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("CATS");

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json",  "subfolderCATSjpg"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Verify ID contains the encoded identifier
        String id = json.get("id").asText();
        assertEquals("http://localhost/iiif/3/subfolderCATSjpg", id);
    }

    @Test
    void testGetInformation_URIsInJSONWithEncodedCharacters() throws Exception {
        when(configuration.getString(Key.SLASH_SUBSTITUTE, "")).thenReturn("`");

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json",  "subfolder%60jpg"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Verify ID contains the encoded identifier
        String id = json.get("id").asText();
        assertEquals("http://localhost/iiif/3/subfolder%60jpg", id);
    }

    @Test
    void testGetInformation_URIsInJSONWithBaseURIOverride() throws Exception {
        when(configuration.getString(Key.BASE_URI, "")).thenReturn("http://example.org/");

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", IMAGE))
            .andReturn();

        // The response may be successful (200) with real image info or error (4xx/5xx) if no image source
        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Verify ID contains the identifier
        String id = json.get("id").asText();
        assertEquals("http://example.org/iiif/3/" + IMAGE, id);
    }

    @Test
    void testGetInformation_BaseURIOverridesProxyHeaders() throws Exception {
        when(configuration.getString(Key.BASE_URI, "")).thenReturn("http://example.net/");

        MvcResult result = mockMvc.perform(
            get("/iiif/3/{identifier}/info.json", IMAGE)
                .header("X-Forwarded-Proto", "HTTP")
                .header("X-Forwarded-Host", "example.org")
                .header("X-Forwarded-Port", "8080")
                .header("X-Forwarded-Path", "/cats"))
            .andReturn();

        // The response may be successful (200) with real image info or error (4xx/5xx) if no image source
        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Verify ID contains the identifier
        String id = json.get("id").asText();
        assertEquals("http://example.net/iiif/3/" + IMAGE, id);
    }

    // @Test
    // void testGetInformation_LastModifiedResponseHeaderWhenDerivativeCacheIsEnabled()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testLastModifiedHeaderWhenDerivativeCacheIsEnabled(uri);
    // }
}
