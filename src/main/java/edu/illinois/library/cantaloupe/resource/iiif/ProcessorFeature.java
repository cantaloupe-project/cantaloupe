package edu.illinois.library.cantaloupe.resource.iiif;

/**
 * Encapsulates a "feature" that is processor-dependent.
 */
public enum ProcessorFeature implements Feature {

    /**
     * The image may be rotated around the vertical axis, resulting in a
     * left-to-right mirroring of the content.
     */
    MIRRORING("mirroring"),

    /**
     * Regions of images may be requested by percentage.
     */
    REGION_BY_PERCENT("regionByPct"),

    /**
     * Regions of images may be requested by pixel dimensions.
     */
    REGION_BY_PIXELS("regionByPx"),

    /**
     * <p>A square region where the width and height are equal to the shorter
     * dimension of the complete image content.</p>
     *
     * <p>New in Image API 2.1.</p>
     */
    REGION_SQUARE("regionSquare"),

    /**
     * Rotation of images may be requested by degrees other than multiples of
     * 90.
     */
    ROTATION_ARBITRARY("rotationArbitrary"),

    /**
     * Rotation of images may be requested by degrees in multiples of 90.
     */
    ROTATION_BY_90S("rotationBy90s"),

    /**
     * Size of images may be requested larger than the "full" size.
     */
    SIZE_ABOVE_FULL("sizeAboveFull"),

    /**
     * <p>Size of images may be requested in the form "w,h", including sizes
     * that would distort the image.</p>
     *
     * <p>New in Image API 2.1.</p>
     * */
    SIZE_BY_DISTORTED_WIDTH_HEIGHT("sizeByDistortedWh"),

    /**
     * Deprecated in Image API 2.1.
     */
    SIZE_BY_FORCED_WIDTH_HEIGHT("sizeByForcedWh"),

    /**
     * Size of images may be requested in the form ",h".
     */
    SIZE_BY_HEIGHT("sizeByH"),

    /**
     * Size of images may be requested in the form "pct:n".
     */
    SIZE_BY_PERCENT("sizeByPct"),

    /**
     * Size of images may be requested in the form "w,".
     */
    SIZE_BY_WIDTH("sizeByW"),

    /**
     * Size of images may be requested in the form "w,h" where the supplied w
     * and h preserve the aspect ratio.
     */
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
