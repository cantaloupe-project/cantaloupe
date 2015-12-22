package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ChannelProcessor;

abstract class AbstractResolver {

    public final boolean isCompatible(Processor processor) {
         return !(!(this instanceof FileResolver) &&
                !(processor instanceof ChannelProcessor));
    }

}
