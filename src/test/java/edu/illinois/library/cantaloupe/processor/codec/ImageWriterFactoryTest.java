package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ImageWriterFactoryTest extends BaseTest {

    private ImageWriterFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ImageWriterFactory();
    }

    @Test
    void testSupportedFormats() {
        Set<Format> outputFormats = Set.of(
                Format.get("gif"), Format.get("jpg"), Format.get("png"), Format.get("tif"));
        assertEquals(outputFormats, ImageWriterFactory.supportedFormats());
    }

    @Test
    void testNewImageWriter() throws Exception {
        assertNotNull(instance.newImageWriter(new Encode(Format.get("jpg"))));
    }

    @Test
    void testNewImageWriterWithUnsupportedFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.newImageWriter(new Encode(Format.UNKNOWN)));
    }

}
