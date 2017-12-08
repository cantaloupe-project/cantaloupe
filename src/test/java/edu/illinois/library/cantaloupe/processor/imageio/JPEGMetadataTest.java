package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RIOT;
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

public class JPEGMetadataTest extends BaseTest {

    private JPEGMetadata getInstance(String fixtureName)
            throws IOException {
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("JPEG");
        final ImageReader reader = it.next();
        final Path srcFile = TestUtil.getImage(fixtureName);
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new JPEGMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testGetExif() throws IOException {
        assertNotNull(getInstance("jpg-exif.jpg").getXMP());
    }

    @Test
    public void testGetExifOrientation() throws IOException {
        assertNull(getInstance("jpg-iptc.jpg").getExifOrientation());
        assertEquals(Orientation.ROTATE_90,
                getInstance("jpg-rotated.jpg").getExifOrientation());
    }

    @Test
    public void testGetIptc() throws IOException {
        assertNotNull(getInstance("jpg-iptc.jpg").getXMP());
    }

    @Test
    public void testGetOrientation() throws IOException {
        assertEquals(Orientation.ROTATE_90,
                getInstance("jpg-rotated.jpg").getOrientation());
    }

    @Test
    public void testGetXmp() throws IOException {
        assertNotNull(getInstance("jpg-xmp.jpg").getXMP());
    }

    @Test
    public void testGetXmpOrientation() throws IOException {
        assertNull(getInstance("jpg-rotated.jpg").getXmpOrientation());
    }

    @Test
    public void testGetXmpRdf() throws IOException {
        RIOT.init();
        final String rdf = getInstance("jpg-xmp.jpg").getXMPRDF();
        final Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(rdf), null, "RDF/XML");
    }

}
