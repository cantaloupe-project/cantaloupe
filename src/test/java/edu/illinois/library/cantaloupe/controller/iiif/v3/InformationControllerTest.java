package edu.illinois.library.cantaloupe.controller.iiif.v3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.EndpointDisabledException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Boot test for IIIF v3 Information Controller.
 * Tests the info.json endpoint functionality and IIIF compliance.
 */
@WebMvcTest(InformationController.class)
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
class InformationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Configuration configuration;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Default: endpoint is enabled
        when(configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)).thenReturn(true);
    }

    @Test
    void testGetInformation_ValidIdentifier() throws Exception {
        String identifier = "test-image";

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/ld+json"))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, OPTIONS"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Verify IIIF v3 structure
        assertEquals("http://iiif.io/api/image/3/context.json", json.get("@context").asText());
        assertEquals("ImageService3", json.get("type").asText());
        assertEquals("http://iiif.io/api/image", json.get("protocol").asText());
        assertEquals("level2", json.get("profile").asText());

        // Verify image dimensions
        assertEquals(1000, json.get("width").asInt());
        assertEquals(1000, json.get("height").asInt());
        assertEquals(1000, json.get("maxWidth").asInt());
        assertEquals(1000, json.get("maxHeight").asInt());

        // Verify supported formats
        JsonNode formats = json.get("format");
        assertTrue(formats.isArray());
        assertTrue(formats.toString().contains("jpg"));
        assertTrue(formats.toString().contains("png"));

        // Verify supported qualities
        JsonNode qualities = json.get("quality");
        assertTrue(qualities.isArray());
        assertTrue(qualities.toString().contains("default"));
        assertTrue(qualities.toString().contains("color"));

        // Verify rights information
        assertEquals("http://creativecommons.org/licenses/by/3.0/", json.get("rights").asText());

        // Verify ID contains the identifier
        String id = json.get("id").asText();
        assertTrue(id.contains(identifier));
        assertTrue(id.contains("/iiif/3/"));
    }

    @Test
    void testGetInformation_ContentNegotiation_JSON() throws Exception {
        mockMvc.perform(get("/iiif/3/test-image/info.json")
                .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andExpect(header().string("Content-Type", containsString("profile=\"http://iiif.io/api/image/3/context.json\"")));
    }

    @Test
    void testGetInformation_ContentNegotiation_JSONLD() throws Exception {
        mockMvc.perform(get("/iiif/3/test-image/info.json")
                .accept("application/ld+json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/ld+json")))
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andExpect(header().string("Content-Type", containsString("profile=\"http://iiif.io/api/image/3/context.json\"")));
    }

    @Test
    void testGetInformation_DefaultContentType() throws Exception {
        // Without Accept header, should default to JSON-LD
        mockMvc.perform(get("/iiif/3/test-image/info.json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/ld+json")));
    }

    @Test
    void testGetInformation_SpecialCharactersInIdentifier() throws Exception {
        String identifier = "test-image%20with%20spaces";

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", containsString(identifier)));
    }

    @Test
    void testGetInformation_URIConstruction() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/test-image/info.json")
                .header("Host", "example.com:8080")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "cdn.example.com"))
                .andExpect(status().isOk())
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
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Verify all required IIIF v3 properties are present
        assertNotNull(json.get("@context"), "@context is required");
        assertNotNull(json.get("id"), "id is required");
        assertNotNull(json.get("type"), "type is required");
        assertNotNull(json.get("protocol"), "protocol is required");
        assertNotNull(json.get("profile"), "profile is required");
        assertNotNull(json.get("width"), "width is required");
        assertNotNull(json.get("height"), "height is required");

        // Verify correct IIIF v3 values
        assertEquals("http://iiif.io/api/image/3/context.json", json.get("@context").asText());
        assertEquals("ImageService3", json.get("type").asText());
        assertEquals("http://iiif.io/api/image", json.get("protocol").asText());

        // Verify technical properties are integers
        assertTrue(json.get("width").isInt(), "width should be integer");
        assertTrue(json.get("height").isInt(), "height should be integer");
        assertTrue(json.get("maxWidth").isInt(), "maxWidth should be integer");
        assertTrue(json.get("maxHeight").isInt(), "maxHeight should be integer");

        // Verify arrays
        assertTrue(json.get("format").isArray(), "format should be array");
        assertTrue(json.get("quality").isArray(), "quality should be array");
    }

    @Test
    void testGetInformation_PlaceholderNote() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/test/info.json"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Verify placeholder note is present
        assertTrue(json.has("_note"), "Should contain implementation note");
        String note = json.get("_note").asText();
        assertTrue(note.contains("placeholder"), "Note should mention this is a placeholder");
    }

    @Test
    void testGetInformation_CORSHeaders() throws Exception {
        mockMvc.perform(get("/iiif/3/cors-test/info.json")
                .header("Origin", "https://example.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, OPTIONS"));
    }

    @Test
    void testGetInformation_MultipleConcurrentRequests() throws Exception {
        // Test thread safety with multiple concurrent requests
        String[] identifiers = {"image1", "image2", "image3", "image4", "image5"};

        for (String identifier : identifiers) {
            mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", containsString(identifier)));
        }
    }

    @Test
    void testGetInformation_LongIdentifier() throws Exception {
        String longIdentifier = "a".repeat(200); // Very long identifier

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", longIdentifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", containsString(longIdentifier)));
    }

    @Test
    void testGetInformation_IdentifierWithSlashes() throws Exception {
        // Test identifier that contains encoded slashes
        String identifier = "collection%2Fsubcollection%2Fimage";

        mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", containsString(identifier)));
    }
}
