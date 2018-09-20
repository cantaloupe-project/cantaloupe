package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;

import java.io.IOException;
import java.nio.file.Path;

public class MockFileSource extends AbstractSource implements FileSource {

    @Override
    public void checkAccess() throws IOException {
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
