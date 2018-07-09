package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class AutomaticSelectionStrategyTest extends BaseTest {

    private AutomaticSelectionStrategy instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new AutomaticSelectionStrategy();
    }

    @Test
    public void getPreferredProcessorsWithPDF() {
        List<?> expected = Arrays.asList(PdfBoxProcessor.class,
                GraphicsMagickProcessor.class, ImageMagickProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.PDF));
    }

    @Test
    public void getPreferredProcessorsWithVideo() {
        List<?> expected = Arrays.asList(FfmpegProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.MPG));
    }

    @Test
    public void getPreferredProcessorsWithJP2() {
        List<?> expected = Arrays.asList(KakaduNativeProcessor.class,
                KakaduDemoProcessor.class, OpenJpegProcessor.class,
                GraphicsMagickProcessor.class, ImageMagickProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.JP2));
    }

    @Test
    public void getPreferredProcessorsWithOther() {
        List<?> expected = Arrays.asList(Java2dProcessor.class,
                GraphicsMagickProcessor.class, ImageMagickProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.BMP));
        assertEquals(expected, instance.getPreferredProcessors(Format.GIF));
        assertEquals(expected, instance.getPreferredProcessors(Format.JPG));
        assertEquals(expected, instance.getPreferredProcessors(Format.PNG));
        assertEquals(expected, instance.getPreferredProcessors(Format.TIF));
    }

    @Test
    public void testToString() {
        assertEquals("AutomaticSelectionStrategy", instance.toString());
    }

}
