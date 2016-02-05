package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Processor using the Java 2D framework.
 */
class Java2dProcessor extends AbstractProcessor
        implements StreamProcessor, FileProcessor {

    public static final String JPG_QUALITY_CONFIG_KEY =
            "Java2dProcessor.jpg.quality";
    public static final String SCALE_MODE_CONFIG_KEY =
            "Java2dProcessor.scale_mode";
    public static final String TIF_COMPRESSION_CONFIG_KEY =
            "Java2dProcessor.tif.compression";

    private static final HashMap<SourceFormat,Set<OutputFormat>> FORMATS =
            availableOutputFormats();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

    private File sourceFile;
    private StreamSource streamSource;

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE);

        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY);

        SUPPORTED_FEATURES.add(ProcessorFeature.MIRRORING);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PERCENT);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PIXELS);
        SUPPORTED_FEATURES.add(ProcessorFeature.ROTATION_ARBITRARY);
        SUPPORTED_FEATURES.add(ProcessorFeature.ROTATION_BY_90S);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_ABOVE_FULL);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_HEIGHT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_PERCENT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
    }

    /**
     * @return Map of available output formats for all known source formats.
     */
    public static HashMap<SourceFormat, Set<OutputFormat>>
    availableOutputFormats() {
        final HashMap<SourceFormat,Set<OutputFormat>> map = new HashMap<>();
        for (SourceFormat sourceFormat : ImageIoImageReader.supportedFormats()) {
            map.put(sourceFormat, ImageIoImageWriter.supportedFormats());
        }
        return map;
    }

    @Override
    public Set<OutputFormat> getAvailableOutputFormats() {
        Set<OutputFormat> formats = FORMATS.get(sourceFormat);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
    }

    @Override
    public Dimension getSize() throws ProcessorException {
        ImageIoImageReader reader = new ImageIoImageReader();
        try {
            try {
                if (streamSource != null) {
                    reader.setSource(streamSource, sourceFormat);
                } else {
                    reader.setSource(sourceFile, sourceFormat);
                }
                return reader.getSize();
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public File getSourceFile() {
        return this.sourceFile;
    }

    @Override
    public StreamSource getStreamSource() {
        return this.streamSource;
    }

    @Override
    public Set<ProcessorFeature> getSupportedFeatures() {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIiif1_1Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_2_0_QUALITIES);
        }
        return qualities;
    }

    @Override
    public void process(final OperationList ops,
                        final Dimension fullSize,
                        final OutputStream outputStream)
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats();
        if (getAvailableOutputFormats().size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            final ImageIoImageReader reader = new ImageIoImageReader();
            if (streamSource != null) {
                reader.setSource(streamSource, sourceFormat);
            } else {
                reader.setSource(sourceFile, sourceFormat);
            }
            final ReductionFactor rf = new ReductionFactor();
            final Set<ImageIoImageReader.ReaderHint> hints = new HashSet<>();
            BufferedImage image = reader.read(ops, rf, hints);

            for (Operation op : ops) {
                if (op instanceof Crop &&
                        !hints.contains(ImageIoImageReader.ReaderHint.ALREADY_CROPPED)) {
                    image = Java2dUtil.cropImage(image, (Crop) op, rf);
                } else if (op instanceof Scale) {
                    final boolean highQuality = Application.getConfiguration().
                            getString(SCALE_MODE_CONFIG_KEY, "speed").
                            equals("quality");
                    image = Java2dUtil.scaleImage(image, (Scale) op, rf,
                            highQuality);
                } else if (op instanceof Transpose) {
                    image = Java2dUtil.transposeImage(image, (Transpose) op);
                } else if (op instanceof Rotate) {
                    image = Java2dUtil.rotateImage(image, (Rotate) op);
                } else if (op instanceof Filter) {
                    image = Java2dUtil.filterImage(image, (Filter) op);
                }
            }
            new ImageIoImageWriter().write(image, ops.getOutputFormat(),
                    outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    @Override
    public void setStreamSource(StreamSource streamSource) {
        this.streamSource = streamSource;
    }

}
