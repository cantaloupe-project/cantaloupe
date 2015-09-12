package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.SourceFormat;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Locates and provides stream access to a source image.
 */
public interface Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return Stream for reading the source image; never null.
     */
    ImageInputStream getInputStream(String identifier) throws IOException;

    /**
     * @param identifier IIIF identifier.
     * @return The expected source format of the image corresponding with the
     * given identifier, or <code>SourceFormat.UNKNOWN</code> if unknown.
     * Never null.
     */
    SourceFormat getSourceFormat(String identifier);

}
