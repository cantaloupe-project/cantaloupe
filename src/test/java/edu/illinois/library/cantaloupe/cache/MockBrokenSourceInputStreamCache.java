package edu.illinois.library.cantaloupe.cache;

import java.io.IOException;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;

public class MockBrokenSourceInputStreamCache extends MockCache {
    MockBrokenSourceInputStreamCache(Configuration configuration) { 
        super(configuration);
    }

    @Override
    public CompletableOutputStream
    newSourceImageOutputStream(Identifier identifier) throws IOException {
        throw new IOException("I'm broken");
    }

}
