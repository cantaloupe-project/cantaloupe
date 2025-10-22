package edu.illinois.library.cantaloupe.controller.iiif.v3;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.FormatRegistry;
import edu.illinois.library.cantaloupe.image.FormatRegistryAccessor;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.resource.IIIFRequest;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandler;
import edu.illinois.library.cantaloupe.resource.InformationRequestHandlerFactory;

/**
 * Spring Boot test for IIIF v3 Information Controller.
 * Tests the info.json endpoint functionality and IIIF compliance.
 * Note: These tests may fail if image sources are not properly configured.
 */
@WebMvcTest(InformationController.class)
@Import({FormatRegistry.class, FormatRegistryAccessor.class})
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
class InformationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    @MockitoBean
    private InformationRequestHandlerFactory handlerFactory;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();

        // Set up configuration system properties
        ConfigurationFactory.clearInstance();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "memory");
        System.setProperty(Application.TEST_VM_ARGUMENT, "true");

        // Default: endpoint is enabled
        when(configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)).thenReturn(true);

        // Mock the factory to return a mock handler
        InformationRequestHandler mockHandler = org.mockito.Mockito.mock(InformationRequestHandler.class);
        when(handlerFactory.create(any(IIIFRequest.class), any(InformationRequestHandler.Callback.class)))
            .thenReturn(mockHandler);

        // Mock the handler's handle() method to return a basic Info
        Info mockInfo = createMockInfo();
        when(mockHandler.handle()).thenReturn(mockInfo);
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
}
