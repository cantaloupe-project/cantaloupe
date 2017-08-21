package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;

abstract class ImageIOProcessorTest extends ProcessorTest {

    @Test
    public void getAvailableOutputFormats() throws Exception {
        final HashMap<Format,Set<Format>> formats = new HashMap<>();
        for (Format format : ImageReader.supportedFormats()) {
            formats.put(format, ImageWriter.supportedFormats());
        }

        Processor proc = newInstance();
        proc.setSourceFormat(Format.JPG);
        Set<Format> expectedFormats = formats.get(Format.JPG);
        assertEquals(expectedFormats, proc.getAvailableOutputFormats());
    }

    /**
     * Tile-aware override.
     */
    @Test
    @Override
    public void readImageInfo() throws Exception {
        Info expectedInfo = new Info(64, 56, Format.TIF);
        expectedInfo.getImages().get(0).tileWidth = 16;
        expectedInfo.getImages().get(0).tileHeight = 16;

        final File fixture = TestUtil.
                getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif");

        // test as a StreamProcessor
        StreamProcessor sproc = (StreamProcessor) newInstance();
        StreamSource streamSource = new TestStreamSource(fixture);
        sproc.setStreamSource(streamSource);
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
            expectedInfo = new Info(640, 360, Format.MPG);
            assertEquals(expectedInfo, fproc.readImageInfo());
        } catch (UnsupportedSourceFormatException e) {
            // pass
        }
    }

    @Test
    public void readImageInfoWithOrientation() throws Exception {
        Configuration.getInstance().
                setProperty(Key.PROCESSOR_RESPECT_ORIENTATION, true);

        final File fixture = TestUtil.getImage("jpg-rotated.jpg");

        final FileProcessor fproc = (FileProcessor) newInstance();
        fproc.setSourceFile(fixture);
        fproc.setSourceFormat(Format.JPG);

        final Info info = fproc.readImageInfo();
        assertEquals(Orientation.ROTATE_90, info.getOrientation());
    }

}
