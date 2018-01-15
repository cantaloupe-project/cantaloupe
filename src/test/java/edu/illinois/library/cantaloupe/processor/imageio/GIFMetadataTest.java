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
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new GIFMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testGetFrameIntervalOfStaticImage() throws Exception {
        assertEquals(0, newInstance("gif").getFrameInterval());
    }

    @Test
    public void testGetFrameIntervalOfAnimatedImage() throws Exception {
        assertEquals(150, newInstance("gif-animated-looping.gif").getFrameInterval());
    }

    @Test
    public void testGetLoopCountWithStaticImage() throws Exception {
        assertEquals(1, newInstance("gif").getLoopCount());
    }

    @Test
    public void testGetLoopCountWithAnimatedLoopingImage() throws Exception {
        assertEquals(0, newInstance("gif-animated-looping.gif").getLoopCount());
    }

    @Test
    public void testGetLoopCountWithAnimatedNonLoopingImage() throws Exception {
        assertEquals(1, newInstance("gif-animated-non-looping.gif").getLoopCount());
    }

    @Test
    @Ignore // Disabled because GIFMetadata.getXMP() and getXMPRDF() are disabled.
    public void testGetOrientation() throws Exception {
        assertEquals(Orientation.ROTATE_90,
                newInstance("gif-rotated.gif").getOrientation());
    }

    @Test
    @Ignore // Disabled because GIFMetadata.getXMP() and getXMPRDF() are disabled.
    public void testGetXMP() throws Exception {
        assertNotNull(newInstance("gif-xmp.gif").getXMP());
    }

    @Test
    @Ignore // Disabled because GIFMetadata.getXMP() and getXMPRDF() are disabled.
    public void testGetXMPRDF() throws Exception {
        RIOT.init();
        final String rdf = newInstance("gif-xmp.gif").getXMPRDF();
        System.out.println(rdf);
        final Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(rdf), null, "RDF/XML");
    }

}
