package edu.illinois.library.cantaloupe.cache;

import java.io.IOException;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.operation.OperationList;

public class MockBrokenDerivativeOutputStreamCache extends MockCache {

    MockBrokenDerivativeOutputStreamCache(Configuration configuration) { 
        super(configuration);
    }

    @Override
    public CompletableOutputStream
    newDerivativeImageOutputStream(OperationList opList) throws IOException {
        throw new IOException("I'm broken");
    }

}
