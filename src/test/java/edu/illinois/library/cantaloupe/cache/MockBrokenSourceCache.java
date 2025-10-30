package edu.illinois.library.cantaloupe.cache;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;

public class MockBrokenSourceCache implements SourceCache {
    MockBrokenSourceCache(Configuration configuration) { 
    }
    
    @Override
    public Optional<Path> getSourceImageFile(Identifier identifier)
            throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public OutputStream newSourceImageOutputStream(Identifier identifier)
            throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public void purge() throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public void purge(Identifier identifier) throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public void purgeInvalid() throws IOException {
        throw new IOException("I'm broken");
    }

}
