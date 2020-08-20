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
        assertEquals(expected, instance.getPreferredProcessors(Format.JP2));
    }

    @Test
    void getPreferredProcessorsWithJPG() {
        List<?> expected = List.of(
                TurboJpegProcessor.class,
                Java2dProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.JPG));
    }

    @Test
    void getPreferredProcessorsWithPDF() {
        List<?> expected = List.of(PdfBoxProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.PDF));
    }

    @Test
    void getPreferredProcessorsWithVideo() {
        List<?> expected = List.of(FfmpegProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.MPG));
    }

    @Test
    void getPreferredProcessorsWithOther() {
        List<?> expected = List.of(Java2dProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.BMP));
        assertEquals(expected, instance.getPreferredProcessors(Format.GIF));
        assertEquals(expected, instance.getPreferredProcessors(Format.PNG));
        assertEquals(expected, instance.getPreferredProcessors(Format.TIF));
    }

    @Test
    void testToString() {
        assertEquals("AutomaticSelectionStrategy", instance.toString());
    }

}
