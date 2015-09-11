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
import org.im4java.core.CommandException;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.process.Pipe;
import org.im4java.process.ProcessStarter;
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

public class GraphicsMagickProcessor implements Processor {

    private static Logger logger = LoggerFactory.
            getLogger(GraphicsMagickProcessor.class);

    private static final HashMap<SourceFormat,Set<OutputFormat>> FORMATS =
            getAvailableOutputFormats();
    private static final Set<Quality> SUPPORTED_QUALITIES =
            new HashSet<Quality>();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<ProcessorFeature>();
    private static HashMap<SourceFormat, Set<OutputFormat>> supportedFormats;

    private File file;
    private InputStream inputStream;
    private SourceFormat sourceFormat;

    static {
        // overrides the PATH; see
        // http://im4java.sourceforge.net/docs/dev-guide.html
        String binaryPath = Application.getConfiguration().
                getString("GraphicsMagickProcessor.path_to_binaries", "");
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
     * based on information reported by <code>gm version</code>.
     */
    public static HashMap<SourceFormat, Set<OutputFormat>> getAvailableOutputFormats() {
        if (supportedFormats == null) {
            loadSupportedFormats();
        }
        return supportedFormats;
    }

    private static void loadSupportedFormats() {
        final Set<SourceFormat> sourceFormats = new HashSet<SourceFormat>();
        final Set<OutputFormat> outputFormats = new HashSet<OutputFormat>();

        try {
            // retrieve the output of the `gm version` command, which contains a
            // list of all optional formats
            Runtime runtime = Runtime.getRuntime();
            String cmdPath = "";
            Configuration config = Application.getConfiguration();
            if (config != null) {
                cmdPath = config.getString("GraphicsMagickProcessor.path_to_binaries", "");
            }
            String[] commands = {cmdPath + File.separator + "gm", "version"};
            Process proc = runtime.exec(commands);
            BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            String s;
            boolean read = false;
            while ((s = stdInput.readLine()) != null) {
                s = s.trim();
                if (s.contains("Feature Support")) {
                    read = true;
                } else if (s.contains("Host type:")) {
                    read = false;
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
        } catch (IOException e) {
            logger.error("Failed to execute gm command");
        }

        // add formats that are definitely available
        // (http://www.graphicsmagick.org/formats.html)
        sourceFormats.add(SourceFormat.BMP);
        sourceFormats.add(SourceFormat.GIF);
        outputFormats.add(OutputFormat.GIF);

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

    public Dimension getSize(File sourceFile, SourceFormat sourceFormat)
            throws InfoException {
        Info sourceInfo = new Info(sourceFormat.getPreferredExtension() + ":" +
                sourceFile.getAbsolutePath(), true);
        return new Dimension(sourceInfo.getImageWidth(),
                sourceInfo.getImageHeight());
    }

    public Dimension getSize(InputStream inputStream, SourceFormat sourceFormat)
            throws InfoException {
        Info sourceInfo = new Info(sourceFormat.getPreferredExtension() + ":-",
                inputStream, true);
        return new Dimension(sourceInfo.getImageWidth(),
                sourceInfo.getImageHeight());
    }

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
                        File file, OutputStream outputStream) throws Exception {
        this.file = file;
        this.sourceFormat = sourceFormat;

        IMOperation op = new IMOperation();
        op.addImage(file.getAbsolutePath());
        op = assembleOperation(op, params);

        // format transformation
        op.addImage(params.getOutputFormat().getExtension() + ":-"); // write to stdout

        Pipe pipeOut = new Pipe(null, outputStream);

        ConvertCmd convert = new ConvertCmd(true);
        convert.setOutputConsumer(pipeOut);
        try {
            convert.run(op);
        } catch (CommandException e) {
            // im4java throws this apparently spuriously when using
            // IMOperation.resize(width) with GraphicsMagick only.
            if (!e.getMessage().contains("unable to resize image (Non-zero width and height required)")) {
                throw e;
            }
        }
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        InputStream inputStream, OutputStream outputStream)
            throws Exception {
        this.inputStream = inputStream;
        this.sourceFormat = sourceFormat;

        IMOperation op = new IMOperation();
        op.addImage(sourceFormat.getPreferredExtension() + ":-"); // read from stdin
        op = assembleOperation(op, params);

        // format transformation
        op.addImage(params.getOutputFormat().getExtension() + ":-"); // write to stdout

        Pipe pipeIn = new Pipe(inputStream, null);
        Pipe pipeOut = new Pipe(null, outputStream);

        ConvertCmd convert = new ConvertCmd(true);
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
                    Dimension imageSize;
                    if (this.file != null) {
                        imageSize = getSize(this.file, this.sourceFormat);
                    } else {
                        imageSize = getSize(this.inputStream, this.sourceFormat);
                    }
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
