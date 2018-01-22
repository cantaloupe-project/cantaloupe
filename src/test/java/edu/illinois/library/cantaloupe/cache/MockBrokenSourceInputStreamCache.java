package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;

import java.io.IOException;
import java.io.OutputStream;

public class MockBrokenSourceInputStreamCache extends MockCache {

    @Override
    public OutputStream newSourceImageOutputStream(Identifier identifier)
            throws IOException {
        throw new IOException("I'm broken");
    }

}
