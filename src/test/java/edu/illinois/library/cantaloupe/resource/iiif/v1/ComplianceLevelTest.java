package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.image.OutputFormat;

import java.util.HashSet;
import java.util.Set;

public class ComplianceLevelTest extends CantaloupeTestCase {

    public void testGetLevel() {
        Set<ProcessorFeature> processorFeatures = new HashSet<>();
        Set<Quality> qualities = new HashSet<>();
        Set<OutputFormat> outputFormats = new HashSet<>();
        assertEquals(ComplianceLevel.LEVEL_0,
                ComplianceLevel.getLevel(processorFeatures, qualities,
                        outputFormats));

        // add the set of level 1 features
        processorFeatures.add(ProcessorFeature.REGION_BY_PIXELS);
        processorFeatures.add(ProcessorFeature.SIZE_BY_WIDTH);
        processorFeatures.add(ProcessorFeature.SIZE_BY_HEIGHT);
        processorFeatures.add(ProcessorFeature.SIZE_BY_PERCENT);
        processorFeatures.add(ProcessorFeature.ROTATION_BY_90S);
        qualities.add(Quality.NATIVE);
        outputFormats.add(OutputFormat.JPG);
        assertEquals(ComplianceLevel.LEVEL_1,
                ComplianceLevel.getLevel(processorFeatures, qualities,
                        outputFormats));

        // add the set of level 2 features
        processorFeatures.add(ProcessorFeature.REGION_BY_PERCENT);
        processorFeatures.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        processorFeatures.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        qualities.add(Quality.BITONAL);
        qualities.add(Quality.COLOR);
        qualities.add(Quality.GRAY);
        outputFormats.add(OutputFormat.PNG);
        assertEquals(ComplianceLevel.LEVEL_2,
                ComplianceLevel.getLevel(processorFeatures, qualities,
                        outputFormats));
    }

    public void testGetUri() {
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level0",
                ComplianceLevel.LEVEL_0.getUri());
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level1",
                ComplianceLevel.LEVEL_1.getUri());
        assertEquals("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2",
                ComplianceLevel.LEVEL_2.getUri());
    }

}
