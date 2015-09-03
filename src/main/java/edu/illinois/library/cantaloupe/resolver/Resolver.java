package edu.illinois.library.cantaloupe.resolver;

/**
 * Locates a source image.
 */
public interface Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return Path, URL, or some other locator to the source image.
     */
    String resolve(String identifier);

}
