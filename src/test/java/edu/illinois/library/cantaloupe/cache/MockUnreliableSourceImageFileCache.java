package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class MockUnreliableSourceImageFileCache extends MockCache {

    private int callCount = 0;

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
