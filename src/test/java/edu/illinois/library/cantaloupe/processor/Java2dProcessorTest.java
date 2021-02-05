package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class Java2dProcessorTest extends AbstractImageIOProcessorTest {

    private Java2dProcessor instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Override
    protected Java2dProcessor newInstance() {
        return new Java2dProcessor();
    }

    @Test
    void testIsSeekingWithNonSeekableSource() throws Exception {
        instance.setSourceFormat(Format.get("bmp"));
        instance.setSourceFile(TestUtil.getImage("bmp"));
        assertFalse(instance.isSeeking());
    }

    @Test
    void testIsSeekingWithSeekableSource() throws Exception {
        instance.setSourceFormat(Format.get("tif"));
        instance.setSourceFile(TestUtil.getImage("tif-rgb-1res-64x56x8-tiled-jpeg.tif"));
        assertTrue(instance.isSeeking());
    }

    @Test
    void testProcessWithAnimatedGIF() throws Exception {
        Path image = TestUtil.getImage("gif-animated-looping.gif");
        OperationList ops = OperationList.builder()
                .withOperations(new Encode(Format.get("gif")))
                .build();
        Info info = Info.builder()
                .withSize(136, 200)
                .withFormat(Format.get("gif"))
                .build();

        instance.setSourceFile(image);
        instance.setSourceFormat(Format.get("gif"));

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            instance.process(ops, info, os);

            try (ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray())) {
                ImageReader reader = null;
                try {
                    reader = new ImageReaderFactory().newImageReader(Format.get("gif"), is);
                    assertEquals(2, reader.getNumImages());
                } finally {
                    if (reader != null) {
                        reader.dispose();
                    }
                }
            }
        }
    }

    @Test
    @Override
    public void testProcessWithTurboJPEGAvailable() {
        // This processor doesn't use TurboJPEG ever.
    }

    @Test
    @Override
    public void testProcessWithTurboJPEGNotAvailable() {
        // This processor doesn't use TurboJPEG ever.
    }

    @Test
    void testSupportsSourceFormatWithSupportedFormat() {
        try (Processor instance = newInstance()) {
            assertTrue(instance.supportsSourceFormat(Format.get("jpg")));
        }
    }

    @Test
    void testSupportsSourceFormatWithUnsupportedFormat() {
        try (Processor instance = newInstance()) {
            assertFalse(instance.supportsSourceFormat(Format.get("mp4")));
        }
    }

}
