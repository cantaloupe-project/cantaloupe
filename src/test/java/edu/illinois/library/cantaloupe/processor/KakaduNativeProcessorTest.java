package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.MetadataCopy;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
    public void testGetInitializationErrorWithNoException() {
        assertNull(instance.getInitializationError());
    }

    @Test
    public void testIsSeeking() {
        assertTrue(instance.isSeeking());
    }

    @Test
    public void testProcessWithMetadataCopy() throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            instance.setSourceFormat(Format.JP2);
            instance.setSourceFile(TestUtil.getImage("jp2-xmp.jp2"));

            OperationList opList = new OperationList(
                    new MetadataCopy(),
                    new Encode(Format.JPG));
            Info info = instance.readInfo();

            instance.process(opList, info, os);

            String str = new String(os.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(str.contains("<rdf:RDF"));
        }
    }

    @Test
    public void testProcessWithoutMetadataCopy() throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            instance.setSourceFormat(Format.JP2);
            instance.setSourceFile(TestUtil.getImage("jp2-xmp.jp2"));

            OperationList opList = new OperationList(new Encode(Format.JPG));
            Info info = instance.readInfo();

            instance.process(opList, info, os);

            String str = new String(os.toByteArray(), StandardCharsets.UTF_8);
            assertFalse(str.contains("<rdf:RDF"));
        }
    }

    @Test
    public void testReadImageInfoWithUntiledImage() throws Exception {
        instance.setSourceFile(TestUtil.getImage("jp2-5res-rgb-64x56x8-monotiled-lossy.jp2"));
        Info expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(64, 56)
                .withFormat(Format.JP2)
                .withNumResolutions(5)
                .build();
        assertEquals(expectedInfo, instance.readInfo());
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
        assertEquals(expectedInfo, instance.readInfo());
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

}
