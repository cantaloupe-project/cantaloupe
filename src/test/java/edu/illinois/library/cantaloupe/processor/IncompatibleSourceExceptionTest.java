package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.source.MockStreamSource;
import edu.illinois.library.cantaloupe.source.Source;
import org.junit.Test;

import static org.junit.Assert.*;

public class IncompatibleSourceExceptionTest {

    @Test
    public void testConstructor() {
        final Source source = new MockStreamSource();
        final Processor processor = new MockFileProcessor();
        Exception e = new IncompatibleSourceException(source, processor);

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
