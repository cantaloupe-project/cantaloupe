package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

public class PathStreamFactoryTest extends BaseTest {

    private PathStreamFactory instance;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new PathStreamFactory(TestUtil.getImage("jpg"));
    }

    @Test
    public void testIsSeekingDirect() {
        assertTrue(instance.isSeekingDirect());
    }

    @Test
    public void testNewInputStream() throws Exception {
        try (InputStream is = instance.newInputStream()) {
            assertEquals(5439, is.available());
        }
    }

    @Test
    public void testNewSeekableStream() throws Exception {
        try (ImageInputStream is = instance.newSeekableStream()) {
            assertEquals(5439, is.length());
        }
    }

}
