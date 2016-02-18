package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class PdfBoxProcessorTest extends ProcessorTest {

    private PdfBoxProcessor instance;

    @Before
    public void setUp() throws Exception {
        instance = new PdfBoxProcessor();
        instance.setSourceFormat(Format.PDF);
    }

    protected Processor getProcessor() {
        return instance;
    }

    @Test
    @Override
    public void testGetSize() throws Exception {
        Dimension expectedSize = new Dimension(100, 88);
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));
        assertEquals(expectedSize, instance.getSize());
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        Set<ProcessorFeature> expectedFeatures = new HashSet<>();
        expectedFeatures.add(ProcessorFeature.MIRRORING);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PIXELS);
        expectedFeatures.add(ProcessorFeature.ROTATION_ARBITRARY);
        expectedFeatures.add(ProcessorFeature.ROTATION_BY_90S);
        expectedFeatures.add(ProcessorFeature.SIZE_ABOVE_FULL);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

    @Test
    public void testProcessWithPageOption() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf-multipage.pdf"));
        final Dimension size = instance.getSize();

        // page option missing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = TestUtil.newOperationList();
        instance.process(ops, size, outputStream);
        final byte[] page1 = outputStream.toByteArray();

        // page option present
        ops.getOptions().put("page", "2");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, size, outputStream);
        final byte[] page2 = outputStream.toByteArray();

        assertFalse(Arrays.equals(page1, page2));
    }

    @Test
    public void testProcessWithIllegalPageOptionReturnsFirstPage() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf-multipage.pdf"));
        final Dimension size = instance.getSize();

        // page 1
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = TestUtil.newOperationList();
        instance.process(ops, size, outputStream);
        final byte[] page1 = outputStream.toByteArray();
        // page "35"
        ops.getOptions().put("page", "35");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, size, outputStream);
        final byte[] page35 = outputStream.toByteArray();

        assertTrue(Arrays.equals(page1, page35));

        // page "cats"
        ops.getOptions().put("page", "cats");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, size, outputStream);
        final byte[] pageCats = outputStream.toByteArray();
        assertTrue(Arrays.equals(page1, pageCats));
    }

    @Test
    public void testGetTileSizes() throws Exception {
        // untiled image
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));
        Dimension expectedSize = new Dimension(100, 88);
        List<Dimension> tileSizes = instance.getTileSizes();
        assertEquals(1, tileSizes.size());
        assertEquals(expectedSize, tileSizes.get(0));
    }

}
