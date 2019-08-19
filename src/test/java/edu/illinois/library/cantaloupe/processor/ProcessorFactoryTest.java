package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ProcessorFactoryTest extends BaseTest {

    private ProcessorFactory instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new ProcessorFactory();
    }

    @Test
    public void testGetAllProcessors() {
        assertTrue(ProcessorFactory.getAllProcessors().size() > 5);
    }

    /* newProcessor(String) */

    @Test
    public void testNewProcessorWithStringWithUnqualifiedName() throws Exception {
        assertTrue(instance.newProcessor(Java2dProcessor.class.getSimpleName()) instanceof Java2dProcessor);
    }

    @Test(expected = ClassNotFoundException.class)
    public void testNewProcessorWithStringWithNonExistingUnqualifiedName() throws Exception {
        instance.newProcessor("Bogus");
    }

    @Test
    public void testNewProcessorWithStringWithQualifiedName() throws Exception {
        assertTrue(instance.newProcessor(Java2dProcessor.class.getName()) instanceof Java2dProcessor);
    }

    @Test(expected = ClassNotFoundException.class)
    public void testNewProcessorWithStringWithNonExistingQualifiedName() throws Exception {
        instance.newProcessor(ProcessorFactory.class.getPackage().getName() + ".Bogus");
    }

    /* newProcessor(Format) */

    @Test
    public void testNewProcessorWithFormatWithWorkingFirstPreferenceMatch() throws Exception {
        instance.setSelectionStrategy(f ->
                Arrays.asList(PdfBoxProcessor.class, Java2dProcessor.class));
        assertTrue(instance.newProcessor(Format.PDF) instanceof PdfBoxProcessor);
    }

    @Test
    public void testNewProcessorWithFormatWithBrokenFirstPreferenceMatchAndWorkingSecondPreferenceMatch()
            throws Exception {
        instance.setSelectionStrategy(f ->
                Arrays.asList(MockBrokenProcessor.class, Java2dProcessor.class));
        assertTrue(instance.newProcessor(Format.JPG) instanceof Java2dProcessor);
    }

    @Test
    public void testNewProcessorWithFormatWithWorkingSecondPreferenceMatch() throws Exception {
        instance.setSelectionStrategy(f ->
                Arrays.asList(PdfBoxProcessor.class, Java2dProcessor.class));
        assertTrue(instance.newProcessor(Format.JPG) instanceof Java2dProcessor);
    }

    @Test(expected = InitializationException.class)
    public void testNewProcessorWithFormatWithBrokenSecondPreferenceMatch() throws Exception {
        instance.setSelectionStrategy(f ->
                Arrays.asList(PdfBoxProcessor.class, MockBrokenProcessor.class));
        instance.newProcessor(Format.JPG);
    }

    @Test(expected = UnsupportedSourceFormatException.class)
    public void testNewProcessorWithFormatWithNoMatch() throws Exception {
        instance.setSelectionStrategy(f -> Arrays.asList(
                MockPDFOnlyProcessor.class, MockPNGOnlyProcessor.class));
        instance.newProcessor(Format.JPG);
    }

    @Test(expected = UnsupportedSourceFormatException.class)
    public void testNewProcessorWithFormatWithUnknownFormat() throws Exception {
        Configuration.getInstance().setProperty(Key.PROCESSOR_FALLBACK,
                Java2dProcessor.class.getSimpleName());
        instance.newProcessor(Format.UNKNOWN);
    }

}
