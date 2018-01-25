package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resolver.Resolver;

class IncompatibleResolverException extends ConfigurationException {

    private static String getMessage(Resolver resolver, Processor processor) {
        return String.format("%s and %s are not compatible. Either use a " +
                        "different resolver/processor combination, or " +
                        "enable the source cache and set %s=%s.",
                resolver.getClass().getSimpleName(),
                processor.getClass().getSimpleName(),
                Key.STREAMPROCESSOR_RETRIEVAL_STRATEGY,
                ProcessorConnector.StreamProcessorRetrievalStrategy.CACHE);
    }

    IncompatibleResolverException(final Resolver resolver,
                                  final Processor processor) {
        super(getMessage(resolver, processor));
    }

}
