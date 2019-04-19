package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.source.MockStreamSource;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IncompatibleSourceExceptionTest extends BaseTest {

    @Test
    void testConstructor() {
        Source source       = new MockStreamSource();
        Processor processor = new MockFileProcessor();
        Exception e         = new IncompatibleSourceException(source, processor);

        String expected = String.format(
                "%s and %s are not compatible using %s = %s. Either use a " +
                        "different source/processor combination, or switch " +
                        "to %s or %s.",
                source.getClass().getSimpleName(),
                processor.getClass().getSimpleName(),
                Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                RetrievalStrategy.STREAM,
                RetrievalStrategy.DOWNLOAD,
                RetrievalStrategy.CACHE);
        assertEquals(expected, e.getMessage());
    }

}
