package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.image.Format;

import java.util.EnumSet;
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

    private static final Set<Format> LEVEL_1_OUTPUT_FORMATS =
            EnumSet.noneOf(Format.class);
    private static final Set<Quality> LEVEL_1_QUALITIES =
            EnumSet.noneOf(Quality.class);
    private static final Set<Format> LEVEL_2_OUTPUT_FORMATS =
            EnumSet.noneOf(Format.class);
    private static final Set<Quality> LEVEL_2_QUALITIES =
            EnumSet.noneOf(Quality.class);

    private String uri;

    static {
        LEVEL_1_QUALITIES.add(Quality.NATIVE);
        LEVEL_1_OUTPUT_FORMATS.add(Format.JPG);

        LEVEL_2_QUALITIES.addAll(LEVEL_1_QUALITIES);
        LEVEL_2_QUALITIES.add(Quality.BITONAL);
        LEVEL_2_QUALITIES.add(Quality.COLOR);
        LEVEL_2_QUALITIES.add(Quality.GREY);
        LEVEL_2_OUTPUT_FORMATS.addAll(LEVEL_1_OUTPUT_FORMATS);
        LEVEL_2_OUTPUT_FORMATS.add(Format.PNG);
    }

    /**
     * @return Effective IIIF compliance level corresponding to the given
     * parameters.
     */
    public static ComplianceLevel getLevel(Set<Quality> qualities,
                                           Set<Format> outputFormats) {
        ComplianceLevel level = LEVEL_0;
        if (qualities.containsAll(LEVEL_1_QUALITIES) &&
                outputFormats.containsAll(LEVEL_1_OUTPUT_FORMATS)) {
            level = LEVEL_1;
            if (qualities.containsAll(LEVEL_2_QUALITIES) &&
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
