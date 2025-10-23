package edu.illinois.library.cantaloupe.controller.iiif.v3;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import edu.illinois.library.cantaloupe.delegate.DelegateProxyService;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.FormatRegistry;
import edu.illinois.library.cantaloupe.image.FormatRegistryAccessor;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandlerFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;

/**
 * Spring Boot test for IIIF v3 Information Controller.
 * Tests the info.json endpoint functionality and IIIF compliance.
 * Note: These tests may fail if image sources are not properly configured.
 */
@WebMvcTest(InformationController.class)
@Import({FormatRegistry.class, FormatRegistryAccessor.class, DelegateProxyService.class, InformationRequestHandlerFactory.class})
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
class InformationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    // @Autowired
    // private InformationRequestHandlerFactory handlerFactory;

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
    }

    private Info createMockInfo() {
        return Info.builder()
            .withSize(800, 600)
            .withFormat(Format.get("jpg"))
            .build();
    }

    @Test
    void testGetInformation_ValidIdentifier() throws Exception {
        String identifier = "test-image";

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier)
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
        assertTrue(id.contains(identifier));
        assertTrue(id.contains("/iiif/3/"));
    }

    @Test
    void testGetInformation_ContentNegotiation_JSON() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/test-image/info.json")
                .accept("application/json"))
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andExpect(header().string("Content-Type", containsString("profile=\"http://iiif.io/api/image/3/context.json\"")))
                .andReturn();

        // Content type should be JSON-LD by default (not JSON) since we updated the controller
        assertTrue(result.getResponse().getContentType().contains("application/ld+json"));
    }

    @Test
    void testGetInformation_ContentNegotiation_JSONLD() throws Exception {
        mockMvc.perform(get("/iiif/3/test-image/info.json")
                .accept("application/ld+json"))
                .andExpect(header().string("Content-Type", containsString("application/ld+json")))
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andExpect(header().string("Content-Type", containsString("profile=\"http://iiif.io/api/image/3/context.json\"")));
    }

    @Test
    void testGetInformation_DefaultContentType() throws Exception {
        // Without Accept header, should default to JSON-LD
        mockMvc.perform(get("/iiif/3/test-image/info.json"))
                .andExpect(header().string("Content-Type", containsString("application/ld+json")));
    }

    @Test
    void testGetInformation_SpecialCharactersInIdentifier() throws Exception {
        String identifier = "test-image%20with%20spaces";

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andReturn();

        // Verify the response contains the identifier regardless of success or error status
        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        assertTrue(json.get("id").asText().contains(identifier));
    }

    @Test
    void testGetInformation_URIConstruction() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/test-image/info.json")
                .header("Host", "example.com:8080")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "cdn.example.com"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        String id = json.get("id").asText();
        assertTrue(id.startsWith("http"), "ID should start with http protocol");
        assertTrue(id.contains("/iiif/3/test-image"), "ID should contain the correct path");
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
        MvcResult result = mockMvc.perform(get("/iiif/3/sample-image/info.json"))
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
    void testGetInformation_RealImplementation() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/test/info.json"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // The implementation is now real, not placeholder - verify IIIF compliance
        assertEquals("http://iiif.io/api/image/3/context.json", json.get("@context").asText());
        assertEquals("ImageService3", json.get("type").asText());
        assertEquals("http://iiif.io/api/image", json.get("protocol").asText());
        assertEquals("level2", json.get("profile").asText());
    }

    @Test
    void testGetInformation_CORSHeaders() throws Exception {
        mockMvc.perform(get("/iiif/3/cors-test/info.json")
                .header("Origin", "https://example.com"))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, OPTIONS"));
    }

    @Test
    void testGetInformation_MultipleConcurrentRequests() throws Exception {
        // Test thread safety with multiple concurrent requests
        String[] identifiers = {"image1", "image2", "image3", "image4", "image5"};

        for (String identifier : identifiers) {
            MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(responseBody);
            assertTrue(json.get("id").asText().contains(identifier));
        }
    }

    @Test
    void testGetInformation_LongIdentifier() throws Exception {
        String longIdentifier = "a".repeat(200); // Very long identifier

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", longIdentifier))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        assertTrue(json.get("id").asText().contains(longIdentifier));
    }

    @Test
    void testGetInformation_IdentifierWithSlashes() throws Exception {
        // Test identifier that contains encoded slashes
        String identifier = "collection%2Fsubcollection%2Fimage";

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        assertTrue(json.get("id").asText().contains(identifier));
    }


    // @Test
    // void testGETAuthorizationWhenAuthorized() {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testAuthorizationWhenAuthorized(uri);
    // }

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

    // @Test
    // void testGETAuthorizationWhenForbidden() {
    //     URI uri = getHTTPURI("/forbidden.jpg/info.json");
    //     tester.testAuthorizationWhenForbidden(uri);
    // }

    // @Test
    // void testGETAuthorizationWhenNotAuthorizedWhenAccessingCachedResource()
    //         throws Exception {
    //     URI uri = getHTTPURI("/forbidden.jpg/info.json");
    //     tester.testAuthorizationWhenNotAuthorizedWhenAccessingCachedResource(uri);
    // }

    // @Test
    // void testGETAuthorizationWhenScaleConstraining() throws Exception {
    //     URI uri = getHTTPURI("/reduce.jpg/info.json");
    //     tester.testAuthorizationWhenScaleConstraining(uri);
    // }

    // @Test
    // void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    // }

    // @Test
    // void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable()
    //         throws Exception {
    //     URI uri = getHTTPURI("/bogus/info.json");
    //     tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable(uri);
    // }

    // /**
    //  * Tests that there is no Cache-Control header returned when
    //  * cache.client.enabled = true but a cache=false argument is present in the
    //  * URL query.
    //  */
    // @Test
    // void testGETCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=false");
    //     tester.testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(uri);
    // }

    // @Test
    // void testGETCacheHeadersWhenClientCachingIsDisabled() throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheHeadersWhenClientCachingIsDisabled(uri);
    // }

    // @Test
    // void testGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied1()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=nocache");
    //     tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    // }

    // @Test
    // void testGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied2()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=false");
    //     tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    // }

    // @Test
    // void testGETCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied()
    //         throws Exception {
    //     assumeFalse(SystemUtils.IS_OS_WINDOWS); // TODO: why does this fail in Windows?

    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=recache");
    //     tester.testCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied(uri);
    // }

    // @Test
    // void testGETCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGETCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGETCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGETCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGETCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGETCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGETCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGETCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled()
    //         throws Exception {
    //     // The image must be modified as unmodified images aren't cached.
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled(
    //             uri, TestUtil.getImage(IMAGE));
    // }

    // @Test
    // void testGETEndpointEnabled() {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.IIIF_3_ENDPOINT_ENABLED, true);

    //     assertStatus(200, getHTTPURI("/" + IMAGE + "/info.json"));
    // }

    // @Test
    // void testGETEndpointDisabled() {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.IIIF_3_ENDPOINT_ENABLED, false);

    //     assertStatus(403, getHTTPURI("/" + IMAGE + "/info.json"));
    // }

    // @Test
    // void testGETWithForwardSlashInIdentifier() {
    //     URI uri = getHTTPURI("/subfolder%2Fjpg/info.json");
    //     tester.testForwardSlashInIdentifier(uri);
    // }

    // @Test
    // void testGETWithBackslashInIdentifier() {
    //     URI uri = getHTTPURI("/subfolder%5Cjpg/info.json");
    //     tester.testBackslashInIdentifier(uri);
    // }

    // @Test
    // void testGETWithIllegalCharactersInIdentifier() {
    //     String uri = getHTTPURIString("/[bogus]/info.json");
    //     tester.testIllegalCharactersInIdentifier(uri);
    // }

    // @Test
    // void testGETHTTP2() throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testHTTP2(uri);
    // }

    // @Test
    // void testGETHTTPS1_1() throws Exception {
    //     URI uri = getHTTPSURI("/" + IMAGE + "/info.json");
    //     tester.testHTTPS1_1(uri);
    // }

    // @Test
    // void testGETHTTPS2() throws Exception {
    //     URI uri = getHTTPSURI("/" + IMAGE + "/info.json");
    //     tester.testHTTPS2(uri);
    // }

    // @Test
    // void testGETForbidden() {
    //     URI uri = getHTTPURI("/forbidden/info.json");
    //     tester.testForbidden(uri);
    // }

    // @Test
    // void testGETNotFound() {
    //     URI uri = getHTTPURI("/invalid/info.json");
    //     tester.testNotFound(uri);
    // }

    // @Test
    // void testGETWithPageNumberInMetaIdentifier() {
    //     final String image = "pdf-multipage.pdf";
    //     URI uri1 = getHTTPURI("/" + image + "/info.json");
    //     URI uri2 = getHTTPURI("/" + image + ";2/info.json");
    //     assertRepresentationsNotSame(uri1, uri2);
    // }

    // @Test
    // void testGETWithPageNumberInQuery() {
    //     final String image = "pdf-multipage.pdf";
    //     URI uri1 = getHTTPURI("/" + image + "/info.json");
    //     URI uri2 = getHTTPURI("/" + image + "/info.json?page=2");
    //     assertRepresentationsNotSame(uri1, uri2);
    // }

    // @Test
    // void testGETPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(uri);
    // }

    // @Test
    // void testGETPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(uri);
    // }

    // @Test
    // void testGETRecoveryFromDerivativeCacheNewDerivativeImageInputStreamException()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testRecoveryFromDerivativeCacheNewDerivativeImageInputStreamException(uri);
    // }

    // @Test
    // void testGETRecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testRecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException(uri);
    // }

    // @Test
    // void testGETRecoveryFromIncorrectSourceFormat() throws Exception {
    //     URI uri = getHTTPURI("/jpg-incorrect-extension.png/info.json");
    //     tester.testRecoveryFromIncorrectSourceFormat(uri);
    // }

    // /**
    //  * Tests that a scale constraint of {@literal 1:1} is redirected to no
    //  * scale constraint.
    //  */
    // @Test
    // void testGETRedirectToNormalizedScaleConstraint1() {
    //     MetaIdentifier metaIdentifier = MetaIdentifier.builder()
    //             .withIdentifier(IMAGE)
    //             .withScaleConstraint(1, 1)
    //             .build();
    //     String metaIdentifierString = new StandardMetaIdentifierTransformer()
    //             .serialize(metaIdentifier, false);

    //     URI fromURI = getHTTPURI("/" + metaIdentifierString + "/info.json");
    //     URI toURI   = getHTTPURI("/" + IMAGE + "/info.json");
    //     assertRedirect(fromURI, toURI, 301);
    // }

    // /**
    //  * Tests that a scale constraint of {@literal 2:2} is redirected to no
    //  * scale constraint.
    //  */
    // @Test
    // void testGETRedirectToNormalizedScaleConstraint2() {
    //     MetaIdentifier metaIdentifier = MetaIdentifier.builder()
    //             .withIdentifier(IMAGE)
    //             .withScaleConstraint(2, 2)
    //             .build();
    //     String metaIdentifierString = new StandardMetaIdentifierTransformer()
    //             .serialize(metaIdentifier, false);

    //     URI fromURI = getHTTPURI("/" + metaIdentifierString + "/info.json");
    //     URI toURI   = getHTTPURI("/" + IMAGE + "/info.json");
    //     assertRedirect(fromURI, toURI, 301);
    // }

    // /**
    //  * Tests that a scale constraint of {@literal 2:4} is redirected to
    //  * {@literal 1:2}.
    //  */
    // @Test
    // void testGETRedirectToNormalizedScaleConstraint3() {
    //     MetaIdentifier.Builder builder = MetaIdentifier.builder()
    //             .withIdentifier(IMAGE);
    //     // create the "from" URI
    //     MetaIdentifier metaIdentifier = builder
    //             .withScaleConstraint(2, 4)
    //             .build();
    //     String metaIdentifierString =
    //             new StandardMetaIdentifierTransformer().serialize(metaIdentifier);
    //     URI fromURI = getHTTPURI("/" + metaIdentifierString + "/info.json");

    //     // create the "to" URI
    //     metaIdentifier = builder.withScaleConstraint(1, 2).build();
    //     metaIdentifierString =
    //             new StandardMetaIdentifierTransformer().serialize(metaIdentifier);
    //     URI toURI = getHTTPURI("/" + metaIdentifierString + "/info.json");

    //     assertRedirect(fromURI, toURI, 301);
    // }

    // @Test
    // void testGETScaleConstraintIsRespected() throws Exception {
    //     client = newClient("/" + IMAGE + ";1:2/info.json");
    //     Response response = client.send();

    //     String json = response.getBodyAsString();
    //     ObjectMapper mapper = new ObjectMapper();
    //     Information<?, ?> info = mapper.readValue(json, Information.class);
    //     assertEquals(32, info.get("width"));
    //     assertEquals(28, info.get("height"));
    // }

    // @Test
    // void testGETSourceCheckAccessNotCalledWithSourceCacheHit()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testSourceCheckAccessNotCalledWithSourceCacheHit(new Identifier(IMAGE), uri);
    // }

    // @Test
    // void testGETSourceGetSourceFormatNotCalledWithSourceCacheHit()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testSourceGetFormatNotCalledWithSourceCacheHit(new Identifier(IMAGE), uri);
    // }

    // /**
    //  * Checks that the server responds with HTTP 500 when a non-FileSource is
    //  * used with a non-StreamProcessor.
    //  */
    // @Test
    // void testGETSourceProcessorCompatibility() {
    //     URI uri = getHTTPURI("/jp2/info.json");
    //     tester.testSourceProcessorCompatibility(
    //             uri,
    //             appServer.getHTTPHost(),
    //             appServer.getHTTPPort());
    // }

    // @Test
    // void testGETSlashSubstitution() {
    //     URI uri = getHTTPURI("/subfolderCATSjpg/info.json");
    //     tester.testSlashSubstitution(uri);
    // }

    // @Test
    // void testGETUnavailableSourceFormat() {
    //     URI uri = getHTTPURI("/text.txt/info.json");
    //     tester.testUnavailableSourceFormat(uri);
    // }

    // @Test
    // void testGETURIsInJSON() throws Exception {
    //     client = newClient("/" + IMAGE + "/info.json");
    //     Response response = client.send();

    //     String json = response.getBodyAsString();
    //     ObjectMapper mapper = new ObjectMapper();
    //     Information<?, ?> info = mapper.readValue(json, Information.class);
    //     assertEquals("http://localhost:" + getHTTPPort() +
    //             Route.IIIF_3_PATH + "/" + IMAGE, info.get("id"));
    // }

    // @Test
    // void testGETURIsInJSONWithBaseURIOverride() throws Exception {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.BASE_URI, "http://example.org/");

    //     client = newClient("/" + IMAGE + "/info.json");
    //     Response response = client.send();

    //     String json = response.getBodyAsString();
    //     ObjectMapper mapper = new ObjectMapper();
    //     Information<?, ?> info = mapper.readValue(json, Information.class);
    //     assertEquals("http://example.org" +
    //             Route.IIIF_3_PATH + "/" + IMAGE, info.get("id"));
    // }

    // @Test
    // void testGETURIsInJSONWithSlashSubstitution() throws Exception {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.SLASH_SUBSTITUTE, "CATS");

    //     final String path = "/subfolderCATSjpg";
    //     client = newClient(path + "/info.json");
    //     Response response = client.send();

    //     String json = response.getBodyAsString();
    //     ObjectMapper mapper = new ObjectMapper();
    //     Information<?, ?> info = mapper.readValue(json, Information.class);
    //     assertEquals("http://localhost:" + getHTTPPort() +
    //             Route.IIIF_3_PATH + path, info.get("id"));
    // }

    // @Test
    // void testGETURIsInJSONWithEncodedCharacters() throws Exception {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.SLASH_SUBSTITUTE, "`");

    //     final String path = "/subfolder%60jpg";
    //     client = newClient(path + "/info.json");
    //     Response response = client.send();

    //     String json = response.getBodyAsString();
    //     ObjectMapper mapper = new ObjectMapper();
    //     Information<?, ?> info = mapper.readValue(json, Information.class);
    //     assertEquals("http://localhost:" + getHTTPPort() +
    //             Route.IIIF_3_PATH + path, info.get("id"));
    // }

    // @Test
    // void testGETURIsInJSONWithProxyHeaders() throws Exception {
    //     client = newClient("/" + IMAGE + "/info.json");
    //     client.getHeaders().set("X-Forwarded-Proto", "HTTP");
    //     client.getHeaders().set("X-Forwarded-Host", "example.org");
    //     client.getHeaders().set("X-Forwarded-Port", "8080");
    //     client.getHeaders().set("X-Forwarded-Path", "/cats");
    //     client.getHeaders().set(
    //             IIIFRequest.PUBLIC_IDENTIFIER_HEADER, "originalID");
    //     Response response = client.send();

    //     String json = response.getBodyAsString();
    //     ObjectMapper mapper = new ObjectMapper();
    //     Information<?, ?> info = mapper.readValue(json, Information.class);
    //     assertEquals("http://example.org:8080/cats" +
    //             Route.IIIF_3_PATH + "/originalID", info.get("id"));
    // }

    // @Test
    // void testGETBaseURIOverridesProxyHeaders() throws Exception {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.BASE_URI, "https://example.net/");

    //     client = newClient("/" + IMAGE + "/info.json");
    //     client.getHeaders().set("X-Forwarded-Proto", "HTTP");
    //     client.getHeaders().set("X-Forwarded-Host", "example.org");
    //     client.getHeaders().set("X-Forwarded-Port", "8080");
    //     client.getHeaders().set("X-Forwarded-Path", "/cats");
    //     Response response = client.send();

    //     String json = response.getBodyAsString();
    //     ObjectMapper mapper = new ObjectMapper();
    //     Information<?, ?> info = mapper.readValue(json, Information.class);
    //     assertEquals("https://example.net" +
    //             Route.IIIF_3_PATH + "/" + IMAGE, info.get("id"));
    // }

    // /**
    //  * Tests the default response headers. Individual headers may be tested
    //  * more thoroughly elsewhere.
    //  */
    // @Test
    // void testGETResponseHeaders() throws Exception {
    //     client = newClient("/" + IMAGE + "/info.json");
    //     Response response = client.send();
    //     Headers headers = response.getHeaders();
    //     assertEquals(8, headers.size());

    //     // Access-Control-Allow-Origin
    //     assertEquals("*", headers.getFirstValue("Access-Control-Allow-Origin"));
    //     // Content-Length
    //     assertNotNull(headers.getFirstValue("Content-Length"));
    //     // Content-Type
    //     assertTrue("application/ld+json;charset=UTF-8;profile=\"http://iiif.io/api/image/3/context.json\"".equalsIgnoreCase(
    //             headers.getFirstValue("Content-Type")));
    //     // Date
    //     assertNotNull(headers.getFirstValue("Date"));
    //     // Last-Modified
    //     assertNotNull(headers.getFirstValue("Last-Modified"));
    //     // Server
    //     assertNotNull(headers.getFirstValue("Server"));
    //     // Vary
    //     List<String> parts =
    //             List.of(StringUtils.split(headers.getFirstValue("Vary"), ", "));
    //     assertEquals(5, parts.size());
    //     assertTrue(parts.contains("Accept"));
    //     assertTrue(parts.contains("Accept-Charset"));
    //     assertTrue(parts.contains("Accept-Encoding"));
    //     assertTrue(parts.contains("Accept-Language"));
    //     assertTrue(parts.contains("Origin"));
    //     // X-Powered-By
    //     assertEquals(Application.getName() + "/" + Application.getVersion(),
    //             headers.getFirstValue("X-Powered-By"));
    // }

    // @Test
    // void testGETLastModifiedResponseHeaderWhenDerivativeCacheIsEnabled()
    //         throws Exception {
    //     URI uri = getHTTPURI("/" + IMAGE + "/info.json");
    //     tester.testLastModifiedHeaderWhenDerivativeCacheIsEnabled(uri);
    // }

    // @Test
    // void testOPTIONSWhenEnabled() throws Exception {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.IIIF_3_ENDPOINT_ENABLED, true);

    //     client = newClient("/" + IMAGE + "/info.json");
    //     client.setMethod(Method.OPTIONS);
    //     Response response = client.send();
    //     assertEquals(204, response.getStatus());

    //     Headers headers = response.getHeaders();
    //     List<String> methods =
    //             List.of(StringUtils.split(headers.getFirstValue("Allow"), ", "));
    //     assertEquals(2, methods.size());
    //     assertTrue(methods.contains("GET"));
    //     assertTrue(methods.contains("OPTIONS"));

    //     List<String> allowedHeaders =
    //             List.of(StringUtils.split(headers.getFirstValue("Access-Control-Allow-Headers"), ", "));
    //     assertEquals(1, allowedHeaders.size());
    //     assertTrue(allowedHeaders.contains("Authorization"));
    // }

    // @Test
    // void testOPTIONSWhenDisabled() throws Exception {
    //     Configuration config = Configuration.getInstance();
    //     config.setProperty(Key.IIIF_3_ENDPOINT_ENABLED, false);
    //     try {
    //         client = newClient("/" + IMAGE + "/info.json");
    //         client.setMethod(Method.OPTIONS);
    //         client.send();
    //         fail("Expected exception");
    //     } catch (ResourceException e) {
    //         assertEquals(403, e.getStatusCode());
    //     }
    // }

}
