package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ComplianceLevelTest extends BaseTest {

    @Test
    void testGetLevel() {
        Set<ServiceFeature> serviceFeatures = new HashSet<>();
        Set<Quality> qualities              = new HashSet<>();
        Set<Format> outputFormats           = new HashSet<>();
        assertEquals(ComplianceLevel.LEVEL_0,
                ComplianceLevel.getLevel(serviceFeatures, outputFormats));

        // add the set of level 1 features
        serviceFeatures.add(ServiceFeature.SIZE_BY_WHITELISTED);
        qualities.add(Quality.DEFAULT);
        outputFormats.add(Format.get("jpg"));
        serviceFeatures.add(ServiceFeature.BASE_URI_REDIRECT);
        serviceFeatures.add(ServiceFeature.CORS);
        serviceFeatures.add(ServiceFeature.JSON_LD_MEDIA_TYPE);
        assertEquals(ComplianceLevel.LEVEL_1,
                ComplianceLevel.getLevel(serviceFeatures, outputFormats));

        // add the set of level 2 features
        qualities.add(Quality.BITONAL);
        qualities.add(Quality.COLOR);
        qualities.add(Quality.GRAY);
        outputFormats.add(Format.get("png"));
        assertEquals(ComplianceLevel.LEVEL_2,
                ComplianceLevel.getLevel(serviceFeatures, outputFormats));
    }

    @Test
    void testGetUri() {
        assertEquals("http://iiif.io/api/image/2/level0.json",
                ComplianceLevel.LEVEL_0.getUri());
        assertEquals("http://iiif.io/api/image/2/level1.json",
                ComplianceLevel.LEVEL_1.getUri());
        assertEquals("http://iiif.io/api/image/2/level2.json",
                ComplianceLevel.LEVEL_2.getUri());
    }

}
