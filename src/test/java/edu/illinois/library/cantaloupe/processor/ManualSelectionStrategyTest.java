package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ManualSelectionStrategyTest extends BaseTest {

    private ManualSelectionStrategy instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new ManualSelectionStrategy();
    }

    @Test
    public void getPreferredProcessorsWhenOnlyAssignedIsSet() {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.pdf", PdfBoxProcessor.class.getSimpleName());

        List<Class<? extends Processor>> expected =
                Collections.singletonList(PdfBoxProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.PDF));
    }

    @Test
    public void getPreferredProcessorsWhenOnlyFallbackIsSet() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, ImageMagickProcessor.class.getSimpleName());

        List<Class<? extends Processor>> expected =
                Collections.singletonList(ImageMagickProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.GIF));
    }

    @Test
    public void getPreferredProcessorsWhenAssignedAndFallbackAreSet() {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.jpg", GraphicsMagickProcessor.class.getSimpleName());
        config.setProperty(Key.PROCESSOR_FALLBACK, ImageMagickProcessor.class.getSimpleName());

        List<Class<? extends Processor>> expected =
                Arrays.asList(GraphicsMagickProcessor.class, ImageMagickProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.JPG));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPreferredProcessorsWithIllegalAssignedName() {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.jpg", "bogus");
        instance.getPreferredProcessors(Format.JPG);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPreferredProcessorsWithIllegalFallbackName() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, "bogus");
        instance.getPreferredProcessors(Format.JPG);
    }

    @Test
    public void getPreferredProcessorsWithFullyQualifiedNames() {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.jpg", GraphicsMagickProcessor.class.getName());
        config.setProperty(Key.PROCESSOR_FALLBACK, ImageMagickProcessor.class.getName());

        List<Class<? extends Processor>> expected =
                Arrays.asList(GraphicsMagickProcessor.class, ImageMagickProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.JPG));
    }

    @Test
    public void testToString() {
        assertEquals("ManualSelectionStrategy", instance.toString());
    }

}
