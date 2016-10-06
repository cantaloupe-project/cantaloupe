package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static edu.illinois.library.cantaloupe.processor.PdfBoxProcessor.DOWNSCALE_FILTER_CONFIG_KEY;
import static edu.illinois.library.cantaloupe.processor.PdfBoxProcessor.UPSCALE_FILTER_CONFIG_KEY;
import static org.junit.Assert.*;

public class PdfBoxProcessorTest extends ProcessorTest {

    private PdfBoxProcessor instance;

    @Before
    public void setUp() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        config.setProperty(PdfBoxProcessor.DPI_CONFIG_KEY, 72);

        instance = newInstance();
    }

    protected PdfBoxProcessor newInstance() {
        PdfBoxProcessor proc = new PdfBoxProcessor();
        try {
            proc.setSourceFormat(Format.PDF);
        } catch (UnsupportedSourceFormatException e) {
            fail("Huge bug");
        }
        return proc;
    }

    @Test
    public void testGetDownscaleFilter() {
        assertNull(instance.getDownscaleFilter());

        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "bell");
        assertEquals(Scale.Filter.BELL, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "bicubic");
        assertEquals(Scale.Filter.BICUBIC, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "bspline");
        assertEquals(Scale.Filter.BSPLINE, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "box");
        assertEquals(Scale.Filter.BOX, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "hermite");
        assertEquals(Scale.Filter.HERMITE, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "lanczos3");
        assertEquals(Scale.Filter.LANCZOS3, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "mitchell");
        assertEquals(Scale.Filter.MITCHELL, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "triangle");
        assertEquals(Scale.Filter.TRIANGLE, instance.getDownscaleFilter());
    }

    @Test
    public void testGetUpscaleFilter() {
        assertNull(instance.getUpscaleFilter());

        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "bell");
        assertEquals(Scale.Filter.BELL, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "bicubic");
        assertEquals(Scale.Filter.BICUBIC, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "bspline");
        assertEquals(Scale.Filter.BSPLINE, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "box");
        assertEquals(Scale.Filter.BOX, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "hermite");
        assertEquals(Scale.Filter.HERMITE, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "lanczos3");
        assertEquals(Scale.Filter.LANCZOS3, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "mitchell");
        assertEquals(Scale.Filter.MITCHELL, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "triangle");
        assertEquals(Scale.Filter.TRIANGLE, instance.getUpscaleFilter());
    }

    @Test
    @Override
    public void testGetImageInfo() throws Exception {
        ImageInfo expectedInfo = new ImageInfo(100, 88, 100, 88, Format.PDF);
        instance.setSourceFile(TestUtil.getImage("pdf.pdf"));
        instance.setSourceFormat(Format.PDF);
        assertEquals(expectedInfo, instance.getImageInfo());
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        Set<ProcessorFeature> expectedFeatures = new HashSet<>();
        expectedFeatures.add(ProcessorFeature.MIRRORING);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PIXELS);
        expectedFeatures.add(ProcessorFeature.REGION_SQUARE);
        expectedFeatures.add(ProcessorFeature.ROTATION_ARBITRARY);
        expectedFeatures.add(ProcessorFeature.ROTATION_BY_90S);
        expectedFeatures.add(ProcessorFeature.SIZE_ABOVE_FULL);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT);
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
        final ImageInfo imageInfo = instance.getImageInfo();

        // page option missing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = TestUtil.newOperationList();
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
    public void testProcessWithIllegalPageOptionReturnsFirstPage() throws Exception {
        instance.setSourceFile(TestUtil.getImage("pdf-multipage.pdf"));
        final ImageInfo imageInfo = instance.getImageInfo();

        // page 1
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = TestUtil.newOperationList();
        instance.process(ops, imageInfo, outputStream);
        final byte[] page1 = outputStream.toByteArray();
        // page "35"
        ops.getOptions().put("page", "35");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, imageInfo, outputStream);
        final byte[] page35 = outputStream.toByteArray();

        assertTrue(Arrays.equals(page1, page35));

        // page "cats"
        ops.getOptions().put("page", "cats");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, imageInfo, outputStream);
        final byte[] pageCats = outputStream.toByteArray();
        assertTrue(Arrays.equals(page1, pageCats));
    }

}
