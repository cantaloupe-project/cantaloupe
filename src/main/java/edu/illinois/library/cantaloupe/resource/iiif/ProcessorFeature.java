package edu.illinois.library.cantaloupe.resource.iiif;

/**
 * Encapsulates a "feature" that is processor-dependent.
 */
public enum ProcessorFeature implements Feature {

    MIRRORING("mirroring"),
    REGION_BY_PERCENT("regionByPct"),
    REGION_BY_PIXELS("regionByPx"),
    REGION_SQUARE("regionSquare"),
    ROTATION_ARBITRARY("rotationArbitrary"),
    ROTATION_BY_90S("rotationBy90s"),
    SIZE_ABOVE_FULL("sizeAboveFull"),
    SIZE_BY_FORCED_WIDTH_HEIGHT("sizeByForcedWh"),
    SIZE_BY_HEIGHT("sizeByH"),
    SIZE_BY_PERCENT("sizeByPct"),
    SIZE_BY_WIDTH("sizeByW"),
    SIZE_BY_WIDTH_HEIGHT("sizeByWh");

    private String name;

    ProcessorFeature(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * @return The name.
     */
    public String toString() {
        return this.getName();
    }

}
