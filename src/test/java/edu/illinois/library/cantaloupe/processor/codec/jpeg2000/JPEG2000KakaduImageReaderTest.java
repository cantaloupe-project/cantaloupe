package edu.illinois.library.cantaloupe.processor.codec.jpeg2000;

import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.image.exif.Directory;
import edu.illinois.library.cantaloupe.image.iptc.DataSet;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess") // a JMH test extends this
public class JPEG2000KakaduImageReaderTest extends BaseTest {

    private static final double DELTA = 0.0000001;

    private JPEG2000KakaduImageReader instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new JPEG2000KakaduImageReader();
        instance.setSource(TestUtil.getImage("jp2"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        instance.close();
    }

    @Test
    public void testGetEXIF() throws Exception {
        instance.setSource(TestUtil.getImage("jp2-exif.jp2"));
        try (edu.illinois.library.cantaloupe.image.exif.Reader reader =
                     new edu.illinois.library.cantaloupe.image.exif.Reader()) {
            reader.setSource(instance.getEXIF());
            Directory dir = reader.read();
            assertEquals(5, dir.size());
        }
    }

    @Test
    public void testGetHeight() throws Exception {
        assertEquals(56, instance.getHeight());
    }

    @Test
    public void testGetHeightWithInvalidImage() {
        instance.setSource(TestUtil.getImage("unknown"));
        assertThrows(SourceFormatException.class, () -> instance.getHeight());
    }

    @Test
    public void testGetIPTC() throws Exception {
        instance.setSource(TestUtil.getImage("jp2-iptc.jp2"));
        try (edu.illinois.library.cantaloupe.image.iptc.Reader reader =
                     new edu.illinois.library.cantaloupe.image.iptc.Reader()) {
            reader.setSource(instance.getIPTC());
            List<DataSet> dataSets = reader.read();
            assertEquals(2, dataSets.size());
        }
    }

    @Test
    public void testGetIPTCWithInvalidImage() {
        instance.setSource(TestUtil.getImage("unknown"));
        assertThrows(SourceFormatException.class, () -> instance.getIPTC());
    }

    @Test
    public void testGetNumDecompositionLevels() throws Exception {
        assertEquals(5, instance.getNumDecompositionLevels());
    }

    @Test
    public void testGetNumDecompositionLevelsWithInvalidImage() {
        instance.setSource(TestUtil.getImage("unknown"));
        assertThrows(SourceFormatException.class,
                () -> instance.getNumDecompositionLevels());
    }

    @Test
    public void testGetTileHeight() throws Exception {
        assertEquals(56, instance.getTileHeight());
    }

    @Test
    public void testGetTileHeightWithInvalidImage() {
        instance.setSource(TestUtil.getImage("unknown"));
        assertThrows(SourceFormatException.class, () -> instance.getTileHeight());
    }

    @Test
    public void testGetTileWidth() throws Exception {
        assertEquals(64, instance.getTileWidth());
    }

    @Test
    public void testGetTileWidthWithInvalidImage() {
        instance.setSource(TestUtil.getImage("unknown"));
        assertThrows(SourceFormatException.class, () -> instance.getTileWidth());
    }

    @Test
    public void testGetWidth() throws Exception {
        assertEquals(64, instance.getWidth());
    }

    @Test
    public void testGetWidthWithInvalidImage() {
        instance.setSource(TestUtil.getImage("unknown"));
        assertThrows(SourceFormatException.class, () -> instance.getWidth());
    }

    @Test
    public void testGetWidthWithStreamSource() throws Exception {
        try (ImageInputStream is = new FileImageInputStream(
                TestUtil.getImage("jp2").toFile())) {
            instance.close();
            instance = new JPEG2000KakaduImageReader();
            instance.setSource(is);
            assertEquals(64, instance.getWidth());
        }
    }

    @Test
    public void testGetWidthWithUnreadableStreamSource() throws Exception {
        final IOException readException = new IOException("Connection reset");
        try (ImageInputStream is = new FileImageInputStream(
                TestUtil.getImage("jp2").toFile()) {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw readException;
            }
        }) {
            instance.close();
            instance = new JPEG2000KakaduImageReader();
            instance.setSource(is);
            IOException e = assertThrows(IOException.class,
                    () -> instance.getWidth());
            assertSame(readException, e);
        }
    }

    @Test
    public void testGetXMP() throws Exception {
        instance.setSource(TestUtil.getImage("jp2-xmp.jp2"));
        String xmp = instance.getXMP();
        assertTrue(xmp.startsWith("<rdf:RDF"));
        assertTrue(xmp.endsWith("</rdf:RDF>"));
    }

    @Test
    public void testGetXMPWithInvalidImage() {
        instance.setSource(TestUtil.getImage("unknown"));
        assertThrows(SourceFormatException.class, () -> instance.getXMP());
    }

    @Test
    public void testReadRegion() throws Exception {
        final Rectangle roi = new Rectangle(0, 0, 32, 28);
        final Scale scale = new ScaleByPercent(0.45);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 2);
        final ReductionFactor reductionFactor = new ReductionFactor();
        final double[] diffScales = new double[2];

        BufferedImage image = instance.readRegion(roi, scale, scaleConstraint,
                reductionFactor, diffScales);
        assertEquals(7, image.getWidth());
        assertEquals(6, image.getHeight());
        assertEquals(2, reductionFactor.factor);
        assertEquals(0.9, diffScales[0], DELTA);
        assertEquals(0.9, diffScales[1], DELTA);
    }

    /**
     * The fixture has packet length (PLT) markers and a corrupted packet
     * that is only encountered when decoding certain regions.
     */
    @Test
    public void testReadRegionWithCorruptedPacket() {
        instance.setSource(TestUtil.getImage("jp2-corrupted-packet.jp2"));

        final Rectangle roi = new Rectangle(0, 28, 32, 28);
        final Scale scale = new ScaleByPercent(1);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        final ReductionFactor reductionFactor = new ReductionFactor();
        final double[] diffScales = new double[2];

        assertThrows(IOException.class, () ->
                instance.readRegion(roi, scale, scaleConstraint,
                        reductionFactor, diffScales));
    }

    @Test
    public void testReadRegionWithCorruptedPacketOutsideOfRegion()
            throws Exception {
        instance.setSource(TestUtil.getImage("jp2-corrupted-packet.jp2"));

        final Rectangle roi = new Rectangle(32, 0, 32, 28);
        final Scale scale = new ScaleByPercent(1);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        final ReductionFactor reductionFactor = new ReductionFactor();
        final double[] diffScales = new double[2];

        BufferedImage image = instance.readRegion(roi, scale, scaleConstraint,
                reductionFactor, diffScales);
        assertEquals(32, image.getWidth());
        assertEquals(28, image.getHeight());
    }

    @Test
    public void testReadRegionWithInvalidImage() {
        instance.setSource(TestUtil.getImage("unknown"));

        final Rectangle roi = new Rectangle(0, 0, 32, 28);
        final Scale scale = new ScaleByPercent(0.45);
        final ScaleConstraint scaleConstraint = new ScaleConstraint(1, 2);
        final ReductionFactor reductionFactor = new ReductionFactor();
        final double[] diffScales = new double[2];

        assertThrows(SourceFormatException.class, () ->
                instance.readRegion(roi, scale, scaleConstraint,
                        reductionFactor, diffScales));
    }

}
