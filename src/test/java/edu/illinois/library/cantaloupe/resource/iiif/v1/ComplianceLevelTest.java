package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ComplianceLevelTest extends BaseTest {

    @Test
    void testGetLevel() {
        Set<Quality> qualities    = EnumSet.noneOf(Quality.class);
        Set<Format> outputFormats = EnumSet.noneOf(Format.class);
        assertEquals(ComplianceLevel.LEVEL_0,
                ComplianceLevel.getLevel(qualities, outputFormats));

        // add the set of level 1 features
        qualities.add(Quality.NATIVE);
        outputFormats.add(Format.JPG);
        assertEquals(ComplianceLevel.LEVEL_1,
                ComplianceLevel.getLevel(qualities, outputFormats));

        // add the set of level 2 features
        qualities.add(Quality.BITONAL);
        qualities.add(Quality.COLOR);
        qualities.add(Quality.GREY);
        outputFormats.add(Format.PNG);
        assertEquals(ComplianceLevel.LEVEL_2,
                ComplianceLevel.getLevel(qualities, outputFormats));
    }

    @Test
    void testGetUri() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level0",
                ComplianceLevel.LEVEL_0.getUri());
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level1",
                ComplianceLevel.LEVEL_1.getUri());
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2",
                ComplianceLevel.LEVEL_2.getUri());
    }

}
