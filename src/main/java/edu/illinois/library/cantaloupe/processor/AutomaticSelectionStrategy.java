package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Selects a {@link Processor} based on which ones are available for use, and
 * their assumed relative efficiency at handling a given source format.
 */
class AutomaticSelectionStrategy implements SelectionStrategy {

    private static final List<Class<? extends Processor>> JP2_CANDIDATES,
            JPG_CANDIDATES, PDF_CANDIDATES, VIDEO_CANDIDATES,
            FALLBACK_CANDIDATES;

    static {
        // JP2
        List<Class<? extends Processor>> list = new ArrayList<>();
        list.add(KakaduNativeProcessor.class);
        list.add(OpenJpegProcessor.class);
        list.add(ImageMagickProcessor.class);
        list.add(GraphicsMagickProcessor.class);
        JP2_CANDIDATES = Collections.unmodifiableList(list);

        // JPEG
        list = new ArrayList<>();
        list.add(TurboJpegProcessor.class);
        list.add(Java2dProcessor.class);
        list.add(GraphicsMagickProcessor.class);
        list.add(ImageMagickProcessor.class);
        JPG_CANDIDATES = Collections.unmodifiableList(list);

        // PDF
        list = new ArrayList<>();
        list.add(PdfBoxProcessor.class);
        list.add(GraphicsMagickProcessor.class);
        list.add(ImageMagickProcessor.class);
        PDF_CANDIDATES = Collections.unmodifiableList(list);

        // Video
        list = new ArrayList<>();
        list.add(FfmpegProcessor.class);
        VIDEO_CANDIDATES = Collections.unmodifiableList(list);

        // Fallback
        list = new ArrayList<>();
        list.add(Java2dProcessor.class);
        list.add(GraphicsMagickProcessor.class);
        list.add(ImageMagickProcessor.class);
        FALLBACK_CANDIDATES = Collections.unmodifiableList(list);
    }

    @Override
    public List<Class<? extends Processor>> getPreferredProcessors(Format sourceFormat) {
        if (Format.JP2.equals(sourceFormat)) {
            return JP2_CANDIDATES;
        } if (Format.JPG.equals(sourceFormat)) {
            return JPG_CANDIDATES;
        } else if (Format.PDF.equals(sourceFormat)) {
            return PDF_CANDIDATES;
        } else if (sourceFormat.isVideo()) {
            return VIDEO_CANDIDATES;
        }
        return FALLBACK_CANDIDATES;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
