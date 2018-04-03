package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

public class KakaduNativeProcessorTest extends AbstractProcessorTest {

    private KakaduNativeProcessor instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        KakaduNativeProcessor.resetInitialization();

        instance = newInstance();
    }

    @Override
    protected Format getSupported16BitSourceFormat() {
        return Format.JP2;
    }

    @Override
    protected Path getSupported16BitImage() throws IOException {
        return TestUtil.getImage("jp2-6res-rgb-64x56x16-monotiled-lossy.jp2");
    }

    @Override
    protected KakaduNativeProcessor newInstance() {
        KakaduNativeProcessor proc = new KakaduNativeProcessor();
        try {
            proc.setSourceFormat(Format.JP2);
        } catch (UnsupportedSourceFormatException e) {
            fail("Huge bug");
        }
        return proc;
    }

    @Test
    public void testGetInitializationExceptionWithNoException() {
        assertNull(instance.getInitializationException());
    }

    @Ignore
    @Override
    @Test
    public void testProcessOf16BitImageWithEncodeOperationWithNoLimit() {}

    @Test
    public void testReadImageInfoWithUntiledImage() throws Exception {
        instance.setSourceFile(TestUtil.getImage("jp2-5res-rgb-64x56x8-monotiled-lossy.jp2"));
        Info expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(64, 56)
                .withFormat(Format.JP2)
                .withNumResolutions(5)
                .build();
        assertEquals(expectedInfo, instance.readImageInfo());
    }

    @Test
    public void testReadImageInfoWithTiledImage() throws Exception {
        instance.setSourceFile(TestUtil.getImage("jp2-6res-rgb-64x56x8-multitiled-lossy.jp2"));
        Info expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(32, 28)
                .withFormat(Format.JP2)
                .withNumResolutions(6)
                .build();
        assertEquals(expectedInfo, instance.readImageInfo());
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
                ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_HEIGHT,
                ProcessorFeature.SIZE_BY_PERCENT,
                ProcessorFeature.SIZE_BY_WIDTH,
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

}
