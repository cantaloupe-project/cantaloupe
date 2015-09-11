package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Feature;

/**
 * Encapsulates an IIIF "feature" that is processor-dependent.
 *
 * @see edu.illinois.library.cantaloupe.resource.ServiceFeature
 */
public enum ProcessorFeature implements Feature {

    MIRRORING("mirroring"),
    REGION_BY_PERCENT("regionByPct"),
    REGION_BY_PIXELS("regionByPx"),
    ROTATION_ARBITRARY("rotationArbitrary"),
    ROTATION_BY_90S("rotationBy90s"),
    SIZE_ABOVE_FULL("sizeAboveFull"),
    SIZE_BY_WHITELISTED("sizeByWhListed"), // TODO: this is not really a processor feature
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
