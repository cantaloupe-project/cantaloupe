package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.image.Format;

import java.util.HashSet;
import java.util.Set;

/**
 * @see <a href="http://iiif.io/api/image/1.1/compliance.html">Compliance
 * Levels</a>
 */
enum ComplianceLevel {

    LEVEL_0("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level0"),
    LEVEL_1("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level1"),
    LEVEL_2("http://library.stanford.edu/iiif/image-api/1.1/compliance.html#level2");

    private static final Set<Format> LEVEL_1_OUTPUT_FORMATS = new HashSet<>();
    private static final Set<Format> LEVEL_2_OUTPUT_FORMATS = new HashSet<>();

    private String uri;

    static {
        LEVEL_1_OUTPUT_FORMATS.add(Format.get("jpg"));
        LEVEL_2_OUTPUT_FORMATS.addAll(LEVEL_1_OUTPUT_FORMATS);
        LEVEL_2_OUTPUT_FORMATS.add(Format.get("png"));
    }

    /**
     * @return Effective IIIF compliance level corresponding to the given
     * parameters.
     */
    public static ComplianceLevel getLevel(Set<Format> outputFormats) {
        ComplianceLevel level = LEVEL_0;
        if (outputFormats.containsAll(LEVEL_1_OUTPUT_FORMATS)) {
            level = LEVEL_1;
            if (outputFormats.containsAll(LEVEL_2_OUTPUT_FORMATS)) {
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
