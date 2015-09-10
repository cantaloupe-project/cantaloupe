package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.SourceFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Locates a source image.
 */
public interface Resolver {

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
     * @return InputStream for reading the source image; never null.
     */
    InputStream getInputStream(String identifier) throws IOException;

    /**
     * @param identifier IIIF identifier.
     * @return The expected source format of the image corresponding with the
     * given identifier.
     */
    SourceFormat getSourceFormat(String identifier);

}
