package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;

public class MockFileSource extends AbstractSource implements FileSource {

    @Override
    public void checkAccess() throws IOException {
    }

    @Override
    public Iterator<Format> getFormatIterator() {
        return Collections.emptyIterator();
    }

    @Override
    public Path getPath() throws IOException {
        return null;
    }

}
