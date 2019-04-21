package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.CropByPixels;
import edu.illinois.library.cantaloupe.operation.CropToSquare;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class KakaduDemoProcessorTest extends AbstractProcessorTest {

    private KakaduDemoProcessor instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration.getInstance().clearProperty(
                Key.KAKADUDEMOPROCESSOR_PATH_TO_BINARIES);
        KakaduDemoProcessor.resetInitialization();

        instance = newInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        instance.close();
    }

    @Override
    protected KakaduDemoProcessor newInstance() {
        KakaduDemoProcessor proc = new KakaduDemoProcessor();
        try {
            proc.setSourceFormat(Format.JP2);
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
    void testGetInitializationErrorWithMissingBinaries() {
        Configuration.getInstance().setProperty(
                Key.KAKADUDEMOPROCESSOR_PATH_TO_BINARIES,
                "/bogus/bogus/bogus");
        KakaduDemoProcessor.resetInitialization();
        assertNotNull(instance.getInitializationError());
    }

    /**
     * The behavior of kdu_expand's -region argument changed sometime between
     * v7.6 and v7.10.4, and it's no longer possible to make it pixel-perfect,
     * AFAICT. This is terrible but better than @Disabled.
     */
    @Override
    @Test
    public void testProcessWithSquareCropOperation() throws Exception {
        CropToSquare crop = new CropToSquare();
        OperationList ops = new OperationList(crop, new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    double expectedSize = (this.sourceSize.width() > this.sourceSize.height()) ?
                            this.sourceSize.height() : this.sourceSize.width();
                    assertTrue(Math.abs(expectedSize - this.resultingImage.getWidth()) < 2);
                    assertTrue(Math.abs(expectedSize - this.resultingImage.getHeight()) < 2);
                }
            }
        });
    }

    /**
     * The behavior of kdu_expand's -region argument changed sometime between
     * v7.6 and v7.10.4, and it's no longer possible to make it pixel-perfect,
     * AFAICT. This is terrible but better than @Disabled.
     */
    @Override
    @Test
    public void testProcessWithCropByPixelsOperation() throws Exception {
        OperationList ops = new OperationList(
                new CropByPixels(10, 10, 35, 30),
                new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                assertTrue(Math.abs(this.resultingImage.getWidth() - 35) < 3);
                assertTrue(Math.abs(this.resultingImage.getHeight() - 30) < 3);
            }
        });
    }

    @Test
    @Override
    public void testProcessWithTurboJPEGAvailable() {
        // This processor doesn't use TurboJPEG ever.
    }

    @Test
    @Override
    public void testProcessWithTurboJPEGNotAvailable() {
        // This processor doesn't use TurboJPEG ever.
    }

    @Test
    void testReadInfoIPTCAwareness() throws Exception {
        instance.setSourceFile(TestUtil.getImage("jp2-iptc.jp2"));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getIPTC().isPresent());
    }

    @Test
    void testReadInfoXMPAwareness() throws Exception {
        instance.setSourceFile(TestUtil.getImage("jp2-xmp.jp2"));
        Info info = instance.readInfo();
        assertTrue(info.getMetadata().getXMP().isPresent());
    }

    @Test
    void testReadInfoTileAwareness() throws Exception {
        // untiled image
        instance.setSourceFile(TestUtil.getImage("jp2-5res-rgb-64x56x8-monotiled-lossy.jp2"));
        Info expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(64, 56)
                .withFormat(Format.JP2)
                .withNumResolutions(5)
                .build();
        assertEquals(expectedInfo, instance.readInfo());

        // tiled image
        instance.setSourceFile(TestUtil.getImage("jp2-6res-rgb-64x56x8-multitiled-lossy.jp2"));
        expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(32, 28)
                .withFormat(Format.JP2)
                .withNumResolutions(6)
                .build();
        assertEquals(expectedInfo, instance.readInfo());
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

}
