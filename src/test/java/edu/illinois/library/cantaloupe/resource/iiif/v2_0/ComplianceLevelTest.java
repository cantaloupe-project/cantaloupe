package edu.illinois.library.cantaloupe.resource.iiif.v2_0;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.processor.ProcessorFeature;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Quality;

import java.util.HashSet;
import java.util.Set;

public class ComplianceLevelTest extends CantaloupeTestCase {

    public void testGetLevel() {
        Set<ServiceFeature> serviceFeatures = new HashSet<>();
        Set<ProcessorFeature> processorFeatures = new HashSet<>();
        Set<Quality> qualities = new HashSet<>();
        Set<OutputFormat> outputFormats = new HashSet<>();
        assertEquals(ComplianceLevel.LEVEL_0,
                ComplianceLevel.getLevel(serviceFeatures, processorFeatures,
                        qualities, outputFormats));

        // add the set of level 1 features
        processorFeatures.add(ProcessorFeature.REGION_BY_PIXELS);
        serviceFeatures.add(ServiceFeature.SIZE_BY_WHITELISTED);
        processorFeatures.add(ProcessorFeature.SIZE_BY_WIDTH);
        processorFeatures.add(ProcessorFeature.SIZE_BY_HEIGHT);
        processorFeatures.add(ProcessorFeature.SIZE_BY_PERCENT);
        qualities.add(Quality.DEFAULT);
        outputFormats.add(OutputFormat.JPG);
        serviceFeatures.add(ServiceFeature.BASE_URI_REDIRECT);
        serviceFeatures.add(ServiceFeature.CORS);
        serviceFeatures.add(ServiceFeature.JSON_LD_MEDIA_TYPE);
        assertEquals(ComplianceLevel.LEVEL_1,
                ComplianceLevel.getLevel(serviceFeatures, processorFeatures,
                        qualities, outputFormats));

        // add the set of level 2 features
        processorFeatures.add(ProcessorFeature.REGION_BY_PERCENT);
        processorFeatures.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        processorFeatures.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        processorFeatures.add(ProcessorFeature.ROTATION_BY_90S);
        qualities.add(Quality.BITONAL);
        qualities.add(Quality.COLOR);
        qualities.add(Quality.GRAY);
        outputFormats.add(OutputFormat.PNG);
        assertEquals(ComplianceLevel.LEVEL_2,
                ComplianceLevel.getLevel(serviceFeatures, processorFeatures,
                        qualities, outputFormats));
    }

    public void testGetUri() {
        assertEquals("http://iiif.io/api/image/2/level0.json",
                ComplianceLevel.LEVEL_0.getUri());
        assertEquals("http://iiif.io/api/image/2/level1.json",
                ComplianceLevel.LEVEL_1.getUri());
        assertEquals("http://iiif.io/api/image/2/level2.json",
                ComplianceLevel.LEVEL_2.getUri());
    }

}
