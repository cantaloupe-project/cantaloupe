package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;

import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Processor using the Java Advanced Imaging (JAI) framework.
 */
class JaiProcessor implements FileProcessor, StreamProcessor {

    public static final String JPG_QUALITY_CONFIG_KEY =
            "JaiProcessor.jpg.quality";
    public static final String TIF_COMPRESSION_CONFIG_KEY =
            "JaiProcessor.tif.compression";

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

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
     * @return Map of available output formats for all known source formats,
     * based on information reported by the ImageIO library.
     */
    public static HashMap<SourceFormat, Set<OutputFormat>> getFormats() {
        final HashMap<SourceFormat,Set<OutputFormat>> map = new HashMap<>();
        for (SourceFormat sourceFormat : ImageIoImageReader.supportedFormats()) {
            map.put(sourceFormat, ImageIoImageWriter.supportedFormats());
        }
        return map;
    }

    @Override
    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> formats = getFormats().get(sourceFormat);
        return (formats != null) ? formats : new HashSet<OutputFormat>();
    }

    @Override
    public Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException {
        ImageIoImageReader reader = new ImageIoImageReader();
        try {
            return reader.readSize(inputFile, sourceFormat);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public Dimension getSize(final StreamSource streamSource,
                             final SourceFormat sourceFormat)
            throws ProcessorException {
        ImageIoImageReader reader = new ImageIoImageReader();
        try {
            return reader.readSize(streamSource, sourceFormat);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public Set<ProcessorFeature> getSupportedFeatures(
            final SourceFormat sourceFormat) {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIiif1_1Qualities(final SourceFormat sourceFormat) {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities(final SourceFormat sourceFormat) {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_2_0_QUALITIES);
        }
        return qualities;
    }

    @Override
    public void process(final OperationList ops,
                        final SourceFormat sourceFormat,
                        final Dimension fullSize,
                        final File inputFile,
                        final OutputStream outputStream)
            throws ProcessorException {
        doProcess(ops, sourceFormat, inputFile, outputStream);
    }

    @Override
    public void process(final OperationList ops,
                        final SourceFormat sourceFormat,
                        final Dimension fullSize,
                        final StreamSource streamSource,
                        final OutputStream outputStream)
            throws ProcessorException {
        doProcess(ops, sourceFormat, streamSource, outputStream);
    }

    private void doProcess(final OperationList ops,
                           final SourceFormat sourceFormat,
                           final Object input,
                           final OutputStream outputStream)
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            final ImageIoImageReader reader = new ImageIoImageReader();
            final ReductionFactor rf = new ReductionFactor();
            RenderedImage renderedImage = null;
            if (input instanceof StreamSource) {
                renderedImage = reader.read((StreamSource) input,
                        sourceFormat, ops, rf);
            } else if (input instanceof File) {
                renderedImage = reader.read((File) input, sourceFormat, ops,
                        rf);
            }
            if (renderedImage != null) {
                RenderedOp renderedOp = JaiUtil.reformatImage(
                        RenderedOp.wrapRenderedImage(renderedImage),
                        new Dimension(renderedImage.getTileWidth(),
                                renderedImage.getTileHeight()));
                for (Operation op : ops) {
                    if (op instanceof Crop) {
                        renderedOp = JaiUtil.
                                cropImage(renderedOp, (Crop) op, rf);
                    } else if (op instanceof Scale) {
                        renderedOp = JaiUtil.
                                scaleImage(renderedOp, (Scale) op, rf);
                    } else if (op instanceof Transpose) {
                        renderedOp = JaiUtil.
                                transposeImage(renderedOp, (Transpose) op);
                    } else if (op instanceof Rotate) {
                        renderedOp = JaiUtil.
                                rotateImage(renderedOp, (Rotate) op);
                    } else if (op instanceof Filter) {
                        renderedOp = JaiUtil.
                                filterImage(renderedOp, (Filter) op);
                    }
                }
                ImageIoImageWriter writer = new ImageIoImageWriter();
                writer.write(renderedOp, ops.getOutputFormat(),
                        outputStream);
            }
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

}
