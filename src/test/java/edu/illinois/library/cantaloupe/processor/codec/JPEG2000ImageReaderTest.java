package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class JPEG2000ImageReaderTest extends AbstractImageReaderTest {

    @Override
    JPEG2000ImageReader newInstance() throws IOException {
        JPEG2000ImageReader reader = new JPEG2000ImageReader();
        reader.setSource(TestUtil.getImage("jp2"));
        return reader;
    }

    @Test
    @Override
    public void testGetCompression() throws IOException {
        assertEquals(Compression.JPEG2000, instance.getCompression(0));
    }

    @Test
    @Override
    public void testGetNumResolutions() throws Exception {
        assertEquals(5, instance.getNumResolutions());
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testRead() throws Exception {
        instance.read();
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testReadWithArguments() throws Exception {
        OperationList ops = new OperationList(new Identifier("cats"), Format.JPG);
        Orientation orientation = Orientation.ROTATE_0;
        ReductionFactor rf = new ReductionFactor();
        Set<ReaderHint> hints = new HashSet<>();

        instance.read(ops, orientation, rf, hints);
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testReadRendered() throws Exception {
        instance.read();
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void testReadRenderedWithArguments() throws Exception {
        OperationList ops = new OperationList(new Identifier("cats"), Format.JPG);
        Orientation orientation = Orientation.ROTATE_0;
        ReductionFactor rf = new ReductionFactor();
        Set<ReaderHint> hints = new HashSet<>();

        instance.readRendered(ops, orientation, rf, hints);
    }

}
