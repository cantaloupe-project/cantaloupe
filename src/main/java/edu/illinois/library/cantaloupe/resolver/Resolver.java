package edu.illinois.library.cantaloupe.resolver;

import java.io.InputStream;

/**
 * Locates a source image.
 */
public interface Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return InputStream for reading the source image, or null if an image
     * corresponding to the given identifier does not exist.
     */
    InputStream resolve(String identifier);

}
