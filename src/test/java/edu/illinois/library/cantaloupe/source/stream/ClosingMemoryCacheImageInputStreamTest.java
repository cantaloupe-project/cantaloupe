package edu.illinois.library.cantaloupe.source.stream;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ClosingMemoryCacheImageInputStreamTest extends BaseTest {

    @Test
    public void testCloseCanBeCalledMultipleTimes() throws Exception {
        byte[] arr = new byte[0];
        try (InputStream is = new ByteArrayInputStream(arr);
             ImageInputStream iis = new ClosingMemoryCacheImageInputStream(is)) {
            iis.close();
            iis.close();
        }
    }

}