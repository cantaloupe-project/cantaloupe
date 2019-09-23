package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;

public class AccessDeniedSource extends AbstractSource implements FileSource {

    @Override
    public void checkAccess() throws IOException {
        throw new AccessDeniedException("");
    }

    @Override
    public Format getFormat() throws IOException {
        return null;
    }

    @Override
    public Path getPath() throws IOException {
        return null;
    }

}
