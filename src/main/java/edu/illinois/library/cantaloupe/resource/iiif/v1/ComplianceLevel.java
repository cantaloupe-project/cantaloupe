package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.iiif.Feature;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * IIIF compliance level.
 *
 * @see <a href="http://iiif.io/api/image/1.1/compliance.html">Compliance
 * Levels</a>
 */
enum ComplianceLevel {

    LEVEL_0("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level0"),
    LEVEL_1("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level1"),
    LEVEL_2("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2");

    private static final Set<Feature> LEVEL_1_FEATURES =
            new HashSet<>();
    private static final Set<Format> LEVEL_1_OUTPUT_FORMATS =
            EnumSet.noneOf(Format.class);
    private static final Set<Quality> LEVEL_1_QUALITIES =
            EnumSet.noneOf(Quality.class);
    private static final Set<Feature> LEVEL_2_FEATURES =
            new HashSet<>();
    private static final Set<Format> LEVEL_2_OUTPUT_FORMATS =
            EnumSet.noneOf(Format.class);
    private static final Set<Quality> LEVEL_2_QUALITIES =
            EnumSet.noneOf(Quality.class);

    private String uri;

    static {
        LEVEL_1_FEATURES.add(ProcessorFeature.REGION_BY_PIXELS);
        LEVEL_1_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH);
        LEVEL_1_FEATURES.add(ProcessorFeature.SIZE_BY_HEIGHT);
        LEVEL_1_FEATURES.add(ProcessorFeature.SIZE_BY_PERCENT);
        LEVEL_1_FEATURES.add(ProcessorFeature.ROTATION_BY_90S);
        LEVEL_1_QUALITIES.add(Quality.NATIVE);
        LEVEL_1_OUTPUT_FORMATS.add(Format.JPG);

        LEVEL_2_FEATURES.addAll(LEVEL_1_FEATURES);
        LEVEL_2_FEATURES.add(ProcessorFeature.REGION_BY_PERCENT);
        LEVEL_2_FEATURES.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        LEVEL_2_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        LEVEL_2_QUALITIES.addAll(LEVEL_1_QUALITIES);
        LEVEL_2_QUALITIES.add(Quality.BITONAL);
        LEVEL_2_QUALITIES.add(Quality.COLOR);
        LEVEL_2_QUALITIES.add(Quality.GRAY);
        LEVEL_2_OUTPUT_FORMATS.addAll(LEVEL_1_OUTPUT_FORMATS);
        LEVEL_2_OUTPUT_FORMATS.add(Format.PNG);
    }

    /**
     * @param processorFeatures
     * @param qualities
     * @param outputFormats
     * @return Effective IIIF compliance level corresponding to the given
     * parameters.
     */
    public static ComplianceLevel getLevel(
            Set<ProcessorFeature> processorFeatures,
            Set<Quality> qualities,
            Set<Format> outputFormats) {
        Set<Feature> allFeatures = new HashSet<>(processorFeatures);

        ComplianceLevel level = LEVEL_0;
        if (allFeatures.containsAll(LEVEL_1_FEATURES) &&
                qualities.containsAll(LEVEL_1_QUALITIES) &&
                outputFormats.containsAll(LEVEL_1_OUTPUT_FORMATS)) {
            level = LEVEL_1;
            if (allFeatures.containsAll(LEVEL_2_FEATURES) &&
                    qualities.containsAll(LEVEL_2_QUALITIES) &&
                    outputFormats.containsAll(LEVEL_2_OUTPUT_FORMATS)) {
                level = LEVEL_2;
            }
        }
        return level;
    }

    ComplianceLevel(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return this.uri;
    }

}
