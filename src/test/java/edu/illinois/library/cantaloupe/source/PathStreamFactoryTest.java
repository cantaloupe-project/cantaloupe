package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class PathStreamFactoryTest extends BaseTest {

    private PathStreamFactory instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new PathStreamFactory(TestUtil.getImage("jpg"));
    }

    @Test
    void testIsSeekingDirect() {
        assertTrue(instance.isSeekingDirect());
    }

    @Test
    void testNewInputStream() throws Exception {
        try (InputStream is = instance.newInputStream()) {
            assertEquals(1584, is.available());
        }
    }

    @Test
    void testNewSeekableStream() throws Exception {
        try (ImageInputStream is = instance.newSeekableStream()) {
            assertEquals(1584, is.length());
        }
    }

}
