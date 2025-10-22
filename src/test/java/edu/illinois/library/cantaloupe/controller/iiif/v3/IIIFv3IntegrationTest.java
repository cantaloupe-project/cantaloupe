package edu.illinois.library.cantaloupe.controller.iiif.v3;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

/**
 * Integration test for IIIF v3 controllers.
 * Tests the complete IIIF v3 workflow and controller interactions.
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties",
    "logging.level.edu.illinois.library.cantaloupe=DEBUG"
})
class IIIFv3IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Configuration configuration;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Default: endpoint is enabled
        when(configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)).thenReturn(true);
    }

    @Test
    void testCompleteIIIFv3Workflow() throws Exception {
        String identifier = "integration-test-image";

        // 1. Test IIIF v3 landing page
        mockMvc.perform(get("/iiif/3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"));

        // 2. Test identifier redirect
        MvcResult redirectResult = mockMvc.perform(get("/iiif/3/{identifier}", identifier))
                .andExpect(status().isSeeOther())
                .andExpect(header().exists("Location"))
                .andReturn();

        String locationHeader = redirectResult.getResponse().getHeader("Location");
        assertTrue(locationHeader.contains("/iiif/3/" + identifier + "/info.json"));

        // 3. Test information endpoint
        MvcResult infoResult = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/ld+json"))
                .andReturn();

        String infoResponseBody = infoResult.getResponse().getContentAsString();
        JsonNode infoJson = objectMapper.readTree(infoResponseBody);

        // Verify IIIF v3 compliance
        assertEquals("http://iiif.io/api/image/3/context.json", infoJson.get("@context").asText());
        assertEquals("ImageService3", infoJson.get("type").asText());
        assertTrue(infoJson.get("id").asText().contains(identifier));

        // 4. Test image endpoint (placeholder)
        MvcResult imageResult = mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", identifier))
                .andExpect(status().isNotImplemented())
                .andExpect(content().contentType("application/json"))
                .andReturn();

        String imageResponseBody = imageResult.getResponse().getContentAsString();
        JsonNode imageJson = objectMapper.readTree(imageResponseBody);
        assertEquals(identifier, imageJson.get("identifier").asText());
    }

    @Test
    void testIIIFv3DiscoveryWorkflow() throws Exception {
        String identifier = "discovery-test";

        // Test the typical IIIF discovery workflow:
        // Client discovers service through info.json, then makes image requests

        // 1. Client requests info.json to discover capabilities
        MvcResult infoResult = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier)
                .accept("application/ld+json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/ld+json")))
                .andReturn();

        String infoResponse = infoResult.getResponse().getContentAsString();
        JsonNode info = objectMapper.readTree(infoResponse);

        // Verify service discovery information
        assertTrue(info.has("format"));
        assertTrue(info.has("quality"));
        assertTrue(info.has("width"));
        assertTrue(info.has("height"));
        assertTrue(info.has("profile"));

        // 2. Client uses discovered information to make image requests
        JsonNode formats = info.get("format");
        assertTrue(formats.isArray());
        String firstFormat = formats.get(0).asText();

        JsonNode qualities = info.get("quality");
        assertTrue(qualities.isArray());
        String firstQuality = qualities.get(0).asText();

        // Make image request using discovered capabilities
        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/{quality}.{format}",
                identifier, firstQuality, firstFormat))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.identifier", is(identifier)))
                .andExpect(jsonPath("$.quality", is(firstQuality)))
                .andExpect(jsonPath("$.format", is(firstFormat)));
    }

    @Test
    void testIIIFv3ContentNegotiation() throws Exception {
        String identifier = "content-negotiation-test";

        // Test JSON-LD preference (default)
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/ld+json")));

        // Test JSON preference
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier)
                .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")));

        // Test profile parameter in content type
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertTrue(contentType.contains("profile=\"http://iiif.io/api/image/3/context.json\""));
    }

    @Test
    void testIIIFv3CORSSupport() throws Exception {
        String identifier = "cors-test";

        // Test CORS headers on info.json
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier)
                .header("Origin", "https://viewer.example.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, OPTIONS"));

        // Test CORS headers on identifier redirect
        mockMvc.perform(get("/iiif/3/{identifier}", identifier)
                .header("Origin", "https://viewer.example.com"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));

        // Test CORS headers on image endpoint
        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", identifier)
                .header("Origin", "https://viewer.example.com"))
                .andExpect(status().isNotImplemented())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    @Test
    void testIIIFv3ErrorHandling() throws Exception {
        // Test endpoint disabled
        when(configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(get("/iiif/3/disabled-test/info.json"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("This endpoint is disabled")));

        mockMvc.perform(get("/iiif/3/disabled-test"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/iiif/3/disabled-test/full/max/0/default.jpg"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testIIIFv3SpecialIdentifiers() throws Exception {
        // Test various special identifier cases that IIIF implementations must handle

        // Identifier with spaces
        String spacedIdentifier = "my test image";
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", spacedIdentifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", containsString(spacedIdentifier)));

        // Identifier with slashes (encoded)
        String slashedIdentifier = "folder%2Fsubfolder%2Fimage";
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", slashedIdentifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", containsString(slashedIdentifier)));

        // Very long identifier
        String longIdentifier = "a".repeat(100);
        mockMvc.perform(get("/iiif/3/{identifier}/info.json", longIdentifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", containsString(longIdentifier)));
    }

    @Test
    void testIIIFv3URLConstruction() throws Exception {
        String identifier = "url-construction-test";

        // Test with custom host and port
        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier)
                .header("Host", "iiif.example.org:8080"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        String serviceId = json.get("id").asText();

        assertTrue(serviceId.contains("iiif.example.org:8080"));
        assertTrue(serviceId.contains("/iiif/3/" + identifier));

        // Test redirect URL construction
        MvcResult redirectResult = mockMvc.perform(get("/iiif/3/{identifier}", identifier)
                .header("Host", "iiif.example.org:8080"))
                .andExpect(status().isSeeOther())
                .andReturn();

        String location = redirectResult.getResponse().getHeader("Location");
        assertTrue(location.contains("iiif.example.org:8080"));
        assertTrue(location.endsWith("/info.json"));
    }

    @Test
    void testIIIFv3ParameterParsing() throws Exception {
        String identifier = "parameter-test";

        // Test complex IIIF parameters
        String[][] parameterTests = {
            // region, size, rotation, quality, format
            {"full", "max", "0", "default", "jpg"},
            {"square", "256,256", "90", "color", "png"},
            {"100,100,200,200", "!300,300", "180", "gray", "webp"},
            {"pct:25,25,50,50", "pct:150", "22.5", "bitonal", "tif"},
            {"0,0,1000,1000", "^max", "!90", "default", "pdf"}
        };

        for (String[] params : parameterTests) {
            MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                    identifier, params[0], params[1], params[2], params[3], params[4]))
                    .andExpect(status().isNotImplemented())
                    .andExpect(content().contentType("application/json"))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            JsonNode json = objectMapper.readTree(responseBody);

            assertEquals(identifier, json.get("identifier").asText());
            assertEquals(params[0], json.get("region").asText());
            assertEquals(params[1], json.get("size").asText());
            assertEquals(params[2], json.get("rotation").asText());
            assertEquals(params[3], json.get("quality").asText());
            assertEquals(params[4], json.get("format").asText());
        }
    }

    @Test
    void testIIIFv3ServiceCompliance() throws Exception {
        String identifier = "compliance-test";

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/info.json", identifier))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode info = objectMapper.readTree(responseBody);

        // Test all required IIIF v3 Image Information properties
        assertNotNull(info.get("@context"), "Missing required @context");
        assertNotNull(info.get("id"), "Missing required id");
        assertNotNull(info.get("type"), "Missing required type");
        assertNotNull(info.get("protocol"), "Missing required protocol");
        assertNotNull(info.get("profile"), "Missing required profile");
        assertNotNull(info.get("width"), "Missing required width");
        assertNotNull(info.get("height"), "Missing required height");

        // Test correct values
        assertEquals("http://iiif.io/api/image/3/context.json", info.get("@context").asText());
        assertEquals("ImageService3", info.get("type").asText());
        assertEquals("http://iiif.io/api/image", info.get("protocol").asText());

        // Test that arrays are properly formatted
        assertTrue(info.get("format").isArray(), "format should be an array");
        assertTrue(info.get("quality").isArray(), "quality should be an array");
        assertTrue(info.get("format").size() > 0, "format array should not be empty");
        assertTrue(info.get("quality").size() > 0, "quality array should not be empty");

        // Test that dimensions are positive integers
        assertTrue(info.get("width").asInt() > 0, "width should be positive");
        assertTrue(info.get("height").asInt() > 0, "height should be positive");
    }

    @Test
    void testIIIFv3OptionsRequests() throws Exception {
        String identifier = "options-test";

        // Test OPTIONS on all endpoints
        mockMvc.perform(options("/iiif/3/{identifier}/info.json", identifier))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));

        mockMvc.perform(options("/iiif/3/{identifier}", identifier))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));

        mockMvc.perform(options("/iiif/3/{identifier}/full/max/0/default.jpg", identifier))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }
}
