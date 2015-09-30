package edu.illinois.library.cantaloupe.resolver;

import java.io.File;
import java.io.IOException;

public interface FileResolver extends Resolver {

    /**
     * @param identifier IIIF identifier.
     * @return File referencing the source image; never null.
     */
    File getFile(String identifier) throws IOException;

}
