package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Iterator;

public class AccessDeniedSource extends AbstractSource implements Source {

    @Override
    public void checkAccess() throws IOException {
        throw new AccessDeniedException("");
    }

    @Override
    public Iterator<Format> getFormatIterator() {
        return null;
    }

    @Override
    public StreamFactory newStreamFactory() {
        return null;
    }

}
