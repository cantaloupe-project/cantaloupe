package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.Application;
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
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InfoTest extends BaseTest {

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
            assertEquals(obscureTimestamps(info.toString()),
                    obscureTimestamps(instance.toString()));
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
        assertEquals(obscureTimestamps(info.toString()),
                obscureTimestamps(instance.toString()));
    }

    /* fromJSON(String) */

    @Test
    void testFromJSONWithString() throws Exception {
        String json = instance.toJSON();
        Info info = Info.fromJSON(json);
        assertEquals(obscureTimestamps(info.toString()),
                obscureTimestamps(instance.toString()));
    }

    /* fromJSON() serialization */

    @Test
    void testFromJSONWithVersion2Serialization() throws Exception {
        String v2json = "{\n" +
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
        Info actual = Info.fromJSON(v2json);
        Info expected = Info.builder()
                .withFormat(Format.get("jpg"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .build();
        assertEquals(expected, actual);
    }

    @Test
    void testFromJSONWithVersion3Serialization() throws Exception {
        String v3json = "{\n" +
                "  \"identifier\": \"cats\",\n" +
                "  \"mediaType\": \"image/jpeg\",\n" +
                "  \"numResolutions\": 3,\n" +
                "  \"images\": [\n" +
                "    {\n" +
                "      \"width\": 100,\n" +
                "      \"height\": 80,\n" +
                "      \"tileWidth\": 50,\n" +
                "      \"tileHeight\": 40,\n" +
                "      \"orientation\": 1\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        Info actual = Info.fromJSON(v3json);
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
    void testFromJSONWithVersion4Serialization() throws Exception {
        String v4json = "{\n" +
                "  \"applicationVersion\": \"5.0\",\n" +
                "  \"serializationVersion\": 4,\n" +
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
        Info actual = Info.fromJSON(v4json);

        Metadata metadata = new Metadata();
        metadata.setXMP("<cats/>");
        Info expected = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withFormat(Format.get("jpg"))
                .withNumResolutions(3)
                .withMetadata(metadata)
                .withSize(100, 80)
                .withTileSize(50, 40)
                .build();
        expected.setApplicationVersion("5.0");
        actual.setSerializationVersion(Info.Serialization.CURRENT.getVersion());
        assertEquals(expected, actual);
    }

    @Test
    void testFromJSONWithVersion5Serialization() throws Exception {
        Instant timestamp = Instant.now();
        String v6json = "{\n" +
                "  \"applicationVersion\": \"6.0\",\n" +
                "  \"serializationVersion\": 5,\n" +
                "  \"serializationTimestamp\": \"" + timestamp.toString() + "\",\n" +
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
        Info actual = Info.fromJSON(v6json);
        Info expected = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withFormat(Format.get("jpg"))
                .withNumResolutions(3)
                .withMetadata(metadata)
                .withSize(100, 80)
                .withTileSize(50, 40)
                .build();
        expected.setApplicationVersion("6.0");
        expected.setSerializationTimestamp(timestamp);
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
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentApplicationVersions() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        info2.setApplicationVersion("99.0");
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentSerializationVersions() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        info2.setSerializationVersion(2);
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentSerializationTimestamps() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        info2.setSerializationTimestamp(Instant.now());
        assertEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentIdentifiers() {
        Info info2 = Info.builder()
                .withIdentifier(new Identifier("mules"))
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentWidths() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(999, instance.getSize().intHeight())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentHeights() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize().intWidth(), 999)
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentTileWidths() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(999, instance.getImages().get(0).getTileSize().intHeight())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentTileHeights() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize().intWidth(), 999)
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentMetadatas() {
        Metadata metadata2 = new Metadata();
        metadata2.setXMP("<dogs/>");

        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(metadata2)
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentNumResolutions() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions() + 1)
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    void testEqualsWithDifferentFormats() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(Format.get("gif"))
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance, info2);
    }

    /* getApplicationVersion() */

    @Test
    void testGetApplicationVersion() {
        assertEquals(Application.getVersion(),
                instance.getApplicationVersion());
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

    /* getNumPages() */

    @Test
    void testGetNumPagesWithSingleResolutionImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(1)
                .build();
        assertEquals(1, instance.getNumPages());
    }

    @Test
    void testGetNumPagesWithPyramidalImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(1000, 800)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(6)
                .build();
        // level 2
        Info.Image image = new Info.Image();
        image.setSize(new Dimension(500, 400));
        instance.getImages().add(image);
        // level 3
        image = new Info.Image();
        image.setSize(new Dimension(250, 200));
        instance.getImages().add(image);
        // level 4
        image = new Info.Image();
        image.setSize(new Dimension(125, 100));
        instance.getImages().add(image);
        // level 5
        image = new Info.Image();
        image.setSize(new Dimension(63, 50));
        instance.getImages().add(image);
        // level 6
        image = new Info.Image();
        image.setSize(new Dimension(32, 25));
        instance.getImages().add(image);

        assertEquals(1, instance.getNumPages());
    }

    @Test
    void testGetNumPagesWithNonPyramidalMultiImageImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(1000, 800)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(1)
                .build();
        Info.Image image = new Info.Image();
        image.setSize(new Dimension(600, 300));
        instance.getImages().add(image);
        // level 3
        image = new Info.Image();
        image.setSize(new Dimension(200, 900));
        instance.getImages().add(image);

        assertEquals(3, instance.getNumPages());
    }

    /* getNumResolutions() */

    @Test
    void testGetNumResolutions() {
        assertEquals(3, instance.getNumResolutions());
    }

    /* getSerialization() */

    @Test
    void testGetSerialization() {
        assertEquals(Info.Serialization.CURRENT, instance.getSerialization());
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
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(metadata2)
                .build();
        assertEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentSerializationTimestamps() {
        Metadata metadata2 = new Metadata();
        metadata2.setXMP("<cats/>");

        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(metadata2)
                .build();
        info2.setSerializationTimestamp(Instant.now());
        assertEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentIdentifiers() {
        Info info2 = Info.builder()
                .withIdentifier(new Identifier("cows"))
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentWidths() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(999, instance.getSize().intHeight())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentHeights() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize().intWidth(), 999)
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentTileWidths() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(999, instance.getImages().get(0).getTileSize().intHeight())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentTileHeights() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize().intWidth(), 999)
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentMetadatas() {
        Metadata metadata2 = new Metadata();
        metadata2.setXMP("<dogs/>");

        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(metadata2)
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentNumResolutions() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(instance.getSourceFormat())
                .withNumResolutions(instance.getNumResolutions() + 1)
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentFormats() {
        Info info2 = Info.builder()
                .withIdentifier(instance.getIdentifier())
                .withSize(instance.getSize())
                .withTileSize(instance.getImages().get(0).getTileSize())
                .withFormat(Format.get("gif"))
                .withNumResolutions(instance.getNumResolutions())
                .withMetadata(instance.getMetadata())
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    /* isPyramid() */

    @Test
    void testIsPyramidWithSingleResolutionImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(1)
                .build();
        assertFalse(instance.isPyramid());
    }

    @Test
    void testIsPyramidWithPyramidalImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(1000, 800)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(6)
                .build();
        // level 2
        Info.Image image = new Info.Image();
        image.setSize(new Dimension(500, 400));
        instance.getImages().add(image);
        // level 3
        image = new Info.Image();
        image.setSize(new Dimension(250, 200));
        instance.getImages().add(image);
        // level 4
        image = new Info.Image();
        image.setSize(new Dimension(125, 100));
        instance.getImages().add(image);
        // level 5
        image = new Info.Image();
        image.setSize(new Dimension(63, 50));
        instance.getImages().add(image);
        // level 6
        image = new Info.Image();
        image.setSize(new Dimension(32, 25));
        instance.getImages().add(image);

        assertTrue(instance.isPyramid());
    }

    @Test
    void testIsPyramidWithNonPyramidalMultiImageImage() {
        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(1000, 800)
                .withFormat(Format.get("jpg"))
                .withNumResolutions(1)
                .build();
        Info.Image image = new Info.Image();
        image.setSize(new Dimension(600, 300));
        instance.getImages().add(image);
        // level 3
        image = new Info.Image();
        image.setSize(new Dimension(200, 900));
        instance.getImages().add(image);

        assertFalse(instance.isPyramid());
    }

    /* setApplicationVersion() */

    @Test
    void testSetApplicationVersion() {
        String version = "Some Version";
        instance.setApplicationVersion(version);
        assertEquals(version, instance.getApplicationVersion());
    }

    /* setIdentifier() */

    @Test
    void testSetIdentifier() {
        Identifier identifier = new Identifier("Some Identifier");
        instance.setIdentifier(identifier);
        assertEquals(identifier, instance.getIdentifier());
    }

    /* setMediaType() */

    @Test
    void testSetMediaType() {
        MediaType type = new MediaType("image/jpg");
        instance.setMediaType(type);
        assertEquals(type, instance.getMediaType());
    }

    /* setMetadata() */

    @Test
    void testSetMetadata() {
        Metadata metadata = new Metadata();
        instance.setMetadata(metadata);
        assertSame(metadata, instance.getMetadata());
    }

    /* setNumResolutions() */

    @Test
    void testSetNumResolutions() {
        instance.setNumResolutions(7);
        assertEquals(7, instance.getNumResolutions());
    }

    /* setPersistable() */

    @Test
    void testSetPersistable() {
        instance.setPersistable(true);
        assertTrue(instance.isPersistable());
        instance.setPersistable(false);
        assertFalse(instance.isPersistable());
    }

    /* setSerializationTimestamp() */

    @Test
    void testSetSerializationTimestamp() {
        Instant timestamp = Instant.now();
        instance.setSerializationTimestamp(timestamp);
        assertEquals(timestamp, instance.getSerializationTimestamp());
    }

    /* setSerializationVersion() */

    @Test
    void testSetSerializationVersionWithIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setSerializationVersion(99));
    }

    @Test
    void testSetSerializationVersion() {
        instance.setSerializationVersion(2);
        assertEquals(Info.Serialization.VERSION_2, instance.getSerialization());
    }

    /* setSourceFormat() */

    @Test
    void testSetSourceFormat() {
        Format format = Format.get("png");
        instance.setSourceFormat(format);
        assertEquals(format, instance.getSourceFormat());
    }

    /* toJSON() */

    @Test
    void testToJSONContents() throws Exception {
        assertEquals("{" +
                        "\"applicationVersion\":\"" + Application.getVersion() + "\"," +
                        "\"serializationVersion\":" + Info.Serialization.CURRENT.getVersion() + "," +
                        "\"serializationTimestamp\":\"0000-00-00T00:00:00.000000Z\"," +
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
                obscureTimestamps(instance.toJSON()));
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
        assertEquals(obscureTimestamps(instance.toJSON()),
                obscureTimestamps(instance.toString()));
    }

    /* writeAsJSON() */

    @Test
    void testWriteAsJSON() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        instance.writeAsJSON(baos);

        String expected = baos.toString(StandardCharsets.UTF_8);
        String actual   = new String(instance.toJSON().getBytes(),
                StandardCharsets.UTF_8);
        assertEquals(obscureTimestamps(expected), obscureTimestamps(actual));
    }

    /**
     * Converts any ISO-8601 timestamps in the given string to
     * {@literal 0000-00-00T00:00:00.000000Z}.
     */
    private static String obscureTimestamps(String inString) {
        return inString.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d+Z",
                "0000-00-00T00:00:00.000000Z");
    }

}
