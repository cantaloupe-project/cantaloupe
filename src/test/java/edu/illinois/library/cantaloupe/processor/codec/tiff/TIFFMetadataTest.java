package edu.illinois.library.cantaloupe.processor.codec.tiff;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class TIFFMetadataTest extends BaseTest {

    private ImageReader reader;

    @BeforeEach
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

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        reader.dispose();
    }

    private TIFFMetadata newInstance(ImageInputStream is) throws IOException {
        reader.setInput(is);
        final IIOMetadata metadata = reader.getImageMetadata(0);
        return new TIFFMetadata(metadata,
                metadata.getNativeMetadataFormatName());
    }

    @Test
    void testGetEXIF() throws IOException {
        final Path srcFile = TestUtil.getImage("tif-exif.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            assertTrue(newInstance(is).getEXIF().isPresent());
        }
    }

    @Test
    void testGetIPTC() throws IOException {
        final Path srcFile = TestUtil.getImage("tif-iptc.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            assertTrue(newInstance(is).getIPTC().isPresent());
        }
    }

    @Test
    void testGetNativeMetadata() throws IOException {
        final Path srcFile = TestUtil.getImage("tif-xmp.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            assertFalse(newInstance(is).getNativeMetadata().isPresent());
        }
    }

    @Test
    void testGetXMP() throws IOException {
        final Path srcFile = TestUtil.getImage("tif-xmp.tif");
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            final String rdf = newInstance(is).getXMP().orElseThrow();
            final Model model = ModelFactory.createDefaultModel();
            model.read(new StringReader(rdf), "file://" + srcFile.getParent().toAbsolutePath(), "RDF/XML");
        }
    }

}
