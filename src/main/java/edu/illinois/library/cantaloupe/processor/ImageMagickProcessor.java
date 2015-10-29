package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Quality;
import edu.illinois.library.cantaloupe.image.Region;
import edu.illinois.library.cantaloupe.image.Rotation;
import org.apache.commons.configuration.Configuration;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.process.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Processor using the ImageMagick `convert` and `identify` command-line tools.
 */
class ImageMagickProcessor implements StreamProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(ImageMagickProcessor.class);

    private static final Set<Quality> SUPPORTED_QUALITIES = new HashSet<>();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    // Lazy-initialized by getFormats()
    private static HashMap<SourceFormat, Set<OutputFormat>> supportedFormats;

    private InputStream inputStream;
    private SourceFormat sourceFormat;

    static {
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
     * based on information reported by <code>identify -list format</code>.
     */
    private static HashMap<SourceFormat, Set<OutputFormat>> getFormats() {
        if (supportedFormats == null) {
            final Set<SourceFormat> sourceFormats = new HashSet<>();
            final Set<OutputFormat> outputFormats = new HashSet<>();
            String cmdPath = "identify";
            try {
                // retrieve the output of the `identify -list format` command,
                // which contains a list of all supported formats
                Runtime runtime = Runtime.getRuntime();
                Configuration config = Application.getConfiguration();
                if (config != null) {
                    String pathPrefix = config.getString("ImageMagickProcessor.path_to_binaries");
                    if (pathPrefix != null) {
                        cmdPath = pathPrefix + File.separator + cmdPath;
                    }
                }
                String[] commands = {cmdPath, "-list", "format"};
                Process proc = runtime.exec(commands);
                BufferedReader stdInput = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()));
                String s;

                while ((s = stdInput.readLine()) != null) {
                    s = s.trim();
                    if (s.startsWith("JP2")) {
                        sourceFormats.add(SourceFormat.JP2);
                        if (s.contains(" rw")) {
                            outputFormats.add(OutputFormat.JP2);
                        }
                    }
                    if (s.startsWith("JPEG")) {
                        sourceFormats.add(SourceFormat.JPG);
                        if (s.contains(" rw")) {
                            outputFormats.add(OutputFormat.JPG);
                        }
                    }
                    if (s.startsWith("PNG")) {
                        sourceFormats.add(SourceFormat.PNG);
                        if (s.contains(" rw")) {
                            outputFormats.add(OutputFormat.PNG);
                        }
                    }
                    if (s.startsWith("PDF") && s.contains(" rw")) {
                        outputFormats.add(OutputFormat.PDF);
                    }
                    if (s.startsWith("TIFF")) {
                        sourceFormats.add(SourceFormat.TIF);
                        if (s.contains(" rw")) {
                            outputFormats.add(OutputFormat.TIF);
                        }
                    }
                    if (s.startsWith("WEBP")) {
                        sourceFormats.add(SourceFormat.WEBP);
                        if (s.contains(" rw")) {
                            outputFormats.add(OutputFormat.WEBP);
                        }
                    }

                }
            } catch (IOException e) {
                logger.error("Failed to execute {}", cmdPath);
            }

            supportedFormats = new HashMap<>();
            for (SourceFormat sourceFormat : sourceFormats) {
                supportedFormats.put(sourceFormat, outputFormats);
            }
        }
        return supportedFormats;
    }

    @Override
    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> formats = getFormats().get(sourceFormat);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
    }

    @Override
    public Dimension getSize(InputStream inputStream, SourceFormat sourceFormat)
            throws ProcessorException {
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        }
        try {
            Info sourceInfo = new Info(sourceFormat.getPreferredExtension() + ":-",
                    inputStream, true);
            Dimension dimension = new Dimension(sourceInfo.getImageWidth(),
                    sourceInfo.getImageHeight());
            return dimension;
        } catch (IM4JavaException e) {
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
    public Set<Quality> getSupportedQualities(final SourceFormat sourceFormat) {
        Set<Quality> qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_QUALITIES);
        }
        return qualities;
    }

    @Override
    public void process(Operations params, SourceFormat sourceFormat,
                        Dimension fullSize, InputStream inputStream,
                        OutputStream outputStream) throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(params.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        this.inputStream = inputStream;
        this.sourceFormat = sourceFormat;

        try {
            IMOperation op = new IMOperation();
            op.addImage(sourceFormat.getPreferredExtension() + ":-"); // read from stdin
            assembleOperation(op, params, fullSize);

            // format transformation
            op.addImage(params.getOutputFormat().getExtension() + ":-"); // write to stdout

            Pipe pipeIn = new Pipe(inputStream, null);
            Pipe pipeOut = new Pipe(null, outputStream);

            ConvertCmd convert = new ConvertCmd();

            String binaryPath = Application.getConfiguration().
                    getString("ImageMagickProcessor.path_to_binaries", "");
            if (binaryPath.length() > 0) {
                convert.setSearchPath(binaryPath);
            }
            convert.setInputProvider(pipeIn);
            convert.setOutputConsumer(pipeOut);
            convert.run(op);
        } catch (IOException|IM4JavaException|InterruptedException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    private void assembleOperation(IMOperation op, Operations params,
                                   Dimension fullSize) {
        // region transformation
        Region region = params.getRegion();
        if (!region.isFull()) {
            if (region.isPercent()) {
                // im4java doesn't support cropping x/y by percentage (only
                // width/height), so we have to calculate them.
                int x = Math.round(region.getX() / 100.0f * fullSize.width);
                int y = Math.round(region.getY() / 100.0f * fullSize.height);
                int width = Math.round(region.getWidth());
                int height = Math.round(region.getHeight());
                op.crop(width, height, x, y, "%");
            } else {
                op.crop(Math.round(region.getWidth()), Math.round(region.getHeight()),
                        Math.round(region.getX()), Math.round(region.getY()));
            }
        }

        // size transformation
        Scale size = params.getSize();
        if (size.getScaleMode() != Scale.Mode.FULL) {
            if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                op.resize(size.getWidth());
            } else if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                op.resize(null, size.getHeight());
            } else if (size.getScaleMode() == Scale.Mode.NON_ASPECT_FILL) {
                op.resize(size.getWidth(), size.getHeight(), "!".charAt(0));
            } else if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                op.resize(size.getWidth(), size.getHeight());
            } else if (size.getPercent() != null) {
                op.resize(Math.round(size.getPercent()),
                        Math.round(size.getPercent()),
                        "%".charAt(0));
            }
        }

        // rotation transformation
        Rotation rotation = params.getRotation();
        if (rotation.shouldMirror()) {
            op.flop();
        }
        if (rotation.getDegrees() != 0) {
            op.rotate(rotation.getDegrees().doubleValue());
        }

        // quality transformation
        Quality quality = params.getQuality();
        if (quality != Quality.COLOR && quality != Quality.DEFAULT) {
            switch (quality) {
                case GRAY:
                    op.colorspace("Gray");
                    break;
                case BITONAL:
                    op.monochrome();
                    break;
            }
        }
    }

}
