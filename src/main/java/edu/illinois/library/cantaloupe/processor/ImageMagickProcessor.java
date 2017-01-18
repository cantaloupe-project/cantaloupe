package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Transpose;
import org.apache.commons.lang3.StringUtils;
import org.im4java.process.ArrayListOutputConsumer;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Processor using the ImageMagick `magick` (version 7) or `convert` and
 * `identify` (earlier versions) command-line tools.</p>
 *
 * <p>This class does not implement <var>FileProcessor</var> because testing
 * indicates that reading from streams is significantly faster.</p>
 *
 * <p>This processor does not respect the
 * {@link Processor#PRESERVE_METADATA_CONFIG_KEY} setting because telling IM
 * not to preserve metadata means telling it not to preserve an ICC profile.
 * Therefore, metadata always passes through.</p>
 */
class ImageMagickProcessor extends AbstractMagickProcessor implements StreamProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(ImageMagickProcessor.class);

    static final String BACKGROUND_COLOR_CONFIG_KEY =
            "ImageMagickProcessor.background_color";
    static final String NORMALIZE_CONFIG_KEY = "ImageMagickProcessor.normalize";
    static final String PATH_TO_BINARIES_CONFIG_KEY =
            "ImageMagickProcessor.path_to_binaries";
    static final String SHARPEN_CONFIG_KEY = "ImageMagickProcessor.sharpen";

    // ImageMagick 7 uses a `magick` command. Earlier versions use `convert`
    // and `identify`.
    private static AtomicBoolean isUsingVersion7;
    private static final Object lock = new Object();

    // Lazy-initialized by getFormats()
    protected static HashMap<Format, Set<Format>> supportedFormats;

    static {
        // Tell ProcessStarter where to find the binaries.
        final Configuration config = ConfigurationFactory.getInstance();
        final String path = config.getString(PATH_TO_BINARIES_CONFIG_KEY);
        if (path != null && path.length() > 0) {
            ProcessStarter.setGlobalSearchPath(path);
        }
    }

    /**
     * <p>Checks for ImageMagick 7 by attempting to invoke the `magick`
     * command. If the invocation fails, we assume that we are using version
     * <= 6.</p>
     *
     * <p>The result is cached.</p>
     *
     * @return Whether we appear to be using ImageMagick 7.
     */
    private static boolean isUsingVersion7() {
        if (isUsingVersion7 == null) {
            synchronized (lock) {
                final ProcessBuilder pb = new ProcessBuilder();
                final List<String> command = new ArrayList<>();
                command.add(getPath("magick"));
                pb.command(command);
                try {
                    isUsingVersion7 = new AtomicBoolean(false);
                    final String commandString = StringUtils.join(pb.command(), " ");
                    logger.debug("isUsingVersion7(): trying to invoke {}",
                            commandString);
                    final Process process = pb.start();
                    process.waitFor();
                    logger.info("isUsingVersion7(): found magick command; " +
                            "assuming ImageMagick 7+");
                    isUsingVersion7.set(true);
                } catch (Exception e) {
                    logger.info("isUsingVersion7(): couldn't find magick " +
                            "command; assuming ImageMagick <7");
                    isUsingVersion7.set(false);
                }
            }
        }
        return isUsingVersion7.get();
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        Set<Format> formats = getFormats().get(format);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
    }

    private List<String> getConvertArguments(final OperationList ops,
                                             final Dimension fullSize) {
        final List<String> args = new ArrayList<>();

        if (isUsingVersion7()) {
            args.add("magick");
        }
        args.add("convert");
        args.add(format.getPreferredExtension() + ":-"); // read from stdin

        // Normalization needs to happen before cropping to maintain the
        // intensity of cropped regions relative to the full image.
        final Configuration config = ConfigurationFactory.getInstance();
        if (config.getBoolean(NORMALIZE_CONFIG_KEY, false)) {
            args.add("-normalize");
        }

        for (Operation op : ops) {
            if (op instanceof Crop) {
                Crop crop = (Crop) op;
                if (crop.hasEffect(fullSize, ops)) {
                    args.add("-crop");
                    if (crop.getShape().equals(Crop.Shape.SQUARE)) {
                        final int shortestSide =
                                Math.min(fullSize.width, fullSize.height);
                        final int x = (fullSize.width - shortestSide) / 2;
                        final int y = (fullSize.height - shortestSide) / 2;
                        final String string = String.format("%dx%d+%d+%d",
                                shortestSide, shortestSide, x, y);
                        args.add(string);
                    } else if (crop.getUnit().equals(Crop.Unit.PERCENT)) {
                        // IM doesn't support cropping x/y by percentage
                        // (only width/height), so we have to calculate them.
                        final int x = Math.round(crop.getX() * fullSize.width);
                        final int y = Math.round(crop.getY() * fullSize.height);
                        final int width = Math.round(crop.getWidth() * 100);
                        final int height = Math.round(crop.getHeight() * 100);
                        final String string = String.format("%dx%d+%d+%d%%",
                                width, height, x, y);
                        args.add(string);
                    } else {
                        final String string = String.format("%dx%d+%d+%d",
                                Math.round(crop.getWidth()),
                                Math.round(crop.getHeight()),
                                Math.round(crop.getX()),
                                Math.round(crop.getY()));
                        args.add(string);
                    }
                }
            } else if (op instanceof Scale) {
                Scale scale = (Scale) op;
                if (scale.hasEffect(fullSize, ops)) {
                    args.add("-resize");
                    if (scale.getPercent() != null) {
                        final String arg = (scale.getPercent() * 100) + "%";
                        args.add(arg);
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        args.add(scale.getWidth().toString());
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        args.add(scale.getHeight().toString());
                    } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                        final String arg = String.format("%dx%d!",
                                scale.getWidth(), scale.getHeight());
                        args.add(arg);
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        final String arg = String.format("%dx%d",
                                scale.getWidth(), scale.getHeight());
                        args.add(arg);
                    }
                }
            } else if (op instanceof Transpose) {
                switch ((Transpose) op) {
                    case HORIZONTAL:
                        args.add("-flop");
                        break;
                    case VERTICAL:
                        args.add("-flip");
                        break;
                }
            } else if (op instanceof Rotate) {
                final Rotate rotate = (Rotate) op;
                if (rotate.hasEffect(fullSize, ops)) {
                    // If the output format supports transparency, make the
                    // background transparent. Otherwise, use a
                    // user-configurable background color.
                    args.add("-background");
                    if (ops.getOutputFormat().supportsTransparency()) {
                        args.add("none");
                    } else {
                        args.add(config.getString(BACKGROUND_COLOR_CONFIG_KEY, "black"));
                    }
                    args.add("-rotate");
                    args.add(Double.toString(rotate.getDegrees()));
                }
            } else if (op instanceof Color) {
                switch ((Color) op) {
                    case GRAY:
                        args.add("-colorspace");
                        args.add("Gray");
                        break;
                    case BITONAL:
                        args.add("-monochrome");
                        break;
                }
            }
        }

        args.add("-depth");
        args.add("8");

        // Apply the sharpen operation, if present.
        final double sharpenValue = config.getDouble(SHARPEN_CONFIG_KEY, 0);
        if (sharpenValue > 0) {
            args.add("-unsharp");
            args.add(Double.toString(sharpenValue));
        }

        // Write to stdout.
        args.add(ops.getOutputFormat().getPreferredExtension() + ":-");

        return args;
    }

    @Override
    public ImageInfo readImageInfo() throws ProcessorException {
        try (InputStream inputStream = streamSource.newInputStream()) {
            final List<String> args = new ArrayList<>();
            if (isUsingVersion7()) {
                args.add("magick");
            }
            args.add("identify");
            args.add("-ping");
            args.add("-format");
            args.add("%w\n%h\n%r");
            args.add(format.getPreferredExtension() + ":-");

            ArrayListOutputConsumer consumer = new ArrayListOutputConsumer();

            final ProcessStarter cmd = new ProcessStarter();
            cmd.setInputProvider(new Pipe(inputStream, null));
            cmd.setOutputConsumer(consumer);
            logger.info("readImageInfo(): invoking {}",
                    StringUtils.join(args, " ").replace("\n", ","));
            cmd.run(args);

            final ArrayList<String> output = consumer.getOutput();
            final int width = Integer.parseInt(output.get(0));
            final int height = Integer.parseInt(output.get(1));
            return new ImageInfo(width, height, width, height,
                    getSourceFormat());
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * @param binaryName Name of an executable
     * @return
     */
    private static String getPath(String binaryName) {
        String path = ConfigurationFactory.getInstance().
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
     * based on information reported by <code>identify -list format</code>.
     */
    private static HashMap<Format, Set<Format>> getFormats() {
        if (supportedFormats == null) {
            final Set<Format> formats = new HashSet<>();
            final Set<Format> outputFormats = new HashSet<>();

            // Retrieve the output of the `identify -list format` command,
            // which contains a list of all supported formats.
            final ProcessBuilder pb = new ProcessBuilder();
            final List<String> command = new ArrayList<>();
            if (isUsingVersion7()) {
                command.add(getPath("magick"));
                command.add("identify");
            } else {
                command.add(getPath("identify"));
            }
            command.add("-list");
            command.add("format");
            pb.command(command);
            final String commandString = StringUtils.join(pb.command(), " ");

            try {
                logger.info("getFormats(): invoking {}", commandString);
                final Process process = pb.start();

                try (final InputStream processInputStream = process.getInputStream()) {
                    BufferedReader stdInput = new BufferedReader(
                            new InputStreamReader(processInputStream));
                    String s;
                    while ((s = stdInput.readLine()) != null) {
                        s = s.trim();
                        if (s.startsWith("BMP")) {
                            formats.add(Format.BMP);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.BMP);
                            }
                        } else if (s.startsWith("DCM")) {
                            formats.add(Format.DCM);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.DCM);
                            }
                        } else if (s.startsWith("GIF")) {
                            formats.add(Format.GIF);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.GIF);
                            }
                        } else if (s.startsWith("JP2")) {
                            formats.add(Format.JP2);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.JP2);
                            }
                        } else if (s.startsWith("JPEG")) {
                            formats.add(Format.JPG);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.JPG);
                            }
                        } else if (s.startsWith("PNG")) {
                            formats.add(Format.PNG);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.PNG);
                            }
                        } else if (s.startsWith("PDF") && s.contains(" rw")) {
                            outputFormats.add(Format.PDF);
                        } else if (s.startsWith("TIFF")) {
                            formats.add(Format.TIF);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.TIF);
                            }
                        } else if (s.startsWith("WEBP")) {
                            formats.add(Format.WEBP);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.WEBP);
                            }
                        }
                    }
                    process.waitFor();
                } catch (InterruptedException e) {
                    logger.error("getFormats(): {}", e.getMessage());
                }
            } catch (IOException e) {
                logger.error("getFormats(): {}", e.getMessage());
            }

            supportedFormats = new HashMap<>();
            for (Format format : formats) {
                supportedFormats.put(format, outputFormats);
            }
        }
        return supportedFormats;
    }

    @Override
    public void process(final OperationList ops,
                        final ImageInfo imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        if (!getAvailableOutputFormats().contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try (InputStream inputStream = streamSource.newInputStream()) {
            final List<String> args = getConvertArguments(ops, imageInfo.getSize());
            final ProcessStarter cmd = new ProcessStarter();
            cmd.setInputProvider(new Pipe(inputStream, null));
            cmd.setOutputConsumer(new Pipe(null, outputStream));
            logger.info("process(): invoking {}", StringUtils.join(args, " "));
            cmd.run(args);
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

}
