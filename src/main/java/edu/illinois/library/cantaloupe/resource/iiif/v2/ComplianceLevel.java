package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.iiif.Feature;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * IIIF compliance level.
 *
 * @see <a href="http://iiif.io/api/image/2.0/compliance.html">Compliance
 * Levels</a>
 */
enum ComplianceLevel {

    LEVEL_0("http://iiif.io/api/image/2/level0.json"),
    LEVEL_1("http://iiif.io/api/image/2/level1.json"),
    LEVEL_2("http://iiif.io/api/image/2/level2.json");

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
        LEVEL_1_FEATURES.add(ServiceFeature.SIZE_BY_WHITELISTED);
        LEVEL_1_FEATURES.add(ServiceFeature.BASE_URI_REDIRECT);
        LEVEL_1_FEATURES.add(ServiceFeature.CORS);
        LEVEL_1_FEATURES.add(ServiceFeature.JSON_LD_MEDIA_TYPE);
        LEVEL_1_QUALITIES.add(Quality.DEFAULT);
        LEVEL_1_OUTPUT_FORMATS.add(Format.JPG);

        LEVEL_2_FEATURES.addAll(LEVEL_1_FEATURES);
        LEVEL_2_QUALITIES.addAll(LEVEL_1_QUALITIES);
        LEVEL_2_QUALITIES.add(Quality.BITONAL);
        LEVEL_2_QUALITIES.add(Quality.COLOR);
        LEVEL_2_QUALITIES.add(Quality.GRAY);
        LEVEL_2_OUTPUT_FORMATS.addAll(LEVEL_1_OUTPUT_FORMATS);
        LEVEL_2_OUTPUT_FORMATS.add(Format.PNG);
    }

    /**
     * @return The effective compliance level corresponding to the given
     *         arguments.
     */
    public static ComplianceLevel getLevel(Set<ServiceFeature> serviceFeatures,
                                           Set<Quality> qualities,
                                           Set<Format> outputFormats) {
        Set<Feature> allFeatures = new HashSet<>(serviceFeatures);

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
