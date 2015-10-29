package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Quality;

import java.util.Set;

/**
 * Abstract image processor interface. Implementations should implement at
 * least one of the sub-interfaces.
 */
public interface Processor {

    /**
     * @param sourceFormat The source format for which to get a list of
     *                     available output formats.
     * @return Output formats available for the given source format, or an
     * empty set if none.
     */
    Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat);

    /**
     * @param sourceFormat
     * @return All features supported by the processor for the given source
     * format.
     */
    Set<ProcessorFeature> getSupportedFeatures(SourceFormat sourceFormat);

    /**
     * @param sourceFormat
     * @return All qualities supported by the processor for the given source
     * format.
     */
    Set<Quality> getSupportedQualities(SourceFormat sourceFormat);

}
