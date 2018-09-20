package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MockBrokenDerivativeCache implements DerivativeCache {

    @Override
    public Info getImageInfo(Identifier identifier) throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList) throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public OutputStream newDerivativeImageOutputStream(OperationList opList) throws IOException {
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
    public void purge(OperationList opList) throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public void purgeInvalid() throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public void put(Identifier identifier, Info imageInfo) throws IOException {
        throw new IOException("I'm broken");
    }

}
