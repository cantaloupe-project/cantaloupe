package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.ConfigurationException;
import edu.illinois.library.cantaloupe.resolver.Resolver;

public class IncompatibleResolverException extends ConfigurationException {

    public IncompatibleResolverException(final Resolver resolver,
                                         final Processor processor) {
        super(String.format("%s is not compatible with %s",
                processor.getClass().getSimpleName(),
                resolver.getClass().getSimpleName()));
    }

}
