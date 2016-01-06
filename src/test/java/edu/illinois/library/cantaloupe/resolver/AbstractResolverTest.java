package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.ChannelProcessor;
import org.junit.Test;

import static org.junit.Assert.*;

public class AbstractResolverTest {

    @Test
    public void testIsCompatible() {
        for (Processor processor : ProcessorFactory.getAllProcessors()) {
            assertTrue(new FilesystemResolver().isCompatible(processor));
            if (!(processor instanceof ChannelProcessor)) {
                assertFalse(new HttpResolver().isCompatible(processor));
            } else {
                assertTrue(new HttpResolver().isCompatible(processor));
            }
        }
    }

}
