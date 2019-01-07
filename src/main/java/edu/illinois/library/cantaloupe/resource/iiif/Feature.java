package edu.illinois.library.cantaloupe.resource.iiif;

/**
 * Image API "feature," which may be implemented on a per-{@link
 * edu.illinois.library.cantaloupe.resource.iiif.v2.ServiceFeature application}
 * or per-{@link ProcessorFeature processor} basis.
 */
public interface Feature {

    String getName();

}
