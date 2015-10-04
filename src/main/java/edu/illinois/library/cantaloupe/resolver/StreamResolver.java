package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.request.Identifier;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Interface to be implemented by all resolvers that support access to source
 * images via streams.
 */
public interface StreamResolver extends Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return Stream for reading the source image; never null.
     * @throws IOException if the image corresponding to the given identifier
     * does not exist.
     */
    ImageInputStream getInputStream(Identifier identifier) throws IOException;

}
