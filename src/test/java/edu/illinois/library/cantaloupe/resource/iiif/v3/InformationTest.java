package edu.illinois.library.cantaloupe.resource.iiif.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for IIIF v3 Information class.
 * Tests the basic functionality of the Information response object.
 */
class InformationTest {

    private Information<String, Object> information;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        information = new Information<>();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testInformationCreation() {
        assertNotNull(information);
        assertTrue(information.isEmpty());
    }

    @Test
    void testBasicIIIFv3Properties() {
        // Set up basic IIIF v3 properties
        information.put("@context", "http://iiif.io/api/image/3/context.json");
        information.put("id", "https://example.com/iiif/3/test-image");
        information.put("type", "ImageService3");
        information.put("protocol", "http://iiif.io/api/image");
        information.put("profile", "level2");
        information.put("width", 1000);
        information.put("height", 800);

        assertEquals("http://iiif.io/api/image/3/context.json", information.get("@context"));
        assertEquals("https://example.com/iiif/3/test-image", information.get("id"));
        assertEquals("ImageService3", information.get("type"));
        assertEquals("http://iiif.io/api/image", information.get("protocol"));
        assertEquals("level2", information.get("profile"));
        assertEquals(1000, information.get("width"));
        assertEquals(800, information.get("height"));
    }

    @Test
    void testArrayProperties() {
        // Test format array
        information.put("format", Arrays.asList("jpg", "png", "webp"));

        // Test quality array
        information.put("quality", Arrays.asList("default", "color", "gray", "bitonal"));

        assertTrue(information.get("format") instanceof java.util.List);
        assertTrue(information.get("quality") instanceof java.util.List);

        @SuppressWarnings("unchecked")
        java.util.List<String> formats = (java.util.List<String>) information.get("format");
        assertEquals(3, formats.size());
        assertTrue(formats.contains("jpg"));
        assertTrue(formats.contains("png"));
        assertTrue(formats.contains("webp"));

        @SuppressWarnings("unchecked")
        java.util.List<String> qualities = (java.util.List<String>) information.get("quality");
        assertEquals(4, qualities.size());
        assertTrue(qualities.contains("default"));
        assertTrue(qualities.contains("color"));
        assertTrue(qualities.contains("gray"));
        assertTrue(qualities.contains("bitonal"));
    }

    @Test
    void testJsonSerialization() throws JsonProcessingException {
        // Set up complete IIIF v3 information object
        information.put("@context", "http://iiif.io/api/image/3/context.json");
        information.put("id", "https://example.com/iiif/3/test");
        information.put("type", "ImageService3");
        information.put("protocol", "http://iiif.io/api/image");
        information.put("profile", "level2");
        information.put("width", 2000);
        information.put("height", 1500);
        information.put("format", Arrays.asList("jpg", "png"));
        information.put("quality", Arrays.asList("default", "color"));

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(information);
        assertNotNull(json);
        assertFalse(json.isEmpty());

        // Parse back and verify
        JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("http://iiif.io/api/image/3/context.json", jsonNode.get("@context").asText());
        assertEquals("https://example.com/iiif/3/test", jsonNode.get("id").asText());
        assertEquals("ImageService3", jsonNode.get("type").asText());
        assertEquals(2000, jsonNode.get("width").asInt());
        assertEquals(1500, jsonNode.get("height").asInt());

        // Verify arrays
        assertTrue(jsonNode.get("format").isArray());
        assertTrue(jsonNode.get("quality").isArray());
        assertEquals(2, jsonNode.get("format").size());
        assertEquals(2, jsonNode.get("quality").size());
    }

    @Test
    void testPropertyOrder() throws JsonProcessingException {
        // IIIF requires certain properties to be in a specific order
        information.put("width", 1000);
        information.put("@context", "http://iiif.io/api/image/3/context.json");
        information.put("height", 800);
        information.put("id", "https://example.com/test");
        information.put("type", "ImageService3");

        String json = objectMapper.writeValueAsString(information);

        // LinkedHashMap should preserve insertion order
        assertNotNull(json);
        assertTrue(json.contains("@context"));
        assertTrue(json.contains("id"));
        assertTrue(json.contains("type"));
    }

    @Test
    void testOptionalProperties() {
        // Test optional IIIF v3 properties
        information.put("maxWidth", 4000);
        information.put("maxHeight", 3000);
        information.put("maxArea", 12000000);
        information.put("rights", "http://creativecommons.org/licenses/by/3.0/");
        information.put("requiredStatement", "Image courtesy of Example Institution");

        assertEquals(4000, information.get("maxWidth"));
        assertEquals(3000, information.get("maxHeight"));
        assertEquals(12000000, information.get("maxArea"));
        assertEquals("http://creativecommons.org/licenses/by/3.0/", information.get("rights"));
        assertEquals("Image courtesy of Example Institution", information.get("requiredStatement"));
    }

    @Test
    void testComplexStructures() {
        // Test more complex IIIF structures like tiles
        Information.Size tile = new Information.Size();
        tile.width = 256;
        tile.height = 256;

        information.put("tiles", Arrays.asList(tile));

        assertTrue(information.get("tiles") instanceof java.util.List);

        @SuppressWarnings("unchecked")
        java.util.List<Information.Size> tiles = (java.util.List<Information.Size>) information.get("tiles");
        assertEquals(1, tiles.size());
        assertEquals(256, tiles.get(0).width);
        assertEquals(256, tiles.get(0).height);
    }

    @Test
    void testInheritedMapBehavior() {
        // Test that Information behaves like a standard Map
        assertTrue(information.isEmpty());
        assertEquals(0, information.size());

        information.put("test", "value");
        assertFalse(information.isEmpty());
        assertEquals(1, information.size());
        assertTrue(information.containsKey("test"));
        assertTrue(information.containsValue("value"));

        information.remove("test");
        assertTrue(information.isEmpty());
        assertFalse(information.containsKey("test"));
    }

    @Test
    void testNullValues() {
        // Test handling of null values
        information.put("optional", null);
        assertTrue(information.containsKey("optional"));
        assertNull(information.get("optional"));

        information.put("required", "value");
        information.put("required", null);
        assertTrue(information.containsKey("required"));
        assertNull(information.get("required"));
    }

    @Test
    void testStringRepresentation() {
        information.put("@context", "http://iiif.io/api/image/3/context.json");
        information.put("id", "test");
        information.put("width", 100);

        String str = information.toString();
        assertNotNull(str);
        assertTrue(str.contains("@context"));
        assertTrue(str.contains("test"));
        assertTrue(str.contains("100"));
    }
}
