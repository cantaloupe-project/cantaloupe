package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.Iterator;

public class AccessDeniedSource extends AbstractSource implements FileSource {

    @Override
    public void checkAccess() throws IOException {
        throw new AccessDeniedException("");
    }

    @Override
    public Iterator<Format> getFormatIterator() {
        return null;
    }

    @Override
    public Path getPath() throws IOException {
        return null;
    }

}
