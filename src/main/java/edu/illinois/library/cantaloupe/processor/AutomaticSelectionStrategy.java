package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

import java.util.ArrayList;
import java.util.List;

/**
 * Selects a {@link Processor} based on which ones are available for use, and
 * their assumed relative efficiency at handling a given source format.
 */
class AutomaticSelectionStrategy implements SelectionStrategy {

    static final String CONFIGURATION_VALUE = "AutomaticSelectionStrategy";

    @Override
    public List<Class<? extends Processor>> getPreferredProcessors(Format sourceFormat) {
        final List<Class<? extends Processor>> candidates = new ArrayList<>();

        if (sourceFormat.isVideo()) {
            candidates.add(FfmpegProcessor.class);
        } else if (Format.PDF.equals(sourceFormat)) {
            candidates.add(PdfBoxProcessor.class);
            candidates.add(GraphicsMagickProcessor.class);
            candidates.add(ImageMagickProcessor.class);
        } else if (Format.JP2.equals(sourceFormat)) {
            candidates.add(KakaduNativeProcessor.class);
            candidates.add(KakaduDemoProcessor.class);
            candidates.add(OpenJpegProcessor.class);
            candidates.add(GraphicsMagickProcessor.class);
            candidates.add(ImageMagickProcessor.class);
        } else {
            candidates.add(Java2dProcessor.class);
            candidates.add(GraphicsMagickProcessor.class);
            candidates.add(ImageMagickProcessor.class);
        }
        return candidates;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
