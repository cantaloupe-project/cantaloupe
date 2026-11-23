package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class MockBrokenSourceImageFileCache extends MockCache {
    MockBrokenSourceImageFileCache(Configuration configuration) { 
        super(configuration);
    }

    @Override
    public Optional<Path> getSourceImageFile(Identifier identifier)
            throws IOException {
        throw new IOException("I'm broken");
    }

}
