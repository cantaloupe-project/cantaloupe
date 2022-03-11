package edu.illinois.library.cantaloupe.image.xmp;

import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.StringUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapReaderTest {

    @Test
    void readElements1() throws Exception {
        String xmp                  = Files.readString(TestUtil.getFixture("xmp/xmp.xmp"));
        xmp                         = StringUtils.trimXMP(xmp);
        MapReader reader               = new MapReader(xmp);
        Map<String,Object> elements = reader.readElements();

        print(elements);

        assertEquals(18, elements.size());
    }

    @Test
    void readElements2() throws Exception {
        String xmp                  = Files.readString(TestUtil.getFixture("xmp/xmp2.xmp"));
        xmp                         = StringUtils.trimXMP(xmp);
        MapReader reader               = new MapReader(xmp);
        Map<String,Object> elements = reader.readElements();
        assertEquals(61, elements.size());
    }

    private static void print(Map<String,Object> elements) {
        System.out.println("------ ELEMENTS -------");
        for (Map.Entry<String,Object> entry : elements.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
            System.out.println("-------------");
        }
    }
}