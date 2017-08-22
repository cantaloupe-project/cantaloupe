package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.resolver.Resolver;

class IncompatibleResolverException extends ConfigurationException {

    IncompatibleResolverException(final Resolver resolver,
                                  final Processor processor) {
        super(String.format("%s is not compatible with %s",
                processor.getClass().getSimpleName(),
                resolver.getClass().getSimpleName()));
    }

}
