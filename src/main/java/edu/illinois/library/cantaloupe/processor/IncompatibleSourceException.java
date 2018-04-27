package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.source.Source;

class IncompatibleSourceException extends ConfigurationException {

    private static String getMessage(Source source, Processor processor) {
        return String.format("%s and %s are not compatible using %s = %s. " +
                        "Either use a different source/processor " +
                        "combination, or switch to %s or %s.",
                source.getClass().getSimpleName(),
                processor.getClass().getSimpleName(),
                Key.PROCESSOR_STREAM_RETRIEVAL_STRATEGY,
                RetrievalStrategy.STREAM,
                RetrievalStrategy.DOWNLOAD,
                RetrievalStrategy.CACHE);
    }

    IncompatibleSourceException(final Source source,
                                final Processor processor) {
        super(getMessage(source, processor));
    }

}
