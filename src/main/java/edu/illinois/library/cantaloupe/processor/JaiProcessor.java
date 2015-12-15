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
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import it.geosolutions.jaiext.JAIExt;
import org.restlet.data.MediaType;

import javax.imageio.ImageIO;
import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Processor using the Java Advanced Imaging (JAI) framework.
 */
class JaiProcessor implements FileProcessor, StreamProcessor {

    // TODO: this should be used in conjunction with tile offset to match the crop region
    private static final int JAI_TILE_SIZE = 512;
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

    private static HashMap<SourceFormat,Set<OutputFormat>> formatsMap;

    static {
        // use GeoTools JAI-EXT instead of Sun JAI
        JAIExt.initJAIEXT();

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
    public static HashMap<SourceFormat, Set<OutputFormat>> getFormats() {
        if (formatsMap == null) {
            final String[] readerMimeTypes = ImageIO.getReaderMIMETypes();
            final String[] writerMimeTypes = ImageIO.getWriterMIMETypes();
            formatsMap = new HashMap<>();
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                Set<OutputFormat> outputFormats = new HashSet<>();
                for (int i = 0, length = readerMimeTypes.length; i < length; i++) {
                    if (sourceFormat.getMediaTypes().
                            contains(new MediaType(readerMimeTypes[i].toLowerCase()))) {
                        for (OutputFormat outputFormat : OutputFormat.values()) {
                            if (outputFormat == OutputFormat.GIF ||
                                    outputFormat == OutputFormat.JP2) {
                                // these currently don't work
                                // (see ProcessorUtil.writeImage(RenderedOp))
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
                formatsMap.put(sourceFormat, outputFormats);
            }
        }
        return formatsMap;
    }

    @Override
    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        return getFormats().get(sourceFormat);
    }

    @Override
    public Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException {
        return ProcessorUtil.getSize(inputFile, sourceFormat);
    }

    @Override
    public Dimension getSize(InputStream inputStream, SourceFormat sourceFormat)
            throws ProcessorException {
        return ProcessorUtil.getSize(inputStream, sourceFormat);
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
    public void process(OperationList ops, SourceFormat sourceFormat,
                        Dimension fullSize, File inputFile,
                        OutputStream outputStream) throws ProcessorException {
        doProcess(ops, sourceFormat, fullSize, inputFile, outputStream);
    }

    @Override
    public void process(OperationList ops, SourceFormat sourceFormat,
                        Dimension fullSize, InputStream inputStream,
                        OutputStream outputStream) throws ProcessorException {
        doProcess(ops, sourceFormat, fullSize, inputStream, outputStream);
    }

    private void doProcess(OperationList ops, SourceFormat sourceFormat,
                           Dimension fullSize, Object input,
                           OutputStream outputStream) throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            RenderedImage renderedImage = null;
            ReductionFactor rf = new ReductionFactor();
            if (input instanceof InputStream) {
                renderedImage = ProcessorUtil.readImageWithJai(
                        (InputStream) input, sourceFormat, ops, fullSize, rf);
            } else if (input instanceof File) {
                renderedImage = ProcessorUtil.readImageWithJai(
                        (File) input, sourceFormat, ops, fullSize, rf);
            }
            if (renderedImage != null) {
                RenderedOp renderedOp = ProcessorUtil.reformatImage(
                        RenderedOp.wrapRenderedImage(renderedImage),
                        new Dimension(JAI_TILE_SIZE, JAI_TILE_SIZE));
                for (Operation op : ops) {
                    if (op instanceof Crop) {
                        renderedOp = ProcessorUtil.
                                cropImage(renderedOp, (Crop) op, rf.factor);
                    } else if (op instanceof Scale) {
                        renderedOp = ProcessorUtil.
                                scaleImage(renderedOp, (Scale) op, rf.factor);
                    } else if (op instanceof Transpose) {
                        renderedOp = ProcessorUtil.
                                transposeImage(renderedOp, (Transpose) op);
                    } else if (op instanceof Rotate) {
                        renderedOp = ProcessorUtil.
                                rotateImage(renderedOp, (Rotate) op);
                    } else if (op instanceof Filter) {
                        renderedOp = ProcessorUtil.
                                filterImage(renderedOp, (Filter) op);
                    }
                }
                ProcessorUtil.writeImage(renderedOp, ops.getOutputFormat(),
                        outputStream);
            }
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

}
