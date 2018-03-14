package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resolver.MockStreamResolver;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import org.junit.Test;

import static org.junit.Assert.*;

public class IncompatibleResolverExceptionTest {

    @Test
    public void testConstructor() {
        final Resolver resolver = new MockStreamResolver();
        final Processor processor = new MockFileProcessor();
        Exception e = new IncompatibleResolverException(resolver, processor);

        String expected = String.format(
                "%s and %s are not compatible using %s = %s. Either use a " +
                        "different resolver/processor combination, or switch " +
                        "to %s or %s.",
                resolver.getClass().getSimpleName(),
                processor.getClass().getSimpleName(),
                Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                RetrievalStrategy.STREAM,
                RetrievalStrategy.DOWNLOAD,
                RetrievalStrategy.CACHE);
        assertEquals(expected, e.getMessage());
    }

}
