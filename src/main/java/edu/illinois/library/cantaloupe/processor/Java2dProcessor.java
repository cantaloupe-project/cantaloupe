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
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.restlet.data.MediaType;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Processor using the Java 2D framework.
 */
class Java2dProcessor implements ChannelProcessor, FileProcessor {

    public static final String JPG_QUALITY_CONFIG_KEY = "Java2dProcessor.jpg.quality";
    public static final String SCALE_MODE_CONFIG_KEY = "Java2dProcessor.scale_mode";
    public static final String TIF_READER_CONFIG_KEY = "Java2dProcessor.tif.reader";

    private static final HashMap<SourceFormat,Set<OutputFormat>> FORMATS =
            getAvailableOutputFormats();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.BITONAL);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.COLOR);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.GRAY);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.NATIVE);

        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.BITONAL);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.COLOR);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.DEFAULT);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.GRAY);

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
    public static HashMap<SourceFormat, Set<OutputFormat>>
    getAvailableOutputFormats() {
        final String[] readerMimeTypes = ImageIO.getReaderMIMETypes();
        final String[] writerMimeTypes = ImageIO.getWriterMIMETypes();
        final HashMap<SourceFormat,Set<OutputFormat>> map =
                new HashMap<>();
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            Set<OutputFormat> outputFormats = new HashSet<>();
            for (int i = 0, length = readerMimeTypes.length; i < length; i++) {
                if (sourceFormat.getMediaTypes().
                        contains(new MediaType(readerMimeTypes[i].toLowerCase()))) {
                    for (OutputFormat outputFormat : OutputFormat.values()) {
                        // TODO: not working (see inline comment in ProcessorUtil.writeImage())
                        if (outputFormat.equals(OutputFormat.JP2)) {
                            continue;
                        }
                        for (int i2 = 0, length2 = writerMimeTypes.length; i2 < length2; i2++) {
                            if (outputFormat.getMediaType().equals(writerMimeTypes[i2].toLowerCase())) {
                                outputFormats.add(outputFormat);
                            }
                        }
                    }
                }
            }
            map.put(sourceFormat, outputFormats);
        }
        return map;
    }

    @Override
    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> formats = FORMATS.get(sourceFormat);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
    }

    @Override
    public Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException {
        return ProcessorUtil.getSize(inputFile, sourceFormat);
    }

    @Override
    public Dimension getSize(ReadableByteChannel readableChannel,
                             SourceFormat sourceFormat)
            throws ProcessorException {
        return ProcessorUtil.getSize(readableChannel, sourceFormat);
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
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
    getSupportedIiif1_1Qualities(final SourceFormat sourceFormat) {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
    getSupportedIiif2_0Qualities(final SourceFormat sourceFormat) {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
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
                        final WritableByteChannel writableChannel)
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            ReductionFactor reductionFactor = new ReductionFactor();
            BufferedImage image = Java2dUtil.readImage(inputFile,
                    sourceFormat, ops, fullSize, reductionFactor);
            for (Operation op : ops) {
                if (op instanceof Crop) {
                    image = Java2dUtil.cropImage(image, (Crop) op,
                            reductionFactor);
                } else if (op instanceof Scale) {
                    final boolean highQuality = Application.getConfiguration().
                            getString(SCALE_MODE_CONFIG_KEY, "speed").
                            equals("quality");
                    image = Java2dUtil.scaleImageWithG2d(image, (Scale) op,
                            reductionFactor, highQuality);
                } else if (op instanceof Transpose) {
                    image = Java2dUtil.transposeImage(image, (Transpose) op);
                } else if (op instanceof Rotate) {
                    image = Java2dUtil.rotateImage(image, (Rotate) op);
                } else if (op instanceof Filter) {
                    image = Java2dUtil.filterImage(image, (Filter) op);
                }
            }
            Java2dUtil.writeImage(image, ops.getOutputFormat(),
                    writableChannel);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public void process(final OperationList ops,
                        final SourceFormat sourceFormat,
                        final Dimension fullSize,
                        final ReadableByteChannel readableChannel,
                        final WritableByteChannel writableChannel)
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            ReductionFactor reductionFactor = new ReductionFactor();
            BufferedImage image = Java2dUtil.readImage(readableChannel,
                    sourceFormat, ops, fullSize, reductionFactor);
            for (Operation op : ops) {
                if (op instanceof Crop) {
                    image = Java2dUtil.cropImage(image, (Crop) op,
                            reductionFactor);
                } else if (op instanceof Scale) {
                    final boolean highQuality = Application.getConfiguration().
                            getString(SCALE_MODE_CONFIG_KEY, "speed").
                            equals("quality");
                    image = Java2dUtil.scaleImageWithG2d(image, (Scale) op,
                            reductionFactor, highQuality);
                } else if (op instanceof Transpose) {
                    image = Java2dUtil.transposeImage(image, (Transpose) op);
                } else if (op instanceof Rotate) {
                    image = Java2dUtil.rotateImage(image, (Rotate) op);
                } else if (op instanceof Filter) {
                    image = Java2dUtil.filterImage(image, (Filter) op);
                }
            }
            Java2dUtil.writeImage(image, ops.getOutputFormat(),
                    writableChannel);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

}
