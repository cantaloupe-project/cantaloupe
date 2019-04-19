package edu.illinois.library.cantaloupe.processor.codec.png;

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
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PNGMetadataTest extends BaseTest {

    private PNGMetadata getInstance(String fixtureName) throws IOException {
        final Path srcFile = TestUtil.getImage(fixtureName);
        final Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("PNG");
        final ImageReader reader = it.next();
        try (ImageInputStream is = ImageIO.createImageInputStream(srcFile.toFile())) {
            reader.setInput(is);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            return new PNGMetadata(metadata,
                    metadata.getNativeMetadataFormatName());
        } finally {
            reader.dispose();
        }
    }

    @Test
    void testGetNativeMetadata() throws IOException {
        final Map<String,String> metadata =
                getInstance("png-nativemetadata.png").getNativeMetadata().orElseThrow();
        assertEquals(1, metadata.size());
        assertEquals("Cat Highway", metadata.get("Title"));
    }

    @Test
    void testGetXMP() throws IOException {
        final String rdf = getInstance("png-xmp.png").getXMP().orElseThrow();
        final Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(rdf), null, "RDF/XML");
    }

}
