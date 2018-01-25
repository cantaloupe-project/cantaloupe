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
                "%s and %s are not compatible. Either use a different " +
                        "resolver/processor combination, or enable the " +
                        "source cache and set %s=%s.",
                resolver.getClass().getSimpleName(),
                processor.getClass().getSimpleName(),
                Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE);
        assertEquals(expected, e.getMessage());
    }

}
