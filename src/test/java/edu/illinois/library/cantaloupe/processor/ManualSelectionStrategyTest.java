package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ManualSelectionStrategyTest extends BaseTest {

    private ManualSelectionStrategy instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ManualSelectionStrategy();
    }

    @Test
    void getPreferredProcessorsWhenOnlyAssignedIsSet() {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.ManualSelectionStrategy.pdf",
                PdfBoxProcessor.class.getSimpleName());

        List<Class<? extends Processor>> expected =
                Collections.singletonList(PdfBoxProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.PDF));
    }

    @Test
    void getPreferredProcessorsWhenOnlyFallbackIsSet() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, ImageMagickProcessor.class.getSimpleName());

        List<Class<? extends Processor>> expected =
                Collections.singletonList(ImageMagickProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.GIF));
    }

    @Test
    void getPreferredProcessorsWhenAssignedAndFallbackAreSet() {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.ManualSelectionStrategy.jpg",
                GraphicsMagickProcessor.class.getSimpleName());
        config.setProperty(Key.PROCESSOR_FALLBACK,
                ImageMagickProcessor.class.getSimpleName());

        List<Class<? extends Processor>> expected = List.of(
                GraphicsMagickProcessor.class,
                ImageMagickProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.JPG));
    }

    @Test
    void getPreferredProcessorsWithIllegalAssignedName() {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.ManualSelectionStrategy.jpg", "bogus");
        assertThrows(IllegalArgumentException.class,
                () -> instance.getPreferredProcessors(Format.JPG));
    }

    @Test
    void getPreferredProcessorsWithIllegalFallbackName() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, "bogus");
        assertThrows(IllegalArgumentException.class,
                () -> instance.getPreferredProcessors(Format.JPG));
    }

    @Test
    void getPreferredProcessorsWithFullyQualifiedNames() {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.ManualSelectionStrategy.jpg",
                GraphicsMagickProcessor.class.getName());
        config.setProperty(Key.PROCESSOR_FALLBACK, ImageMagickProcessor.class.getName());

        List<Class<? extends Processor>> expected = List.of(
                GraphicsMagickProcessor.class,
                ImageMagickProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.JPG));
    }

    @Test
    void testToString() {
        assertEquals("ManualSelectionStrategy", instance.toString());
    }

}
