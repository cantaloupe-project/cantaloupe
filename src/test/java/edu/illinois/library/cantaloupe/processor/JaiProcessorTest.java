package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class JaiProcessorTest extends AbstractImageIOProcessorTest {

    @Override
    protected JaiProcessor newInstance() {
        return new JaiProcessor();
    }

    @Test
    void testIsSeekingWithNonSeekableSource() throws Exception {
        try (StreamProcessor instance = newInstance()) {
            instance.setSourceFormat(Format.get("bmp"));
            instance.setStreamFactory(new PathStreamFactory(TestUtil.getImage("bmp")));
            assertFalse(instance.isSeeking());
        }
    }

    @Test
    void testIsSeekingWithSeekableSource() throws Exception {
        try (StreamProcessor instance = newInstance()) {
            instance.setSourceFormat(Format.get("tif"));
            instance.setStreamFactory(new PathStreamFactory(TestUtil.getImage("tif-rgb-1res-64x56x8-tiled-jpeg.tif")));
            assertTrue(instance.isSeeking());
        }
    }

    @Test
    @Override
    public void testProcessWithTurboJPEGAvailable() {
        // This processor doesn't use TurboJPEG ever.
    }

    @Test
    @Override
    public void testProcessWithTurboJPEGNotAvailable() {
        // This processor doesn't use TurboJPEG ever.
    }

}
