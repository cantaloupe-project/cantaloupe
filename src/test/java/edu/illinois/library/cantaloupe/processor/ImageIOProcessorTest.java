package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;

abstract class ImageIOProcessorTest extends AbstractProcessorTest {

    @Test
    public void testGetAvailableOutputFormats() throws Exception {
        final HashMap<Format,Set<Format>> formats = new HashMap<>();
        for (Format format : ImageReaderFactory.supportedFormats()) {
            formats.put(format, ImageWriterFactory.supportedFormats());
        }

        Processor proc = newInstance();
        proc.setSourceFormat(Format.JPG);
        Set<Format> expectedFormats = formats.get(Format.JPG);
        assertEquals(expectedFormats, proc.getAvailableOutputFormats());
    }

    @Test
    public void testReadImageInfoTileAwareness() throws Exception {
        Info expectedInfo = Info.builder()
                .withSize(64, 56)
                .withTileSize(16, 16)
                .withNumResolutions(1)
                .withFormat(Format.TIF)
                .build();
        final Path fixture = TestUtil.
                getImage("tif-rgb-1res-64x56x8-tiled-uncompressed.tif");

        // test as a StreamProcessor
        StreamProcessor sproc = (StreamProcessor) newInstance();
        StreamFactory streamFactory = new PathStreamFactory(fixture);
        sproc.setStreamFactory(streamFactory);
        sproc.setSourceFormat(Format.TIF);
        assertEquals(expectedInfo, sproc.readImageInfo());

        // test as a FileProcessor
        FileProcessor fproc = (FileProcessor) newInstance();
        fproc.setSourceFile(fixture);
        fproc.setSourceFormat(Format.TIF);
        assertEquals(expectedInfo, fproc.readImageInfo());

        try {
            fproc.setSourceFile(TestUtil.getImage("mpg"));
            fproc.setSourceFormat(Format.MPG);
            expectedInfo = Info.builder()
                    .withSize(640, 360)
                    .withFormat(Format.MPG)
                    .build();
            assertEquals(expectedInfo, fproc.readImageInfo());
        } catch (UnsupportedSourceFormatException e) {
            // pass
        }
    }

    @Test
    public void testReadImageInfoWithOrientation() throws Exception {
        Configuration.getInstance().
                setProperty(Key.PROCESSOR_RESPECT_ORIENTATION, true);

        final Path fixture = TestUtil.getImage("jpg-rotated.jpg");

        final FileProcessor fproc = (FileProcessor) newInstance();
        fproc.setSourceFile(fixture);
        fproc.setSourceFormat(Format.JPG);

        final Info info = fproc.readImageInfo();
        assertEquals(Orientation.ROTATE_90, info.getOrientation());
    }

}
