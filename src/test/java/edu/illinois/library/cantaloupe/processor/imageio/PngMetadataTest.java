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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import static org.junit.Assert.*;

public class PngMetadataTest extends BaseTest {

    private PngMetadata getInstance(String fixtureName) throws IOException {
        final File srcFile = TestUtil.getImage(fixtureName);
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("PNG");
        final ImageReader reader = it.next();
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new PngMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testGetOrientation() throws IOException {
        assertEquals(Orientation.ROTATE_90,
                getInstance("png-rotated.png").getOrientation());
    }

    @Test
    public void testGetXmp() throws IOException {
        assertNotNull(getInstance("png-xmp.png").getXmp());
    }

    @Test
    public void testGetXmpRdf() throws IOException {
        RIOT.init();
        final String rdf = getInstance("png-xmp.png").getXmpRdf();
        final Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(rdf), null, "RDF/XML");
    }

}
