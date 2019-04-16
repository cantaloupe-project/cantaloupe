package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

public class TurboJpegProcessorTest extends AbstractProcessorTest {

    private TurboJpegProcessor instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        TurboJpegProcessor.resetInitialization();

        instance = newInstance();
    }

    @Override
    protected TurboJpegProcessor newInstance() {
        TurboJpegProcessor proc = new TurboJpegProcessor();
        try {
            proc.setSourceFormat(Format.JPG);
        } catch (UnsupportedSourceFormatException e) {
            fail("Huge bug");
        }
        return proc;
    }

    @Test
    public void testGetInitializationErrorWithNoException() {
        assertNull(instance.getInitializationError());
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));

        Set<ProcessorFeature> expectedFeatures = EnumSet.of(
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

    @Override
    @Ignore // This processor doesn't support this output format.
    @Test
    public void testProcessWritesXMPMetadataIntoPNG() throws Exception {
        super.testProcessWritesXMPMetadataIntoPNG();
    }

    @Override
    @Ignore // This processor doesn't support this output format.
    @Test
    public void testProcessWritesXMPMetadataIntoTIFF() throws Exception {
        super.testProcessWritesXMPMetadataIntoTIFF();
    }

    @Test
    public void testReadInfoEXIFAwareness() throws Exception {
        instance.setStreamFactory(new PathStreamFactory(TestUtil.getImage("jpg-exif.jpg")));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getEXIF().isPresent());
    }

    @Test
    public void testReadInfoIPTCAwareness() throws Exception {
        instance.setStreamFactory(new PathStreamFactory(TestUtil.getImage("jpg-iptc.jpg")));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getIPTC().isPresent());
    }

    @Test
    public void testReadInfoXMPAwareness() throws Exception {
        instance.setStreamFactory(new PathStreamFactory(TestUtil.getImage("jpg-xmp.jpg")));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getXMP().isPresent());
    }

}
