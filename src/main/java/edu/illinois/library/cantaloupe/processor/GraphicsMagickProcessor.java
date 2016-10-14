package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Color;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Transpose;
import org.apache.commons.lang3.StringUtils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Processor using the GraphicsMagick <code>gm</code> command-line tool.
 * Tested with version 1.3.21; other versions may or may not work.</p>
 *
 * <p>This class does not implement <var>FileProcessor</var> because testing
 * indicates that reading from streams is significantly faster.</p>
 *
 * <p>This processor does not respect the
 * {@link edu.illinois.library.cantaloupe.resource.AbstractResource#PRESERVE_METADATA_CONFIG_KEY}
 * setting because to not preserve metadata would entail not preserving an
 * ICC profile. Thus, metadata always passes through.</p>
 */
class GraphicsMagickProcessor extends Im4JavaProcessor
        implements StreamProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(GraphicsMagickProcessor.class);

    static final String BACKGROUND_COLOR_CONFIG_KEY =
            "GraphicsMagickProcessor.background_color";
    static final String SHARPEN_CONFIG_KEY = "GraphicsMagickProcessor.sharpen";
    static final String PATH_TO_BINARIES_CONFIG_KEY =
            "GraphicsMagickProcessor.path_to_binaries";

    // Lazy-initialized by getFormats()
    private static HashMap<Format, Set<Format>> supportedFormats;

    /**
     * @param binaryName Name of an executable
     * @return
     */
    private static String getPath(String binaryName) {
        String path = Configuration.getInstance().
                getString(PATH_TO_BINARIES_CONFIG_KEY);
        if (path != null && path.length() > 0) {
            path = StringUtils.stripEnd(path, File.separator) + File.separator +
                    binaryName;
        } else {
            path = binaryName;
        }
        return path;
    }

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by <code>gm version</code>.
     */
    private static HashMap<Format, Set<Format>> getFormats() {
        if (supportedFormats == null) {
            final Set<Format> formats = new HashSet<>();
            final Set<Format> outputFormats = new HashSet<>();

            // Get the output of the `gm version` command, which contains
            // a list of all optional formats.
            final ProcessBuilder pb = new ProcessBuilder();
            final List<String> command = new ArrayList<>();
            command.add(getPath("gm"));
            command.add("version");
            pb.command(command);
            final String commandString = StringUtils.join(pb.command(), " ");

            try {
                logger.info("Executing {}", commandString);
                final Process process = pb.start();

                try (final InputStream processInputStream = process.getInputStream()) {
                    BufferedReader stdInput = new BufferedReader(
                            new InputStreamReader(processInputStream));
                    String s;
                    boolean read = false;
                    while ((s = stdInput.readLine()) != null) {
                        if (s.contains("Feature Support")) {
                            read = true; // start reading
                        } else if (s.contains("Host type:")) {
                            break; // stop reading
                        }
                        if (read) {
                            s = s.trim();
                            if (s.startsWith("JPEG-2000 ") && s.endsWith(" yes")) {
                                formats.add(Format.JP2);
                                outputFormats.add(Format.JP2);
                            } else if (s.startsWith("JPEG ") && s.endsWith(" yes")) {
                                formats.add(Format.JPG);
                                outputFormats.add(Format.JPG);
                            } else if (s.startsWith("PNG ") && s.endsWith(" yes")) {
                                formats.add(Format.PNG);
                                outputFormats.add(Format.PNG);
                            } else if (s.startsWith("Ghostscript") && s.endsWith(" yes")) {
                                outputFormats.add(Format.PDF);
                            } else if (s.startsWith("TIFF ") && s.endsWith(" yes")) {
                                formats.add(Format.TIF);
                                outputFormats.add(Format.TIF);
                            } else if (s.startsWith("WebP ") && s.endsWith(" yes")) {
                                formats.add(Format.WEBP);
                                outputFormats.add(Format.WEBP);
                            }
                        }
                    }
                    process.waitFor();

                    // add formats that are not listed in the output of
                    // "gm version" but are definitely available
                    // (http://www.graphicsmagick.org/formats.html)
                    formats.add(Format.BMP);
                    formats.add(Format.GIF);
                    // GIF output is buggy
                    //outputFormats.add(OutputFormat.GIF);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }

            supportedFormats = new HashMap<>();
            for (Format format : formats) {
                supportedFormats.put(format, outputFormats);
            }
        }
        return supportedFormats;
    }

    GraphicsMagickProcessor() {
        final Configuration config = Configuration.getInstance();
        final String path = config.getString(PATH_TO_BINARIES_CONFIG_KEY);
        if (path != null && path.length() > 0) {
            ProcessStarter.setGlobalSearchPath(path);
        }
    }

    void assembleOperation(final IMOperation imOp,
                           final OperationList ops,
                           final Dimension fullSize,
                           final String backgroundColor) {
        for (Operation op : ops) {
            if (op instanceof Crop) {
                Crop crop = (Crop) op;
                if (!crop.isNoOp()) {
                    if (crop.getShape().equals(Crop.Shape.SQUARE)) {
                        final int shortestSide =
                                Math.min(fullSize.width, fullSize.height);
                        int x = (fullSize.width - shortestSide) / 2;
                        int y = (fullSize.height - shortestSide) / 2;
                        imOp.crop(shortestSide, shortestSide, x, y);
                    } else if (crop.getUnit().equals(Crop.Unit.PERCENT)) {
                        // im4java doesn't support cropping x/y by percentage
                        // (only width/height), so we have to calculate them.
                        int x = Math.round(crop.getX() * fullSize.width);
                        int y = Math.round(crop.getY() * fullSize.height);
                        int width = Math.round(crop.getWidth() * 100);
                        int height = Math.round(crop.getHeight() * 100);
                        imOp.crop(width, height, x, y, "%");
                    } else {
                        imOp.crop(Math.round(crop.getWidth()),
                                Math.round(crop.getHeight()),
                                Math.round(crop.getX()),
                                Math.round(crop.getY()));
                    }
                }
            } else if (op instanceof Scale) {
                Scale scale = (Scale) op;
                if (!scale.isNoOp()) {
                    if (scale.getPercent() != null) {
                        imOp.resize(Math.round(scale.getPercent() * 100),
                                Math.round(scale.getPercent() * 100), "%");
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        imOp.resize(scale.getWidth());
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        imOp.resize(null, scale.getHeight());
                    } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                        imOp.resize(scale.getWidth(), scale.getHeight(), "!");
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        imOp.resize(scale.getWidth(), scale.getHeight());
                    }
                }
            } else if (op instanceof Transpose) {
                switch ((Transpose) op) {
                    case HORIZONTAL:
                        imOp.flop();
                        break;
                    case VERTICAL:
                        imOp.flip();
                        break;
                }
            } else if (op instanceof Rotate) {
                final Rotate rotate = (Rotate) op;
                if (!rotate.isNoOp()) {
                    // If the output format supports transparency, make the
                    // background transparent. Otherwise, use a
                    // user-configurable background color.
                    if (ops.getOutputFormat().supportsTransparency()) {
                        imOp.background("none");
                    } else {
                        imOp.background(backgroundColor);
                    }
                    imOp.rotate((double) rotate.getDegrees());
                }
            } else if (op instanceof Color) {
                switch ((Color) op) {
                    case GRAY:
                        imOp.colorspace("Gray");
                        break;
                    case BITONAL:
                        imOp.monochrome();
                        break;
                }
            }
        }

        imOp.depth(8);

        // Apply the sharpen operation, if present.
        final Configuration config = Configuration.getInstance();
        final double sharpenValue = config.getDouble(SHARPEN_CONFIG_KEY, 0);
        imOp.unsharp(sharpenValue);
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        Set<Format> formats = getFormats().get(format);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
    }

    @Override
    public void process(final OperationList ops,
                        final ImageInfo imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        if (!getAvailableOutputFormats().contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        final Configuration config = Configuration.getInstance();
        try {
            IMOperation op = new IMOperation();
            op.addImage(format.getPreferredExtension() + ":-");
            String bgColor =
                    config.getString(BACKGROUND_COLOR_CONFIG_KEY, "black");
            assembleOperation(op, ops, imageInfo.getSize(), bgColor);

            op.addImage(ops.getOutputFormat().getPreferredExtension() + ":-"); // write to stdout

            // true = use GraphicsMagick instead of ImageMagick
            ConvertCmd convert = new ConvertCmd(true);

            try (InputStream inputStream = streamSource.newInputStream()) {
                convert.setInputProvider(new Pipe(inputStream, null));
                convert.setOutputConsumer(new Pipe(null, outputStream));
                convert.run(op);
            }
        } catch (InterruptedException | IM4JavaException | IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

}
