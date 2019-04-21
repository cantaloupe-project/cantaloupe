package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TurboJpegProcessorTest extends AbstractProcessorTest {

    private TurboJpegProcessor instance;

    @BeforeEach
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
    void testGetInitializationErrorWithNoException() {
        assertNull(instance.getInitializationError());
    }

    @Test
    void testGetSupportedFeatures() throws Exception {
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
    @Test
    public void testProcessWithTurboJPEGAvailable() {
        // This processor always uses TurboJPEG.
    }

    @Override
    @Test
    public void testProcessWithTurboJPEGNotAvailable() {
        // This processor always uses TurboJPEG.
    }

    @Override
    @Test
    public void testProcessWritesXMPMetadataIntoPNG() {
        // This processor doesn't support this output format.
    }

    @Override
    @Test
    public void testProcessWritesXMPMetadataIntoTIFF() {
        // This processor doesn't support this output format.
    }

    @Test
    void testReadInfoEXIFAwareness() throws Exception {
        instance.setStreamFactory(new PathStreamFactory(TestUtil.getImage("jpg-exif.jpg")));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getEXIF().isPresent());
    }

    @Test
    void testReadInfoIPTCAwareness() throws Exception {
        instance.setStreamFactory(new PathStreamFactory(TestUtil.getImage("jpg-iptc.jpg")));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getIPTC().isPresent());
    }

    @Test
    void testReadInfoXMPAwareness() throws Exception {
        instance.setStreamFactory(new PathStreamFactory(TestUtil.getImage("jpg-xmp.jpg")));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getXMP().isPresent());
    }

}
