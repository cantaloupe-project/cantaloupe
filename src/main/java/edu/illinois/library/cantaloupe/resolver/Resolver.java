package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.Identifier;

/**
 * Locates and provides access to a source image. This is an abstract interface;
 * implementations must implement at least one of the sub-interfaces.
 */
public interface Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return The expected source format of the image corresponding with the
     * given identifier, or <code>SourceFormat.UNKNOWN</code> if unknown.
     * Never null.
     */
    SourceFormat getSourceFormat(Identifier identifier);

}
