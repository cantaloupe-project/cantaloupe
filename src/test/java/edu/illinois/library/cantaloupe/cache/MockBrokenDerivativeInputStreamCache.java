package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.io.IOException;
import java.io.InputStream;

public class MockBrokenDerivativeInputStreamCache extends MockCache {
    MockBrokenDerivativeInputStreamCache(Configuration configuration) { 
        super(configuration);
    }
    
    @Override
    public InputStream newDerivativeImageInputStream(OperationList opList)
            throws IOException {
        throw new IOException("I'm broken");
    }

}
