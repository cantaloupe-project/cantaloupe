package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.operation.OperationList;

import java.io.IOException;

public class MockBrokenDerivativeOutputStreamCache extends MockCache {

    @Override
    public CompletableOutputStream
    newDerivativeImageOutputStream(OperationList opList) throws IOException {
        throw new IOException("I'm broken");
    }

}
