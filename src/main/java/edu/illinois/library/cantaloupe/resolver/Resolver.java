package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.SourceFormat;

import java.io.File;
import java.io.FileNotFoundException;
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
     * Implementations should implement this instead of getInputStream() if
     * they wish for it to be used preferentially instead of getInputStream().
     * Otherwise, they should return null.
     *
     * @param identifier IIIF identifier.
     * @return File corresponding to the given identifier, or null if an image
     * corresponding to the given identifier does not exist.
     */
    File getFile(String identifier) throws FileNotFoundException;

    /**
     * If implementations do not implement getFile(), they
     * <strong>must</strong> implement this.
     *
     * @param identifier IIIF identifier.
     * @return InputStream for reading the source image, or null if an image
     * corresponding to the given identifier does not exist.
     */
    InputStream getInputStream(String identifier) throws FileNotFoundException;

}
