package edu.illinois.library.cantaloupe.resolver;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

public interface StreamResolver extends Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return Stream for reading the source image; never null.
     */
    ImageInputStream getInputStream(String identifier) throws IOException;

}
