package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.SourceFormat;

import java.io.InputStream;

/**
 * Locates a source image.
 */
public interface Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return The expected format of the image corresponding to the given
     * identifier. Never null.
     */
    SourceFormat getExpectedSourceFormat(String identifier);

    /**
     * @param identifier IIIF identifier.
     * @return InputStream for reading the source image, or null if an image
     * corresponding to the given identifier does not exist.
     */
    InputStream resolve(String identifier);

}
