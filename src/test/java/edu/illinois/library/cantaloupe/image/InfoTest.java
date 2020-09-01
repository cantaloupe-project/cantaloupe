package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class InfoTest extends BaseTest {

    /*********************** Builder tests **************************/

    @Nested
    public class BuilderTest {

        @Test
        void testWithFormat() {
            Info info = Info.builder().withFormat(Format.get("png")).build();
            assertEquals(Format.get("png"), info.getSourceFormat());
        }

        @Test
        void testWithIdentifier() {
            Identifier identifier = new Identifier("cats");
            Info info = Info.builder().withIdentifier(identifier).build();
            assertEquals(identifier, info.getIdentifier());
        }

        @Test
        void testWithMetadata() {
            Metadata metadata = new Metadata();
            Info info = Info.builder().withMetadata(metadata).build();
            assertEquals(metadata, info.getMetadata());
        }

        @Test
        void testWithNumResolutions() {
            Info info = Info.builder().withNumResolutions(5).build();
            assertEquals(5, info.getNumResolutions());
        }

        @Test
        void testWithSize1() {
            Dimension size = new Dimension(45, 50);
            Info info = Info.builder().withSize(size).build();
            assertEquals(size, info.getSize());
        }

        @Test
        void testWithSize2() {
            int width = 45;
            int height = 50;
            Info info = Info.builder().withSize(width, height).build();
            assertEquals(new Dimension(45, 50), info.getSize());
        }

        @Test
        void testWithTileSize1() {
            Dimension size = new Dimension(45, 50);
            Info info = Info.builder().withTileSize(size).build();
            assertEquals(size, info.getImages().get(0).getTileSize());
        }

        @Test
        void testWithTileSize2() {
            int width = 45;
            int height = 50;
            Info info = Info.builder().withTileSize(width, height).build();
            assertEquals(new Dimension(width, height),
                    info.getImages().get(0).getTileSize());
        }

    }

    /********************* Info.Image tests *************************/

    @Nested
    public class InfoImageTest {

        @Test
        void testConstructor() {
            Info.Image image = new Info.Image();
            assertEquals(new Dimension(0, 0), image.getSize());
            assertEquals(new Dimension(0, 0), image.getTileSize());
        }

        @Test
        void testEqualsWithEqualInstances() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Dimension(100, 50));
            image1.setTileSize(new Dimension(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(image1.getSize());
            image2.setTileSize(image1.getTileSize());

            assertEquals(image1, image2);
        }

        @Test
        void testEqualsWithUnequalSizes() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Dimension(100, 50));
            image1.setTileSize(new Dimension(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(new Dimension(100, 49));
            image2.setTileSize(image1.getTileSize());

            assertNotEquals(image1, image2);
        }

        @Test
        void testEqualsWithUnequalTileSizes() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Dimension(100, 50));
            image1.setTileSize(new Dimension(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(image1.getSize());
            image2.setTileSize(new Dimension(50, 24));

            assertNotEquals(image1, image2);
        }

        @Test
        void testHashCodeWithEqualInstances() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Dimension(100, 50));
            image1.setTileSize(new Dimension(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(image1.getSize());
            image2.setTileSize(image1.getTileSize());

            assertEquals(image1.hashCode(), image2.hashCode());
        }

        @Test
        void testHashCodeWithUnequalSizes() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Dimension(100, 50));
            image1.setTileSize(new Dimension(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(new Dimension(100, 49));
            image2.setTileSize(image1.getTileSize());

            assertNotEquals(image1.hashCode(), image2.hashCode());
        }

        @Test
        void testHashCodeWithUnequalTileSizes() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Dimension(100, 50));
            image1.setTileSize(new Dimension(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(image1.getSize());
            image2.setTileSize(new Dimension(50, 24));

            assertNotEquals(image1.hashCode(), image2.hashCode());
        }

    }

    private Info instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        final Metadata metadata = new Metadata();
        metadata.setXMP("<cats/>");

        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(3)
                .withMetadata(metadata)
                .build();
    }

    /************************ Info tests ****************************/

    /* fromJSON(Path) */

    @Test
    void testFromJSONWithPath() throws Exception {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("test", "json");

            // Serialize the instance to JSON and write it to a file.
            String json = instance.toJSON();
            Files.write(tempFile, json.getBytes(StandardCharsets.UTF_8));

            Info info = Info.fromJSON(tempFile);
            assertEquals(info.toString(), instance.toString());
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /* fromJSON(InputStream) */

    @Test
    void testFromJSONWithInputStream() throws Exception {
        String json = instance.toJSON();
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        Info info = Info.fromJSON(inputStream);
        assertEquals(info.toString(), instance.toString());
    }

    /* fromJSON(String) */

    @Test
    void testFromJSONWithString() throws Exception {
        String json = instance.toJSON();
        Info info = Info.fromJSON(json);
        assertEquals(info.toString(), instance.toString());
    }

    /* fromJSON() serialization */

    @Test
    void testFromJSONWithVersion34Serialization() throws Exception {
        String v34json = "{\n" +
                "  \"mediaType\": \"image/jpeg\",\n" +
                "  \"images\": [\n" +
                "    {\n" +
                "      \"width\": 100,\n" +
                "      \"height\": 80,\n" +
                "      \"tileWidth\": 50,\n" +
                "      \"tileHeight\": 40\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        Info actual = Info.fromJSON(v34json);
        Info expected = Info.builder()
                .withFormat(Format.get("jpg"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .build();
        assertEquals(expected, actual);
    }

    @Test
    void testFromJSONWithVersion4Serialization() throws Exception {
        String v4json = "{\n" +
                "  \"identifier\": \"cats\",\n" +
                "  \"mediaType\": \"image/jpeg\",\n" +
                "  \"numResolutions\": 3,\n" +
                "  \"images\": [\n" +
                "    {\n" +
                "      \"width\": 100,\n" +
                "      \"height\": 80,\n" +
                "      \"tileWidth\": 50,\n" +
                "      \"tileHeight\": 40\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        Info actual = Info.fromJSON(v4json);
        Info expected = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withFormat(Format.get("jpg"))
                .withNumResolutions(3)
                .withSize(100, 80)
                .withTileSize(50, 40)
                .build();
        assertEquals(expected, actual);
    }

    @Test
    void testFromJSONWithVersion5Serialization() throws Exception {
        String v5json = "{\n" +
                "  \"identifier\": \"cats\",\n" +
                "  \"mediaType\": \"image/jpeg\",\n" +
                "  \"numResolutions\": 3,\n" +
                "  \"images\": [\n" +
                "    {\n" +
                "      \"width\": 100,\n" +
                "      \"height\": 80,\n" +
                "      \"tileWidth\": 50,\n" +
                "      \"tileHeight\": 40\n" +
                "    }\n" +
                "  ],\n" +
                "  \"metadata\": {\n" +
                "    \"xmp\": \"<cats/>\"\n" +
                "  }\n" +
                "}";
        Metadata metadata = new Metadata();
        metadata.setXMP("<cats/>");
        Info actual = Info.fromJSON(v5json);
        Info expected = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withFormat(Format.get("jpg"))
                .withNumResolutions(3)
                .withMetadata(metadata)
                .withSize(100, 80)
                .withTileSize(50, 40)
                .build();
        assertEquals(expected, actual);
    }

    /* Info() */

    @Test
    void testConstructor() {
        instance = new Info();
        assertEquals(1, instance.getImages().size());
        assertNotNull(instance.getMetadata());
    }

    /* equals() */

    @Test
    void testEqualsWithEqualInstances() {
        Metadata metadata2 = new Metadata();
        metadata2.setXMP("<cats/>");

        Info info2 = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withMetadata(metadata2)
                .withFormat(Format.get("jpg"))
                .build();
        assertEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentIdentifiers() {
        Info info2 = Info.builder()
                .withIdentifier(new Identifier("dogs"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentWidths() {
        Info info2 = Info.builder()
                .withSize(99, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentHeights() {
        Info info2 = Info.builder()
                .withSize(100, 79)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentTileWidths() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(49, 40)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentTileHeights() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 39)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentMetadatas() {
        Metadata metadata2 = new Metadata();
        metadata2.setXMP("<dogs/>");

        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withMetadata(metadata2)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentNumResolutions() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(2)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentFormats() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.get("gif"))
                .build();
        assertNotEquals(instance, info2);
    }

    /* getImages() */

    @Test
    void testGetImages() {
        assertEquals(1, instance.getImages().size());
    }

    /* getMetadata() */

    @Test
    void testGetMetadata() {
        assertEquals("<cats/>", instance.getMetadata().getXMP().orElseThrow());
    }

    /* getNumResolutions() */

    @Test
    void testGetNumResolutions() {
        assertEquals(3, instance.getNumResolutions());
    }

    /* adjustedSize() */

    @Test
    void testGetSize() {
        assertEquals(new Dimension(100, 80), instance.getSize());
    }

    /* adjustedSize(int) */

    @Test
    void testGetSizeWithIndex() {
        Info.Image image = new Info.Image();
        image.width = 50;
        image.height = 40;
        instance.getImages().add(image);

        image = new Info.Image();
        image.width = 25;
        image.height = 20;
        instance.getImages().add(image);

        assertEquals(new Dimension(25, 20), instance.getSize(2));
    }

    /* getSourceFormat() */

    @Test
    void testGetSourceFormat() {
        assertEquals(Format.get("jpg"), instance.getSourceFormat());

        instance.setSourceFormat(null);
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    /* hashCode() */

    @Test
    void testHashCodeWithEqualInstances() {
        Metadata metadata2 = new Metadata();
        metadata2.setXMP("<cats/>");

        Info info2 = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withMetadata(metadata2)
                .withFormat(Format.get("jpg"))
                .build();
        assertEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentIdentifiers() {
        Info info2 = Info.builder()
                .withIdentifier(new Identifier("dogs"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentWidths() {
        Info info2 = Info.builder()
                .withSize(99, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentHeights() {
        Info info2 = Info.builder()
                .withSize(100, 79)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentTileWidths() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(49, 40)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentTileHeights() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 39)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentMetadatas() {
        Metadata metadata2 = new Metadata();
        metadata2.setXMP("<dogs/>");

        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withMetadata(metadata2)
                .withNumResolutions(3)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentNumResolutions() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(2)
                .withFormat(Format.get("jpg"))
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentFormats() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.get("gif"))
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    /* toJSON() */

    @Test
    void testToJSONContents() throws Exception {
        assertEquals(
                "{" +
                        "\"identifier\":\"cats\"," +
                        "\"mediaType\":\"image/jpeg\"," +
                        "\"numResolutions\":3," +
                        "\"images\":[" +
                        "{\"width\":100," +
                        "\"height\":80," +
                        "\"tileWidth\":50," +
                        "\"tileHeight\":40" +
                        "}" +
                        "]," +
                        "\"metadata\":{" +
                        "\"xmp\":\"<cats/>\"" +
                        "}" +
                        "}",
                instance.toJSON());
    }

    @Test
    void testToJSONRoundTrip() throws Exception {
        String json = instance.toJSON();
        Info info2 = Info.fromJSON(json);
        assertEquals(instance, info2);
    }

    @Test
    void testToJSONOmitsNullValues() throws Exception {
        String json = instance.toJSON();
        assertFalse(json.contains("null"));
    }

    /* toString() */

    @Test
    void testToString() throws Exception {
        assertEquals(instance.toJSON(), instance.toString());
    }

    /* writeAsJSON() */

    @Test
    void testWriteAsJSON() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        instance.writeAsJSON(baos);
        assertArrayEquals(baos.toByteArray(), instance.toJSON().getBytes());
    }

}
