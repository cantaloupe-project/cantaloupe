package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public class MockBrokenSourceCache implements SourceCache {

    @Override
    public Path getSourceImageFile(Identifier identifier) throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public OutputStream newSourceImageOutputStream(Identifier identifier) throws IOException {
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
