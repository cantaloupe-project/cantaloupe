package edu.illinois.library.cantaloupe.resource.iiif.v3;

/**
 * Features defined for use in the {@code extraFeatures} property of an
 * information response.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#57-extra-functionality">Extra
 *     Functionality</a>
 */
enum Feature {

    /**
     * The base URI of the service will redirect to the image information
     * document.
     */
    BASE_URI_REDIRECT("baseUriRedirect"),

    /**
     * The canonical image URI HTTP link header is provided on image responses.
     */
    CANONICAL_LINK_HEADER("canonicalLinkHeader"),

    /**
     * The CORS HTTP headers are provided on all responses.
     */
    CORS("cors"),

    /**
     * The JSON-LD media type is provided when JSON-LD is requested.
     */
    JSON_LD_MEDIA_TYPE("jsonldMediaType"),

    /**
     * The image may be rotated around the vertical axis, resulting in a
     * left-to-right mirroring of the content.
     */
    MIRRORING("mirroring"),

    /**
     * The profile HTTP link header is provided on image responses.
     */
    PROFILE_LINK_HEADER("profileLinkHeader"),

    /**
     * Regions of the full image may be requested by percentage.
     */
    REGION_BY_PERCENT("regionByPct"),

    /**
     * Regions of the full image may be requested by pixel dimensions.
     */
    REGION_BY_PIXELS("regionByPx"),

    /**
     * <p>A square region may be requested, where the width and height are
     * equal to the shorter dimension of the full image.</p>
     */
    REGION_SQUARE("regionSquare"),

    /**
     * Image rotation may be requested using values other than multiples of 90
     * degrees.
     */
    ROTATION_ARBITRARY("rotationArbitrary"),

    /**
     * Image rotation may be requested in multiples of 90 degrees.
     */
    ROTATION_BY_90S("rotationBy90s"),

    /**
     * Image size may be requested in the form {@code !w,h}.
     */
    SIZE_BY_CONFINED_WIDTH_HEIGHT("sizeByConfinedWh"),

    /**
     * Image size may be requested in the form {@code ,h}.
     */
    SIZE_BY_HEIGHT("sizeByH"),

    /**
     * Image size may be requested in the form {@code pct:n}.
     */
    SIZE_BY_PERCENT("sizeByPct"),

    /**
     * Image size may be requested in the form {@code w,}.
     */
    SIZE_BY_WIDTH("sizeByW"),

    /**
     * Image size may be requested in the form {@code w,h}.
     */
    SIZE_BY_WIDTH_HEIGHT("sizeByWh"),

    /**
     * Image sizes prefixed with {@code ^} may be requested.
     */
    SIZE_UPSCALING("sizeUpscaling");

    private final String name;

    Feature(String name) {
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
