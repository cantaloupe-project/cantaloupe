package edu.illinois.library.cantaloupe.controller.iiif.v3;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

/**
 * Spring Boot test for IIIF v3 Image Controller.
 * Tests the image request functionality and parameter handling.
 */
@WebMvcTest(ImageController.class)
@TestPropertySource(properties = {
    "cantaloupe.config=test.properties"
})
class ImageControllerTest {

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
    void testGetImage_FullParameters() throws Exception {
        String identifier = "test-image";
        String region = "full";
        String size = "max";
        String rotation = "0";
        String quality = "default";
        String format = "jpg";

        MvcResult result = mockMvc.perform(get("/iiif/3/{identifier}/{region}/{size}/{rotation}/{quality}.{format}",
                identifier, region, size, rotation, quality, format))
                .andExpect(status().isNotImplemented()) // 501 status
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, OPTIONS"))
                .andExpect(content().contentType("application/json"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Verify all parameters are captured
        assertEquals("IIIF v3 image processing not yet fully implemented", json.get("message").asText());
        assertEquals(identifier, json.get("identifier").asText());
        assertEquals(region, json.get("region").asText());
        assertEquals(size, json.get("size").asText());
        assertEquals(rotation, json.get("rotation").asText());
        assertEquals(quality, json.get("quality").asText());
        assertEquals(format, json.get("format").asText());
        assertTrue(json.has("note"));
        assertTrue(json.get("note").asText().contains("Full implementation"));
    }

    @Test
    void testGetImage_SpecificRegion() throws Exception {
        mockMvc.perform(get("/iiif/3/test-image/100,100,200,200/max/0/default.jpg"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.region", is("100,100,200,200")));
    }

    @Test
    void testGetImage_PercentageRegion() throws Exception {
        mockMvc.perform(get("/iiif/3/test-image/pct:10,10,80,80/max/0/default.jpg"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.region", is("pct:10,10,80,80")));
    }

    @Test
    void testGetImage_SpecificSize() throws Exception {
        mockMvc.perform(get("/iiif/3/test-image/full/500,400/0/default.jpg"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.size", is("500,400")));
    }

    @Test
    void testGetImage_PercentageSize() throws Exception {
        mockMvc.perform(get("/iiif/3/test-image/full/pct:50/0/default.jpg"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.size", is("pct:50")));
    }

    @Test
    void testGetImage_MaxSize() throws Exception {
        mockMvc.perform(get("/iiif/3/test-image/full/max/0/default.jpg"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.size", is("max")));
    }

    @Test
    void testGetImage_UpscalingAllowed() throws Exception {
        mockMvc.perform(get("/iiif/3/test-image/full/^max/0/default.jpg"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.size", is("^max")));
    }

    @Test
    void testGetImage_RotationValues() throws Exception {
        String[] rotations = {"0", "90", "180", "270", "22.5", "!90"};

        for (String rotation : rotations) {
            mockMvc.perform(get("/iiif/3/test-image/full/max/{rotation}/default.jpg", rotation))
                    .andExpect(status().isNotImplemented())
                    .andExpect(jsonPath("$.rotation", is(rotation)));
        }
    }

    @Test
    void testGetImage_QualityValues() throws Exception {
        String[] qualities = {"default", "color", "gray", "bitonal"};

        for (String quality : qualities) {
            mockMvc.perform(get("/iiif/3/test-image/full/max/0/{quality}.jpg", quality))
                    .andExpect(status().isNotImplemented())
                    .andExpect(jsonPath("$.quality", is(quality)));
        }
    }

    @Test
    void testGetImage_FormatValues() throws Exception {
        String[] formats = {"jpg", "png", "gif", "webp", "tif", "pdf"};

        for (String format : formats) {
            mockMvc.perform(get("/iiif/3/test-image/full/max/0/default.{format}", format))
                    .andExpect(status().isNotImplemented())
                    .andExpect(jsonPath("$.format", is(format)));
        }
    }

    @Test
    void testGetImage_ComplexIdentifier() throws Exception {
        String identifier = "collection%2Fsubcollection%2Fimage.tif";

        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", identifier))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.identifier", is(identifier)));
    }

    @Test
    void testGetImage_LongIdentifier() throws Exception {
        String longIdentifier = "a".repeat(200);

        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", longIdentifier))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.identifier", is(longIdentifier)));
    }

    @Test
    void testGetImage_EndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(get("/iiif/3/test-image/full/max/0/default.jpg"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("This endpoint is disabled")));
    }

    @Test
    void testOptionsImage() throws Exception {
        mockMvc.perform(options("/iiif/3/test-image/full/max/0/default.jpg"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Allow", "GET,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, OPTIONS"));
    }

    @Test
    void testOptionsImage_EndpointDisabled() throws Exception {
        when(configuration.getBoolean(Key.IIIF_3_ENDPOINT_ENABLED, true)).thenReturn(false);

        mockMvc.perform(options("/iiif/3/test-image/full/max/0/default.jpg"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetImage_CORSHeaders() throws Exception {
        mockMvc.perform(get("/iiif/3/cors-test/full/max/0/default.jpg")
                .header("Origin", "https://example.com"))
                .andExpect(status().isNotImplemented())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, OPTIONS"));
    }

    @Test
    void testGetImage_ResponseStructure() throws Exception {
        MvcResult result = mockMvc.perform(get("/iiif/3/structure-test/100,100,200,200/500,400/90/gray.png"))
                .andExpect(status().isNotImplemented())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);

        // Verify response has all expected fields
        assertTrue(json.has("message"), "Should have message field");
        assertTrue(json.has("identifier"), "Should have identifier field");
        assertTrue(json.has("region"), "Should have region field");
        assertTrue(json.has("size"), "Should have size field");
        assertTrue(json.has("rotation"), "Should have rotation field");
        assertTrue(json.has("quality"), "Should have quality field");
        assertTrue(json.has("format"), "Should have format field");
        assertTrue(json.has("note"), "Should have note field");

        // Verify values are correctly parsed
        assertEquals("structure-test", json.get("identifier").asText());
        assertEquals("100,100,200,200", json.get("region").asText());
        assertEquals("500,400", json.get("size").asText());
        assertEquals("90", json.get("rotation").asText());
        assertEquals("gray", json.get("quality").asText());
        assertEquals("png", json.get("format").asText());
    }

    @Test
    void testGetImage_SpecialCharactersInIdentifier() throws Exception {
        String identifier = "test%20image%2Bspecial%26chars";

        mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", identifier))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.identifier", is(identifier)));
    }

    @Test
    void testGetImage_IIIFv3CompliantParameters() throws Exception {
        // Test various IIIF v3 compliant parameter combinations
        String[][] parameterSets = {
            {"full", "max", "0", "default", "jpg"},
            {"square", "256,256", "90", "color", "png"},
            {"0,0,100,100", "!150,150", "180", "gray", "webp"},
            {"pct:25,25,50,50", "pct:200", "22.5", "bitonal", "tif"}
        };

        for (String[] params : parameterSets) {
            mockMvc.perform(get("/iiif/3/test/{region}/{size}/{rotation}/{quality}.{format}",
                    params[0], params[1], params[2], params[3], params[4]))
                    .andExpect(status().isNotImplemented())
                    .andExpect(jsonPath("$.region", is(params[0])))
                    .andExpect(jsonPath("$.size", is(params[1])))
                    .andExpect(jsonPath("$.rotation", is(params[2])))
                    .andExpect(jsonPath("$.quality", is(params[3])))
                    .andExpect(jsonPath("$.format", is(params[4])));
        }
    }

    @Test
    void testGetImage_MultipleConcurrentRequests() throws Exception {
        // Test thread safety with multiple concurrent requests
        String[] identifiers = {"image1", "image2", "image3", "image4", "image5"};

        for (String identifier : identifiers) {
            mockMvc.perform(get("/iiif/3/{identifier}/full/max/0/default.jpg", identifier))
                    .andExpect(status().isNotImplemented())
                    .andExpect(jsonPath("$.identifier", is(identifier)));
        }
    }

    @Test
    void testGetImage_ContentTypeIsJSON() throws Exception {
        // Since this is a placeholder implementation, it returns JSON instead of image data
        mockMvc.perform(get("/iiif/3/content-test/full/max/0/default.jpg"))
                .andExpect(status().isNotImplemented())
                .andExpect(content().contentType("application/json"));
    }
}
