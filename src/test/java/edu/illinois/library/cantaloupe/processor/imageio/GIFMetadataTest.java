package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class GIFMetadataTest extends BaseTest {

    private GIFMetadata newInstance(String fixtureName)
            throws IOException {
        final File srcFile = TestUtil.getImage(fixtureName);
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

    /* These tests are disabled because GIFMetadata.getXmp() and getXmpRdf()
    are disabled.
    @Test
    public void testGetOrientation() throws IOException {
        assertEquals(Metadata.Orientation.ROTATE_90,
                newInstance("gif-rotated.gif").getOrientation());
    }

    @Test
    public void testGetXmp() throws IOException {
        assertNotNull(newInstance("gif-xmp.gif").getXmp());
    }

    @Test
    public void testGetXmpRdf() throws IOException {
        RIOT.init();
        final String rdf = newInstance("gif-xmp.gif").getXmpRdf();
        System.out.println(rdf);
        final Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(rdf), null, "RDF/XML");
    }
    */
}
