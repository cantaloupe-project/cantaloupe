package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;

import java.io.IOException;
import java.nio.file.Path;

public class MockBrokenSourceImageFileCache extends MockCache {

    @Override
    public Path getSourceImageFile(Identifier identifier) throws IOException {
        throw new IOException("I'm broken");
    }

}
