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
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

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
            proc.setSourceFormat(Format.get("pdf"));
        } catch (SourceFormatException e) {
            fail("Huge bug");
        }
        return proc;
    }

    @Test
    void testIsSeeking() {
        assertFalse(instance.isSeeking());
    }

    @Test
    void testProcessWithNonOnePageIndex() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf-multipage.pdf"));
        final Info imageInfo = instance.readInfo();

        // page index missing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = OperationList.builder()
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        instance.process(ops, imageInfo, outputStream);
        final byte[] page1 = outputStream.toByteArray();

        // page index present
        ops.setPageIndex(1);
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, imageInfo, outputStream);
        final byte[] page2 = outputStream.toByteArray();

        assertFalse(Arrays.equals(page1, page2));
    }

    @Test
    void testReadInfoNativeMetadataAwareness() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf-xmp.pdf"));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getNativeMetadata().isPresent());
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

        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Dimension fullSize = new Dimension(100, 88);
        instance.validate(ops, fullSize);
    }

    @Test
    void testValidateWithValidPageArgument() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));

        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withPageIndex(0)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Dimension fullSize = new Dimension(100, 88);

        instance.validate(ops, fullSize);
    }

    @Test
    void testValidateWithExcessivePageArgument() {
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));

        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withPageIndex(2)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Dimension fullSize = new Dimension(100, 88);

        assertThrows(ValidationException.class,
                () -> instance.validate(ops, fullSize));
    }

}
