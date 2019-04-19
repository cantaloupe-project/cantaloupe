package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessorFactoryTest extends BaseTest {

    private ProcessorFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ProcessorFactory();
    }

    @Test
    void testGetAllProcessors() {
        assertTrue(ProcessorFactory.getAllProcessors().size() > 5);
    }

    @Test
    void testNewProcessorWithWorkingFirstPreferenceMatch() throws Exception {
        instance.setSelectionStrategy(f ->
                List.of(PdfBoxProcessor.class, Java2dProcessor.class));
        assertTrue(instance.newProcessor(Format.PDF) instanceof PdfBoxProcessor);
    }

    @Test
    void testNewProcessorWithBrokenFirstPreferenceMatchAndWorkingSecondPreferenceMatch()
            throws Exception {
        instance.setSelectionStrategy(f ->
                List.of(MockBrokenProcessor.class, Java2dProcessor.class));
        assertTrue(instance.newProcessor(Format.JPG) instanceof Java2dProcessor);
    }

    @Test
    void testNewProcessorWithWorkingSecondPreferenceMatch() throws Exception {
        instance.setSelectionStrategy(f ->
                List.of(PdfBoxProcessor.class, Java2dProcessor.class));
        assertTrue(instance.newProcessor(Format.JPG) instanceof Java2dProcessor);
    }

    @Test
    void testNewProcessorWithBrokenSecondPreferenceMatch() {
        instance.setSelectionStrategy(f ->
                List.of(PdfBoxProcessor.class, MockBrokenProcessor.class));
        assertThrows(InitializationException.class,
                () -> instance.newProcessor(Format.JPG));
    }

    @Test
    void testNewProcessorWithNoMatch() {
        instance.setSelectionStrategy(f ->
                List.of(MockPDFOnlyProcessor.class, MockPNGOnlyProcessor.class));
        assertThrows(UnsupportedSourceFormatException.class,
                () -> instance.newProcessor(Format.JPG));
    }

    @Test
    void testNewProcessorWithUnknownFormat() {
        Configuration.getInstance().setProperty(Key.PROCESSOR_FALLBACK,
                Java2dProcessor.class.getSimpleName());
        assertThrows(UnsupportedSourceFormatException.class,
                () -> instance.newProcessor(Format.UNKNOWN));
    }

}
