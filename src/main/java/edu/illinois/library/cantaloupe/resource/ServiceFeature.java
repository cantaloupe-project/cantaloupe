package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Feature;

/**
 * Encapsulates an IIIF "feature" that is application-dependent.
 *
 * @see edu.illinois.library.cantaloupe.processor.ProcessorFeature
 */
enum ServiceFeature implements Feature {

    BASE_URI_REDIRECT("baseUriRedirect"),
    CANONICAL_LINK_HEADER("canonicalLinkHeader"),
    CORS("cors"),
    JSON_LD_MEDIA_TYPE("jsonldMediaType"),
    PROFILE_LINK_HEADER("profileLinkHeader");

    private String name;

    ServiceFeature(String name) {
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
