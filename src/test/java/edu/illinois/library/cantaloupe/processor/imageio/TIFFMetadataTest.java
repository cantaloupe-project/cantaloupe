package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RIOT;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import static org.junit.Assert.*;

public class TIFFMetadataTest extends BaseTest {

    private ImageReader reader;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("TIFF");
        reader = it.next();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        reader.dispose();
    }

    private TIFFMetadata newInstance(final ImageInputStream is)
            throws IOException {
        reader.setInput(is);
        final IIOMetadata metadata = reader.getImageMetadata(0);
        return new TIFFMetadata(metadata,
                metadata.getNativeMetadataFormatName());
    }

    @Test
    @Ignore // TODO: fix this
    public void testGetExif() throws IOException {
        final File srcFile = TestUtil.getImage("tif-exif.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            assertNotNull(newInstance(is).getEXIF());
        }
    }

    @Test
    @Ignore // TODO: fix this
    public void testGetIptc() throws IOException {
        final File srcFile = TestUtil.getImage("tif-iptc.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            assertNotNull(newInstance(is).getIPTC());
        }
    }

    @Test
    public void testGetOrientation() throws IOException {
        final File srcFile = TestUtil.getImage("tif-rotated.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            assertEquals(Orientation.ROTATE_90,
                    newInstance(is).getOrientation());
        }
    }

    @Test
    @Ignore // TODO: fix this
    public void testGetXmp() throws IOException {
        final File srcFile = TestUtil.getImage("tif-xmp.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            assertNotNull(newInstance(is).getXMP());
        }
    }

    @Test
    @Ignore // TODO: fix this
    public void testGetXmpRdf() throws IOException {
        RIOT.init();

        final File srcFile = TestUtil.getImage("tif-xmp.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            final String rdf = newInstance(is).getXMPRDF();
            final Model model = ModelFactory.createDefaultModel();
            model.read(new StringReader(rdf), null, "RDF/XML");
        }
    }

}
