package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.watermark.WatermarkService;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Processor using the Java Advanced Imaging (JAI) framework.
 *
 * @see <a href="http://docs.oracle.com/cd/E19957-01/806-5413-10/806-5413-10.pdf">
 *     Programming in Java Advanced Imaging</a>
 */
class JaiProcessor extends AbstractProcessor
        implements FileProcessor, StreamProcessor {

    private static Logger logger = LoggerFactory.getLogger(JaiProcessor.class);

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
    public Set<OutputFormat> getAvailableOutputFormats() {
        Set<OutputFormat> formats = getFormats().get(sourceFormat);
        return (formats != null) ? formats : new HashSet<OutputFormat>();
    }

    @Override
    public Dimension getSize() throws ProcessorException {
        Java2dProcessor j2dproc = new Java2dProcessor();
        j2dproc.setSourceFormat(sourceFormat);
        if (sourceFile != null) {
            j2dproc.setSourceFile(sourceFile);
        } else {
            j2dproc.setStreamSource(streamSource);
        }
        return j2dproc.getSize();
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
    public List<Dimension> getTileSizes() throws ProcessorException {
        Java2dProcessor j2dproc = new Java2dProcessor();
        j2dproc.setSourceFormat(sourceFormat);
        if (sourceFile != null) {
            j2dproc.setSourceFile(sourceFile);
        } else {
            j2dproc.setStreamSource(streamSource);
        }
        return j2dproc.getTileSizes();
    }

    @Override
    public void process(final OperationList ops,
                        final Dimension fullSize,
                        final OutputStream outputStream)
            throws ProcessorException {
        if (!getAvailableOutputFormats().contains(ops.getOutputFormat())) {
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
            RenderedImage renderedImage = reader.readRendered(ops, rf);

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
                if (WatermarkService.isEnabled()) {
                    // Let's cheat and apply the watermark using Java 2D.
                    // There seems to be minimal performance penalty in doing
                    // this, and doing it in JAI is harder.
                    BufferedImage image = renderedOp.getAsBufferedImage();
                    try {
                        image = Java2dUtil.applyWatermark(image);
                    } catch (ConfigurationException e) {
                        logger.error(e.getMessage());
                    }
                    writer.write(image, ops.getOutputFormat(), outputStream);
                } else {
                    writer.write(renderedOp, ops.getOutputFormat(),
                            outputStream);
                }
            }
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
