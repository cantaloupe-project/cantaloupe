package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Base class for {@link ImageMagickProcessor} and
 * {@link GraphicsMagickProcessor}.
 */
abstract class AbstractMagickProcessor extends AbstractProcessor {

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            Collections.unmodifiableSet(EnumSet.of(
                    ProcessorFeature.MIRRORING,
                    ProcessorFeature.REGION_BY_PERCENT,
                    ProcessorFeature.REGION_BY_PIXELS,
                    ProcessorFeature.REGION_SQUARE,
                    ProcessorFeature.ROTATION_ARBITRARY,
                    ProcessorFeature.ROTATION_BY_90S,
                    ProcessorFeature.SIZE_ABOVE_FULL,
                    ProcessorFeature.SIZE_BY_CONFINED_WIDTH_HEIGHT,
                    ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT,
                    ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                    ProcessorFeature.SIZE_BY_HEIGHT,
                    ProcessorFeature.SIZE_BY_PERCENT,
                    ProcessorFeature.SIZE_BY_WIDTH,
                    ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = Collections.unmodifiableSet(EnumSet.of(
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GREY,
                    edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = Collections.unmodifiableSet(EnumSet.of(
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
                    edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY));

    protected StreamFactory streamFactory;

    public void close() {}

    public StreamFactory getStreamFactory() {
        return streamFactory;
    }

    public Set<ProcessorFeature> getSupportedFeatures() {
        Set<ProcessorFeature> features;
        if (!getAvailableOutputFormats().isEmpty()) {
            features = SUPPORTED_FEATURES;
        } else {
            features = Collections.unmodifiableSet(Collections.emptySet());
        }
        return features;
    }

    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIIIF1Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                qualities;
        if (!getAvailableOutputFormats().isEmpty()) {
            qualities = SUPPORTED_IIIF_1_1_QUALITIES;
        } else {
            qualities = Collections.unmodifiableSet(Collections.emptySet());
        }
        return qualities;
    }

    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIIIF2Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                qualities;
        if (!getAvailableOutputFormats().isEmpty()) {
            qualities = SUPPORTED_IIIF_2_0_QUALITIES;
        } else {
            qualities = Collections.unmodifiableSet(Collections.emptySet());
        }
        return qualities;
    }

    public void setStreamFactory(StreamFactory streamFactory) {
        this.streamFactory = streamFactory;
    }

}
