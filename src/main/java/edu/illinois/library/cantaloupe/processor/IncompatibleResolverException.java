package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resolver.Resolver;

class IncompatibleResolverException extends ConfigurationException {

    private static String getMessage(Resolver resolver, Processor processor) {
        return String.format("%s and %s are not compatible using %s = %s. " +
                        "Either use a different resolver/processor " +
                        "combination, or switch to %s or %s.",
                resolver.getClass().getSimpleName(),
                processor.getClass().getSimpleName(),
                Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                RetrievalStrategy.STREAM,
                RetrievalStrategy.DOWNLOAD,
                RetrievalStrategy.CACHE);
    }

    IncompatibleResolverException(final Resolver resolver,
                                  final Processor processor) {
        super(getMessage(resolver, processor));
    }

}
