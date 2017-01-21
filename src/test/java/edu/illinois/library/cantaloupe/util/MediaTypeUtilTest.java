package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MediaTypeUtilTest {

    private static final Map<Format, File> files = new HashMap<>();
    private MediaTypeUtil instance;

    @BeforeClass
    public static void beforeClass() throws IOException {
        for (Format format : Format.values()) {
            if (format.equals(Format.UNKNOWN)) {
                continue;
            }
            String fixtureName = format.name().toLowerCase();
            files.put(format, TestUtil.getImage(fixtureName));
        }
    }

    @Before
    public void setUp() {
        instance = new MediaTypeUtil();
    }

    @Test
    public void testDetectMediaTypeWithString() {
        for (Format format : files.keySet()) {
            File file = files.get(format);
            MediaType preferredMediaType = format.getPreferredMediaType();

            try {
                boolean result = instance.detectMediaTypes(file).contains(preferredMediaType);
                if (!result) {
                    System.out.println("format: " + format +
                            "\tfile: " + file.getName() +
                            "\tresult: " + result);
                }
                //assertTrue(result);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

}
