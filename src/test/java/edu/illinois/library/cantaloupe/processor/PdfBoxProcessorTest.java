package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PdfBoxProcessorTest extends AbstractProcessorTest {

    private PdfBoxProcessor instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_DPI, 72);
        instance = newInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        instance.close();
    }

    @Override
    protected PdfBoxProcessor newInstance() {
        PdfBoxProcessor proc = new PdfBoxProcessor();
        try {
            proc.setSourceFormat(Format.PDF);
        } catch (SourceFormatException e) {
            fail("Huge bug");
        }
        return proc;
    }

    @Test
    void testGetSupportedFeatures() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));

        Set<ProcessorFeature> expectedFeatures = Set.of(
                ProcessorFeature.MIRRORING,
                ProcessorFeature.REGION_BY_PERCENT,
                ProcessorFeature.REGION_BY_PIXELS,
                ProcessorFeature.REGION_SQUARE,
                ProcessorFeature.ROTATION_ARBITRARY,
                ProcessorFeature.ROTATION_BY_90S,
                ProcessorFeature.SIZE_ABOVE_FULL,
                ProcessorFeature.SIZE_BY_CONFINED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_HEIGHT,
                ProcessorFeature.SIZE_BY_PERCENT,
                ProcessorFeature.SIZE_BY_WIDTH,
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

    @Test
    void testProcessWithPageOption() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf-multipage.pdf"));
        final Info imageInfo = instance.readInfo();

        // page option missing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = new OperationList(new Encode(Format.JPG));
        instance.process(ops, imageInfo, outputStream);
        final byte[] page1 = outputStream.toByteArray();

        // page option present
        ops.getOptions().put("page", "2");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, imageInfo, outputStream);
        final byte[] page2 = outputStream.toByteArray();

        assertFalse(Arrays.equals(page1, page2));
    }

    @Test
    void testProcessWithIllegalPageOptionThrowsException() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf-multipage.pdf"));
        final Info imageInfo = instance.readInfo();

        // page "35"
        OperationList ops = new OperationList(new Encode(Format.JPG));
        ops.getOptions().put("page", "35");
        OutputStream outputStream = OutputStream.nullOutputStream();

        assertThrows(ProcessorException.class,
                () -> instance.process(ops, imageInfo, outputStream));
    }

    @Test
    void testReadInfoXMPAwareness() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf-xmp.pdf"));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getXMP().isPresent());
    }

    @Test
    void testReadInfoWithMultiPagePDF() throws Exception {
        instance.close();
        instance.setSourceFile(TestUtil.getImage("pdf-multipage.pdf"));
        final Info info = instance.readInfo();
        assertEquals(2, info.getImages().size());
        assertEquals(new Dimension(100, 88), info.getImages().get(0).getSize());
        assertEquals(new Dimension(88, 100), info.getImages().get(1).getSize());
    }

    @Test
    void testValidateWithNoPageArgument() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));

        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        Dimension fullSize = new Dimension(100, 88);
        instance.validate(ops, fullSize);
    }

    @Test
    void testValidateWithValidPageArgument() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));

        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        ops.getOptions().put("page", "1");
        Dimension fullSize = new Dimension(100, 88);

        instance.validate(ops, fullSize);
    }

    @Test
    void testValidateWithZeroPageArgument() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));

        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        ops.getOptions().put("page", "0");
        Dimension fullSize = new Dimension(100, 88);

        assertThrows(ValidationException.class,
                () -> instance.validate(ops, fullSize));
    }

    @Test
    void testValidateWithNegativePageArgument() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));

        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        ops.getOptions().put("page", "-1");
        Dimension fullSize = new Dimension(100, 88);

        assertThrows(ValidationException.class,
                () -> instance.validate(ops, fullSize));
    }

    @Test
    void testValidateWithExcessivePageArgument() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));

        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        ops.getOptions().put("page", "3");
        Dimension fullSize = new Dimension(100, 88);

        assertThrows(ValidationException.class,
                () -> instance.validate(ops, fullSize));
    }

}
