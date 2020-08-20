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
        assertEquals(expected, instance.getPreferredProcessors(Format.get("pdf")));
    }

    @Test
    void getPreferredProcessorsWhenOnlyFallbackIsSet() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, Java2dProcessor.class.getSimpleName());

        List<Class<? extends Processor>> expected =
                Collections.singletonList(Java2dProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.get("gif")));
    }

    @Test
    void getPreferredProcessorsWhenAssignedAndFallbackAreSet() {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.ManualSelectionStrategy.pdf",
                PdfBoxProcessor.class.getSimpleName());
        config.setProperty(Key.PROCESSOR_FALLBACK,
                Java2dProcessor.class.getSimpleName());

        List<Class<? extends Processor>> expected = List.of(
                PdfBoxProcessor.class,
                Java2dProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.get("pdf")));
    }

    @Test
    void getPreferredProcessorsWithIllegalAssignedName() {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.ManualSelectionStrategy.jpg", "bogus");
        assertThrows(IllegalArgumentException.class,
                () -> instance.getPreferredProcessors(Format.get("jpg")));
    }

    @Test
    void getPreferredProcessorsWithIllegalFallbackName() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, "bogus");
        assertThrows(IllegalArgumentException.class,
                () -> instance.getPreferredProcessors(Format.get("jpg")));
    }

    @Test
    void getPreferredProcessorsWithFullyQualifiedNames() {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.ManualSelectionStrategy.jpg",
                Java2dProcessor.class.getName());
        config.setProperty(Key.PROCESSOR_FALLBACK, PdfBoxProcessor.class.getName());

        List<Class<? extends Processor>> expected = List.of(
                Java2dProcessor.class,
                PdfBoxProcessor.class);
        assertEquals(expected, instance.getPreferredProcessors(Format.get("jpg")));
    }

    @Test
    void testToString() {
        assertEquals("ManualSelectionStrategy", instance.toString());
    }

}
