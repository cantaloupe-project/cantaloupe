package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class InfoTest extends BaseTest {

    /*********************** Builder tests **************************/

    public static class BuilderTest {

        @Test
        public void testWithFormat() {
            Info info = Info.builder().withFormat(Format.PNG).build();
            assertEquals(Format.PNG, info.getSourceFormat());
        }

        @Test
        public void testWithIdentifier() {
            Identifier identifier = new Identifier("cats");
            Info info = Info.builder().withIdentifier(identifier).build();
            assertEquals(identifier, info.getIdentifier());
        }

        @Test
        public void testWithMetadata() {
            Metadata metadata = new Metadata();
            Info info = Info.builder().withMetadata(metadata).build();
            assertEquals(metadata, info.getMetadata());
        }

        @Test
        public void testWithNumResolutions() {
            Info info = Info.builder().withNumResolutions(5).build();
            assertEquals(5, info.getNumResolutions());
        }

        @Test
        public void testWithSize1() {
            Dimension size = new Dimension(45, 50);
            Info info = Info.builder().withSize(size).build();
            assertEquals(size, info.getSize());
        }

        @Test
        public void testWithSize2() {
            int width = 45;
            int height = 50;
            Info info = Info.builder().withSize(width, height).build();
            assertEquals(new Dimension(45, 50), info.getSize());
        }

        @Test
        public void testWithTileSize1() {
            Dimension size = new Dimension(45, 50);
            Info info = Info.builder().withTileSize(size).build();
            assertEquals(size, info.getImages().get(0).getTileSize());
        }

        @Test
        public void testWithTileSize2() {
            int width = 45;
            int height = 50;
            Info info = Info.builder().withTileSize(width, height).build();
            assertEquals(new Dimension(width, height),
                    info.getImages().get(0).getTileSize());
        }

    }

    /********************* Info.Image tests *************************/

    public static class InfoImageTest {

        @Test
        public void testConstructor() {
            Info.Image image = new Info.Image();
            assertEquals(new Dimension(0, 0), image.getSize());
            assertEquals(new Dimension(0, 0), image.getTileSize());
        }

        @Test
        public void testEqualsWithEqualInstances() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Dimension(100, 50));
            image1.setTileSize(new Dimension(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(image1.getSize());
            image2.setTileSize(image1.getTileSize());

            assertEquals(image1, image2);
        }

        @Test
        public void testEqualsWithUnequalSizes() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Dimension(100, 50));
            image1.setTileSize(new Dimension(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(new Dimension(100, 49));
            image2.setTileSize(image1.getTileSize());

            assertNotEquals(image1, image2);
        }

        @Test
        public void testEqualsWithUnequalTileSizes() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Dimension(100, 50));
            image1.setTileSize(new Dimension(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(image1.getSize());
            image2.setTileSize(new Dimension(50, 24));

            assertNotEquals(image1, image2);
        }

        @Test
        public void testHashCodeWithEqualInstances() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Dimension(100, 50));
            image1.setTileSize(new Dimension(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(image1.getSize());
            image2.setTileSize(image1.getTileSize());

            assertEquals(image1.hashCode(), image2.hashCode());
        }

        @Test
        public void testHashCodeWithUnequalSizes() {
            Info.Image image1 = new Info.Image();
            image1.setSize(new Dimension(100, 50));
            image1.setTileSize(new Dimension(50, 25));

            Info.Image image2 = new Info.Image();
            image2.setSize(new Dimension(100, 49));
            image2.setTileSize(image1.getTileSize());

            assertNotEquals(image1.hashCode(), image2.hashCode());
        }

        @Test
        public void testHashCodeWithUnequalTileSizes() {
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

    @Before
    public void setUp() throws Exception {
        super.setUp();

        final Metadata metadata = new Metadata();
        metadata.setXMP("<cats/>");

        instance = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withFormat(Format.JPG)
                .withNumResolutions(3)
                .withMetadata(metadata)
                .build();
    }

    /************************ Info tests ****************************/

    /* fromJSON(Path) */

    @Test
    public void testFromJSONWithPath() throws Exception {
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
    public void testFromJSONWithInputStream() throws Exception {
        String json = instance.toJSON();
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        Info info = Info.fromJSON(inputStream);
        assertEquals(info.toString(), instance.toString());
    }

    /* fromJSON(String) */

    @Test
    public void testFromJSONWithString() throws Exception {
        String json = instance.toJSON();
        Info info = Info.fromJSON(json);
        assertEquals(info.toString(), instance.toString());
    }

    @Test
    public void testFromJSONWithVersion34Serialization() throws Exception {
        String v4json = "{\n" +
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
                .withFormat(Format.JPG)
                .withNumResolutions(3)
                .withSize(100, 80)
                .withTileSize(50, 40)
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void testFromJSONWithVersion4Serialization() throws Exception {
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
                .withFormat(Format.JPG)
                .withNumResolutions(3)
                .withSize(100, 80)
                .withTileSize(50, 40)
                .build();

        assertEquals(expected, actual);
    }

    /* Info() */

    @Test
    public void testConstructor() {
        instance = new Info();
        assertEquals(1, instance.getImages().size());
        assertNotNull(instance.getMetadata());
    }

    /* equals() */

    @Test
    public void testEqualsWithEqualInstances() {
        Metadata metadata2 = new Metadata();
        metadata2.setXMP("<cats/>");

        Info info2 = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withMetadata(metadata2)
                .withFormat(Format.JPG)
                .build();
        assertEquals(instance, info2);
    }

    @Test
    public void testEqualsWithDifferentIdentifiers() {
        Info info2 = Info.builder()
                .withIdentifier(new Identifier("dogs"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    public void testEqualsWithDifferentWidths() {
        Info info2 = Info.builder()
                .withSize(99, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    public void testEqualsWithDifferentHeights() {
        Info info2 = Info.builder()
                .withSize(100, 79)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    public void testEqualsWithDifferentTileWidths() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(49, 40)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    public void testEqualsWithDifferentTileHeights() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 39)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    public void testEqualsWithDifferentMetadatas() {
        Metadata metadata2 = new Metadata();
        metadata2.setXMP("<dogs/>");

        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withMetadata(metadata2)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    public void testEqualsWithDifferentNumResolutions() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(2)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance, info2);
    }

    @Test
    public void testEqualsWithDifferentFormats() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.GIF)
                .build();
        assertNotEquals(instance, info2);
    }

    /* getImages() */

    @Test
    public void testGetImages() {
        assertEquals(1, instance.getImages().size());
    }

    /* getMetadata() */

    @Test
    public void testGetMetadata() {
        assertEquals("<cats/>", instance.getMetadata().getXMP().orElseThrow());
    }

    /* getNumResolutions() */

    @Test
    public void testGetNumResolutions() {
        assertEquals(3, instance.getNumResolutions());
    }

    /* adjustedSize() */

    @Test
    public void testGetSize() {
        assertEquals(new Dimension(100, 80), instance.getSize());
    }

    /* adjustedSize(int) */

    @Test
    public void testGetSizeWithIndex() {
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
    public void testGetSourceFormat() {
        assertEquals(Format.JPG, instance.getSourceFormat());

        instance.setSourceFormat(null);
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    /* hashCode() */

    @Test
    public void testHashCodeWithEqualInstances() {
        Metadata metadata2 = new Metadata();
        metadata2.setXMP("<cats/>");

        Info info2 = Info.builder()
                .withIdentifier(new Identifier("cats"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withMetadata(metadata2)
                .withFormat(Format.JPG)
                .build();
        assertEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentIdentifiers() {
        Info info2 = Info.builder()
                .withIdentifier(new Identifier("dogs"))
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentWidths() {
        Info info2 = Info.builder()
                .withSize(99, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentHeights() {
        Info info2 = Info.builder()
                .withSize(100, 79)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentTileWidths() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(49, 40)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentTileHeights() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 39)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentMetadatas() {
        Metadata metadata2 = new Metadata();
        metadata2.setXMP("<dogs/>");

        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withMetadata(metadata2)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentNumResolutions() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(2)
                .withFormat(Format.JPG)
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    @Test
    public void testHashCodeWithDifferentFormats() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withNumResolutions(3)
                .withFormat(Format.GIF)
                .build();
        assertNotEquals(instance.hashCode(), info2.hashCode());
    }

    /* toJSON() */

    @Test
    public void testToJSONContents() throws Exception {
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
    public void testToJSONRoundTrip() throws Exception {
        String json = instance.toJSON();
        Info info2 = Info.fromJSON(json);
        assertEquals(instance, info2);
    }

    @Test
    public void testToJSONOmitsNullValues() throws Exception {
        String json = instance.toJSON();
        assertFalse(json.contains("null"));
    }

    /* toString() */

    @Test
    public void testToString() throws Exception {
        assertEquals(instance.toJSON(), instance.toString());
    }

    /* writeAsJSON() */

    @Test
    public void testWriteAsJSON() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        instance.writeAsJSON(baos);
        assertArrayEquals(baos.toByteArray(), instance.toJSON().getBytes());
    }

}
