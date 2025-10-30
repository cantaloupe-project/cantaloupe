package edu.illinois.library.cantaloupe.cache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;

public class MockUnreliableSourceImageFileCache extends MockCache {

    private int callCount = 0;

    MockUnreliableSourceImageFileCache(Configuration configuration) { 
        super(configuration);
    }

    /**
     * @throws IOException only the first time it's called.
     */
    @Override
    public Optional<Path> getSourceImageFile(Identifier identifier)
            throws IOException {
        callCount++;
        if (callCount == 1) {
            throw new IOException("I'm broken");
        }
        return Optional.of(TestUtil.getImage("jpg"));
    }

}
