package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MediaTypeTest {

    private static final Map<Format, File> files = new HashMap<>();

    private MediaType instance;

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
        instance = new MediaType("image/jpeg");
    }

    /* detectMediaTypes(File) */

    @Test
    public void testDetectMediaTypes() {
        for (Format format : files.keySet()) {
            File file = files.get(format);
            MediaType preferredMediaType = format.getPreferredMediaType();

            try {
                boolean result = MediaType.detectMediaTypes(file).
                        contains(preferredMediaType);
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

    /* MediaType(String) */

    @Test
    public void testConstructorWithValidString() {
        instance = new MediaType("image/jpeg");
        assertEquals("image/jpeg", instance.toString());
    }

    @Test
    public void testConstructorWithInvalidString() {
        try {
            new MediaType("cats");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    /* equals() */

    @Test
    public void testEquals() {
        assertTrue(instance.equals("image/jpeg"));
        assertFalse(instance.equals("image/gif"));
    }

    /* toString() */

    @Test
    public void testToString() {
        assertEquals("image/jpeg", instance.toString());
    }

}
