package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.io.OutputStream;

public class MockBrokenFileProcessor extends MockFileProcessor
        implements FileProcessor {

    @Override
    public void process(OperationList opList,
                        Info sourceInfo,
                        OutputStream outputStream) throws ProcessorException {
        throw new ProcessorException("I'm broken");
    }

}
