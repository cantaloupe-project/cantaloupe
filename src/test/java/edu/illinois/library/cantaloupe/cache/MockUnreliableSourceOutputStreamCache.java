package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.TestUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;

public class MockUnreliableSourceOutputStreamCache extends MockCache {

    private int callCount1 = 0;

    @Override
    public Optional<Path> getSourceImageFile(Identifier identifier)
            throws IOException {
        return Optional.of(TestUtil.getImage("jpg"));
    }

    /**
     * @throws IOException only the first time it's called.
     */
    @Override
    public OutputStream newSourceImageOutputStream(Identifier identifier)
            throws IOException {
        callCount1++;
        if (callCount1 == 1) {
            throw new IOException("I'm broken");
        }
        return OutputStream.nullOutputStream();
    }

}
