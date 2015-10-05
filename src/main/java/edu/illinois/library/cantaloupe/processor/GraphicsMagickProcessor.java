package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;
import edu.illinois.library.cantaloupe.request.Rotation;
import edu.illinois.library.cantaloupe.request.Size;
import org.apache.commons.configuration.Configuration;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.process.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Processor using the GraphicsMagick `gm` command-line tool.
 */
class GraphicsMagickProcessor implements StreamProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(GraphicsMagickProcessor.class);

    private static final Set<Quality> SUPPORTED_QUALITIES = new HashSet<>();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    // Lazy-initialized by getFormats()
    private static HashMap<SourceFormat, Set<OutputFormat>> supportedFormats;

    private ImageInputStream inputStream;
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
     * based on information reported by <code>gm version</code>.
     */
    private static HashMap<SourceFormat, Set<OutputFormat>> getFormats() {
        if (supportedFormats == null) {
            final Set<SourceFormat> sourceFormats = new HashSet<>();
            final Set<OutputFormat> outputFormats = new HashSet<>();
            String cmdPath = "gm";
            try {
                // retrieve the output of the `gm version` command, which contains a
                // list of all optional formats
                Runtime runtime = Runtime.getRuntime();
                Configuration config = Application.getConfiguration();
                if (config != null) {
                    String pathPrefix = config.getString("GraphicsMagickProcessor.path_to_binaries");
                    if (pathPrefix != null) {
                        cmdPath = pathPrefix + File.separator + cmdPath;
                    }
                }
                String[] commands = {cmdPath, "version"};
                Process proc = runtime.exec(commands);
                BufferedReader stdInput = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()));
                String s;
                boolean read = false;
                while ((s = stdInput.readLine()) != null) {
                    s = s.trim();
                    if (s.contains("Feature Support")) {
                        read = true; // start reading
                    } else if (s.contains("Host type:")) {
                        break; // stop reading
                    }
                    if (read) {
                        if (s.startsWith("JPEG-2000  ") && s.endsWith(" yes")) {
                            sourceFormats.add(SourceFormat.JP2);
                            outputFormats.add(OutputFormat.JP2);
                        }
                        if (s.startsWith("JPEG  ") && s.endsWith(" yes")) {
                            sourceFormats.add(SourceFormat.JPG);
                            outputFormats.add(OutputFormat.JPG);
                        }
                        if (s.startsWith("PNG  ") && s.endsWith(" yes")) {
                            sourceFormats.add(SourceFormat.PNG);
                            outputFormats.add(OutputFormat.PNG);
                        }
                        if (s.startsWith("Ghostscript") && s.endsWith(" yes")) {
                            outputFormats.add(OutputFormat.PDF);
                        }
                        if (s.startsWith("TIFF  ") && s.endsWith(" yes")) {
                            sourceFormats.add(SourceFormat.TIF);
                            outputFormats.add(OutputFormat.TIF);
                        }
                        if (s.startsWith("WebP  ") && s.endsWith(" yes")) {
                            sourceFormats.add(SourceFormat.WEBP);
                            outputFormats.add(OutputFormat.WEBP);
                        }
                    }
                }

                // add formats that are not listed in the output of "gm version"
                // but are definitely available
                // (http://www.graphicsmagick.org/formats.html)
                sourceFormats.add(SourceFormat.BMP);
                sourceFormats.add(SourceFormat.GIF);
                outputFormats.add(OutputFormat.GIF);
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

    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> formats = getFormats().get(sourceFormat);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
    }

    public Dimension getSize(ImageInputStream inputStream,
                             SourceFormat sourceFormat)
            throws ProcessorException {
        try {
            inputStream.mark();
            ImageInputStreamWrapper wrapper = new ImageInputStreamWrapper(inputStream);
            Info sourceInfo = new Info(sourceFormat.getPreferredExtension() + ":-",
                    wrapper, true);
            Dimension dimension = new Dimension(sourceInfo.getImageWidth(),
                    sourceInfo.getImageHeight());
            inputStream.reset();
            return dimension;
        } catch (InfoException|IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    public Set<ProcessorFeature> getSupportedFeatures(
            final SourceFormat sourceFormat) {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    public Set<Quality> getSupportedQualities(final SourceFormat sourceFormat) {
        Set<Quality> qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_QUALITIES);
        }
        return qualities;
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        ImageInputStream inputStream, OutputStream outputStream)
            throws ProcessorException {
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
            assembleOperation(op, params);

            // format transformation
            op.addImage(params.getOutputFormat().getExtension() + ":-"); // write to stdout

            ImageInputStreamWrapper wrapper = new ImageInputStreamWrapper(inputStream);
            Pipe pipeIn = new Pipe(wrapper, null);
            Pipe pipeOut = new Pipe(null, outputStream);

            ConvertCmd convert = new ConvertCmd(true);

            String binaryPath = Application.getConfiguration().
                    getString("GraphicsMagickProcessor.path_to_binaries", "");
            if (binaryPath.length() > 0) {
                convert.setSearchPath(binaryPath);
            }
            convert.setInputProvider(pipeIn);
            convert.setOutputConsumer(pipeOut);
            convert.run(op);
        } catch (InterruptedException|IM4JavaException|IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    private void assembleOperation(IMOperation op, Parameters params) {
        // region transformation
        Region region = params.getRegion();
        if (!region.isFull()) {
            if (region.isPercent()) {
                try {
                    // im4java doesn't support cropping x/y by percentage (only
                    // width/height), so we have to calculate them.
                    Dimension imageSize = getSize(this.inputStream,
                            this.sourceFormat);
                    int x = Math.round(region.getX() / 100.0f * imageSize.width);
                    int y = Math.round(region.getY() / 100.0f * imageSize.height);
                    int width = Math.round(region.getWidth());
                    int height = Math.round(region.getHeight());
                    op.crop(width, height, x, y, "%");
                } catch (ProcessorException e) {
                    logger.debug("Failed to get dimensions for {}",
                            params.getIdentifier());
                }
            } else {
                op.crop(Math.round(region.getWidth()), Math.round(region.getHeight()),
                        Math.round(region.getX()), Math.round(region.getY()));
            }
        }

        // size transformation
        Size size = params.getSize();
        if (size.getScaleMode() != Size.ScaleMode.FULL) {
            if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_WIDTH) {
                op.resize(size.getWidth());
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_HEIGHT) {
                op.resize(null, size.getHeight());
            } else if (size.getScaleMode() == Size.ScaleMode.NON_ASPECT_FILL) {
                op.resize(size.getWidth(), size.getHeight(), "!".charAt(0));
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_INSIDE) {
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
