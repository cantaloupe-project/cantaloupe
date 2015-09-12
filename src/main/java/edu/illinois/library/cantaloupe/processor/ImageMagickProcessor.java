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
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.process.Pipe;
import org.im4java.process.ProcessStarter;
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

public class ImageMagickProcessor implements Processor {

    private static Logger logger = LoggerFactory.
            getLogger(ImageMagickProcessor.class);

    private static final HashMap<SourceFormat,Set<OutputFormat>> FORMATS =
            getAvailableOutputFormats();
    private static final Set<Quality> SUPPORTED_QUALITIES =
            new HashSet<Quality>();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<ProcessorFeature>();
    private static HashMap<SourceFormat, Set<OutputFormat>> supportedFormats;

    private ImageInputStream inputStream;
    private SourceFormat sourceFormat;

    static {
        // overrides the PATH; see
        // http://im4java.sourceforge.net/docs/dev-guide.html
        String binaryPath = Application.getConfiguration().
                getString("ImageMagickProcessor.path_to_binaries", "");
        if (binaryPath.length() > 0) {
            ProcessStarter.setGlobalSearchPath(binaryPath);
        }

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
        //SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_WHITELISTED);
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
    public static HashMap<SourceFormat, Set<OutputFormat>> getAvailableOutputFormats() {
        if (supportedFormats == null) {
            loadSupportedFormats();
        }
        return supportedFormats;
    }

    public static void loadSupportedFormats() {
        final Set<SourceFormat> sourceFormats = new HashSet<SourceFormat>();
        final Set<OutputFormat> outputFormats = new HashSet<OutputFormat>();

        try {
            // retrieve the output of the `identify -list format` command,
            // which contains a list of all supported formats
            Runtime runtime = Runtime.getRuntime();
            String cmdPath = "";
            Configuration config = Application.getConfiguration();
            if (config != null) {
                cmdPath = config.getString("ImageMagickProcessor.path_to_binaries", "");
            }
            String[] commands = {cmdPath + File.separator + "identify",
                    "-list", "format"};
            Process proc = runtime.exec(commands);
            BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            String s;

            while ((s = stdInput.readLine()) != null) {
                s = s.trim();
                if (s.startsWith("JP2")) {
                    sourceFormats.add(SourceFormat.JP2);
                    outputFormats.add(OutputFormat.JP2);
                }
                if (s.startsWith("JPEG")) {
                    sourceFormats.add(SourceFormat.JPG);
                    outputFormats.add(OutputFormat.JPG);
                }
                if (s.startsWith("PNG")) {
                    sourceFormats.add(SourceFormat.PNG);
                    outputFormats.add(OutputFormat.PNG);
                }
                if (s.startsWith("PDF")) {
                    outputFormats.add(OutputFormat.PDF);
                }
                if (s.startsWith("TIFF")) {
                    sourceFormats.add(SourceFormat.TIF);
                    outputFormats.add(OutputFormat.TIF);
                }
                if (s.startsWith("WEBP")) {
                    sourceFormats.add(SourceFormat.WEBP);
                    outputFormats.add(OutputFormat.WEBP);
                }

            }
        } catch (IOException e) {
            logger.error("Failed to execute identify command");
        }

        supportedFormats = new HashMap<SourceFormat,Set<OutputFormat>>();
        for (SourceFormat sourceFormat : sourceFormats) {
            supportedFormats.put(sourceFormat, outputFormats);
        }
    }

    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> formats = FORMATS.get(sourceFormat);
        if (formats == null) {
            formats = new HashSet<OutputFormat>();
        }
        return formats;
    }

    public Dimension getSize(ImageInputStream inputStream,
                             SourceFormat sourceFormat) throws InfoException {
        ImageInputStreamWrapper bridge = new ImageInputStreamWrapper(inputStream);
        Info sourceInfo = new Info(sourceFormat.getPreferredExtension() + ":-",
                bridge, true);
        return new Dimension(sourceInfo.getImageWidth(),
                sourceInfo.getImageHeight());
    };

    public Set<ProcessorFeature> getSupportedFeatures(SourceFormat sourceFormat) {
        return SUPPORTED_FEATURES;
    }

    public Set<Quality> getSupportedQualities(SourceFormat sourceFormat) {
        return SUPPORTED_QUALITIES;
    }

    public Set<SourceFormat> getSupportedSourceFormats() {
        return FORMATS.keySet();
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        ImageInputStream inputStream, OutputStream outputStream)
            throws Exception {
        this.inputStream = inputStream;
        this.sourceFormat = sourceFormat;

        IMOperation op = new IMOperation();
        op.addImage(sourceFormat.getPreferredExtension() + ":-"); // read from stdin
        op = assembleOperation(op, params);

        // format transformation
        op.addImage(params.getOutputFormat().getExtension() + ":-"); // write to stdout

        ImageInputStreamWrapper wrapper = new ImageInputStreamWrapper(inputStream);
        Pipe pipeIn = new Pipe(wrapper, null);
        Pipe pipeOut = new Pipe(null, outputStream);

        ConvertCmd convert = new ConvertCmd();
        convert.setInputProvider(pipeIn);
        convert.setOutputConsumer(pipeOut);
        convert.run(op);
        inputStream.close();
    }

    private IMOperation assembleOperation(IMOperation op, Parameters params) {
        // region transformation
        Region region = params.getRegion();
        if (!region.isFull()) {
            if (region.isPercent()) {
                try {
                    // im4java doesn't support cropping x/y by percentage (only
                    // width/height), so we have to calculate them.
                    Dimension imageSize = getSize(this.inputStream,
                            this.sourceFormat);
                    int x = (int) Math.round(region.getX() / 100.0 * imageSize.width);
                    int y = (int) Math.round(region.getY() / 100.0 * imageSize.height);
                    op.crop(Math.round(region.getWidth()),
                            Math.round(region.getHeight()), x, y, "%");
                } catch (InfoException e) {
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

        return op;
    }

}
