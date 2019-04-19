package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.image.iptc.DataSet;
import edu.illinois.library.cantaloupe.image.iptc.Tag;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    void testGetEXIF() throws IOException {
        assertTrue(getInstance("jpg-exif.jpg").getEXIF().isPresent());
    }

    @Test
    void testGetIPTC() throws IOException {
        List<DataSet> dataSets = getInstance("jpg-iptc.jpg").getIPTC().orElseThrow();
        assertEquals(2, dataSets.size());
        assertEquals(new DataSet(
                Tag.CITY, "Urbana".getBytes()),
                dataSets.get(0));
        assertEquals(new DataSet(
                Tag.APPLICATION_RECORD_VERSION, new byte[] { 0, 4 }),
                dataSets.get(1));
    }

    @Test
    void testGetXMP() throws IOException {
        final String rdf = getInstance("jpg-xmp.jpg").getXMP().orElseThrow();
        final Model model = ModelFactory.createDefaultModel();
        try (Reader reader = new StringReader(rdf)) {
            model.read(reader, null, "RDF/XML");
        }
    }

}
