package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.Rotation;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Quality;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Transpose;
import it.geosolutions.jaiext.JAIExt;
import org.restlet.data.MediaType;

import javax.imageio.ImageIO;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.RenderingHints;
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

    private static final int JAI_TILE_SIZE = 512;
    private static final Set<Quality> SUPPORTED_QUALITIES = new HashSet<>();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();

    private static HashMap<SourceFormat,Set<OutputFormat>> formatsMap;

    static {
        // replace the JRE JAI operations with GeoTools JAI-EXT
        JAIExt.initJAIEXT();

        SUPPORTED_QUALITIES.add(Quality.BITONAL);
        SUPPORTED_QUALITIES.add(Quality.COLOR);
        SUPPORTED_QUALITIES.add(Quality.DEFAULT);
        SUPPORTED_QUALITIES.add(Quality.GRAY);

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
                                // these currently don't work (see outputImage())
                                continue;
                            }
                            for (i = 0, length = writerMimeTypes.length; i < length; i++) {
                                if (outputFormat.getMediaType().equals(writerMimeTypes[i].toLowerCase())) {
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
    public Set<Quality> getSupportedQualities(final SourceFormat sourceFormat) {
        Set<Quality> qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_QUALITIES);
        }
        return qualities;
    }

    @Override
    public void process(Operations ops, SourceFormat sourceFormat,
                        Dimension sourceSize, File inputFile,
                        OutputStream outputStream) throws ProcessorException {
        doProcess(ops, sourceFormat, inputFile, outputStream);
    }

    @Override
    public void process(Operations ops, SourceFormat sourceFormat,
                        Dimension fullSize, InputStream inputStream,
                        OutputStream outputStream) throws ProcessorException {
        doProcess(ops, sourceFormat, inputStream, outputStream);
    }

    private void doProcess(Operations ops, SourceFormat sourceFormat,
                           Object input, OutputStream outputStream)
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            RenderedOp image = loadImage(input);
            for (Operation op : ops) {
                if (op instanceof Crop) {
                    image = ProcessorUtil.cropImage(image, (Crop) op);
                } else if (op instanceof Scale) {
                    image = ProcessorUtil.scaleImage(image, (Scale) op);
                } else if (op instanceof Transpose) {
                    image = ProcessorUtil.transposeImage(image, (Transpose) op);
                } else if (op instanceof Rotation) {
                    image = ProcessorUtil.rotateImage(image, (Rotation) op);
                } else if (op instanceof Quality) {
                    image = ProcessorUtil.filterImage(image, (Quality) op);
                }
            }
            ProcessorUtil.writeImage(image, ops.getOutputFormat(),
                    outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    private RenderedOp loadImage(Object input) {
        ParameterBlockJAI pbj = new ParameterBlockJAI("ImageRead");
        ImageLayout layout = new ImageLayout();
        layout.setTileWidth(JAI_TILE_SIZE);
        layout.setTileHeight(JAI_TILE_SIZE);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        pbj.setParameter("Input", input);
        return JAI.create("ImageRead", pbj, hints);
    }

}
