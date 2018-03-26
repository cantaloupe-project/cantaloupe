package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Orientation;
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
        while (it.hasNext()) {
            final ImageReader reader = it.next();
            String readerName = reader.getClass().getName();
            String preferredName = new JPEGImageReader().
                    getPreferredIIOImplementations()[0];
            if (readerName.equals(preferredName)) {
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
        }
        return null;
    }

    @Test
    public void testGetExif() throws IOException {
        assertNotNull(getInstance("jpg-exif.jpg").getEXIF());
    }

    @Test
    public void testGetExifOrientationWithNoOrientation() throws IOException {
        assertEquals(Orientation.ROTATE_0,
                getInstance("jpg-iptc.jpg").getExifOrientation());
    }

    @Test
    public void testGetExifOrientationWithOrientation() throws IOException {
        assertEquals(Orientation.ROTATE_90,
                getInstance("jpg-rotated.jpg").getExifOrientation());
    }

    @Test
    public void testGetIPTC() throws IOException {
        assertNotNull(getInstance("jpg-iptc.jpg").getIPTC());
    }

    @Test
    public void testGetOrientation() throws IOException {
        assertEquals(Orientation.ROTATE_90,
                getInstance("jpg-rotated.jpg").getOrientation());
    }

    @Test
    public void testGetXMP() throws IOException {
        assertNotNull(getInstance("jpg-xmp.jpg").getXMP());
    }

    @Test
    public void testGetXmpOrientation() throws IOException {
        assertNull(getInstance("jpg-rotated.jpg").getXmpOrientation());
    }

    @Test
    public void testGetXMPRDF() throws IOException {
        RIOT.init();
        final String rdf = getInstance("jpg-xmp.jpg").getXMPRDF();
        final Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(rdf), null, "RDF/XML");
    }

}
