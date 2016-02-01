package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;

import java.util.Set;

/**
 * Abstract image processor interface. Implementations should implement at
 * least one of the sub-interfaces.
 */
public interface Processor {

    String WATERMARK_FILE_CONFIG_KEY = "watermark.image";
    String WATERMARK_INSET_CONFIG_KEY = "watermark.inset";
    String WATERMARK_POSITION_CONFIG_KEY = "watermark.position";

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
    Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIiif1_1Qualities(SourceFormat sourceFormat);

    /**
     * @param sourceFormat
     * @return All qualities supported by the processor for the given source
     * format.
     */
    Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities(SourceFormat sourceFormat);

}
