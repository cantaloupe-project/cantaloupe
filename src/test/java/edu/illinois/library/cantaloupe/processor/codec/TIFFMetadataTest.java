package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RIOT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.Assert.*;

public class TIFFMetadataTest extends BaseTest {

    private ImageReader reader;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("TIFF");
        while (it.hasNext()) {
            ImageReader reader = it.next();
            if (reader instanceof it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader) {
                this.reader = reader;
                break;
            }
        }
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
    public void testGetExif() throws IOException {
        final Path srcFile = TestUtil.getImage("tif-exif.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            assertNotNull(newInstance(is).getEXIF());
        }
    }

    @Test
    public void testGetIptc() throws IOException {
        final Path srcFile = TestUtil.getImage("tif-iptc.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            assertNotNull(newInstance(is).getIPTC());
        }
    }

    @Test
    public void testGetOrientation() throws IOException {
        final Path srcFile = TestUtil.getImage("tif-rotated.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            assertEquals(Orientation.ROTATE_90,
                    newInstance(is).getOrientation());
        }
    }

    @Test
    public void testGetXmp() throws IOException {
        final Path srcFile = TestUtil.getImage("tif-xmp.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            assertNotNull(newInstance(is).getXMP());
        }
    }

    @Test
    public void testGetXmpRdf() throws IOException {
        RIOT.init();

        final Path srcFile = TestUtil.getImage("tif-xmp.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            final String rdf = newInstance(is).getXMPRDF();
            final Model model = ModelFactory.createDefaultModel();
            model.read(new StringReader(rdf), null, "RDF/XML");
        }
    }

}
