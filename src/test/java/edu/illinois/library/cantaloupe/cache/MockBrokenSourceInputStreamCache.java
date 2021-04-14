package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;

import java.io.IOException;

public class MockBrokenSourceInputStreamCache extends MockCache {

    @Override
    public CompletableOutputStream
    newSourceImageOutputStream(Identifier identifier) throws IOException {
        throw new IOException("I'm broken");
    }

}
