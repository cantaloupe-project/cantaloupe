package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AutomaticSelectionStrategyTest extends BaseTest {

    private AutomaticSelectionStrategy instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new AutomaticSelectionStrategy();
    }

    @Test
    void getPreferredProcessorsWithJP2() {
        List<?> expected = List.of(
                KakaduNativeProcessor.class,
                OpenJpegProcessor.class,
                GrokProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.get("jp2")));
    }

    @Test
    void getPreferredProcessorsWithJPG() {
        List<?> expected = List.of(
                TurboJpegProcessor.class,
                Java2dProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.get("jpg")));
    }

    @Test
    void getPreferredProcessorsWithPDF() {
        List<?> expected = List.of(PdfBoxProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.get("pdf")));
    }

    @Test
    void getPreferredProcessorsWithVideo() {
        List<?> expected = List.of(FfmpegProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.get("mpg")));
    }

    @Test
    void getPreferredProcessorsWithOther() {
        List<?> expected = List.of(Java2dProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.get("bmp")));
        assertEquals(expected, instance.getPreferredProcessors(Format.get("gif")));
        assertEquals(expected, instance.getPreferredProcessors(Format.get("png")));
        assertEquals(expected, instance.getPreferredProcessors(Format.get("tif")));
    }

    @Test
    void testToString() {
        assertEquals("AutomaticSelectionStrategy", instance.toString());
    }

}
