package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RIOT;
import org.junit.Ignore;
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

public class GIFMetadataTest extends BaseTest {

    private GIFMetadata newInstance(String fixtureName)
            throws IOException {
        final Path srcFile = TestUtil.getImage(fixtureName);
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("GIF");
        final ImageReader reader = it.next();
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile)) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new GIFMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        } finally {
            reader.dispose();
        }
    }

    @Test
    @Ignore // Disabled because GIFMetadata.getXMP() and getXMPRDF() are disabled.
    public void testGetOrientation() throws IOException {
        assertEquals(Orientation.ROTATE_90,
                newInstance("gif-rotated.gif").getOrientation());
    }

    @Test
    @Ignore // Disabled because GIFMetadata.getXMP() and getXMPRDF() are disabled.
    public void testGetXmp() throws IOException {
        assertNotNull(newInstance("gif-xmp.gif").getXMP());
    }

    @Test
    @Ignore // Disabled because GIFMetadata.getXMP() and getXMPRDF() are disabled.
    public void testGetXmpRdf() throws IOException {
        RIOT.init();
        final String rdf = newInstance("gif-xmp.gif").getXMPRDF();
        System.out.println(rdf);
        final Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(rdf), null, "RDF/XML");
    }

}
