package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Processor using the ImageMagick <code>magick</code> (version 7) or
 * <code>convert</code> and <code>identify</code> (earlier versions)
 * command-line tools.</p>
 *
 * <p>Implementation notes:</p>
 *
 * <ul>
 *     <li>{@link FileProcessor} is not implemented because testing indicates
 *     that reading from streams is significantly faster.</li>
 *     <li>This processor does not respect the
 *     {@link Key#PROCESSOR_PRESERVE_METADATA} setting because telling IM not
 *     to preserve metadata means telling it not to preserve an ICC profile.
 *     Therefore, metadata always passes through.</li>
 *     <li>This processor does not respect the
 *     {@link Key#PROCESSOR_RESPECT_ORIENTATION} setting. The orientation is
 *     always respected.</li>
 * </ul>
 */
class ImageMagickProcessor extends AbstractMagickProcessor
        implements StreamProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(ImageMagickProcessor.class);

    // ImageMagick 7 uses a `magick` command. Earlier versions use `convert`
    // and `identify`.
    private static AtomicBoolean isUsingVersion7;
    private static final Object lock = new Object();

    // Lazy-initialized by getFormats()
    protected static Map<Format, Set<Format>> supportedFormats;

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by <code>identify -list format</code>.
     */
    private static synchronized Map<Format, Set<Format>> getFormats() {
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
                final InputStream pis = process.getInputStream();
                final InputStreamReader reader = new InputStreamReader(pis);
                try (final BufferedReader buffReader = new BufferedReader(reader)) {
                    String s;
                    while ((s = buffReader.readLine()) != null) {
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
                            formats.add(Format.PDF);
                        } else if (s.startsWith("TIFF")) {
                            formats.add(Format.TIF);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.TIF);
                            }
                        } else if (s.startsWith("SGI") && s.contains("  r")) {
                            formats.add(Format.SGI);
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

    /**
     * @param binaryName Name of an executable.
     * @return
     */
    private static String getPath(final String binaryName) {
        String path = Configuration.getInstance().
                getString(Key.IMAGEMAGICKPROCESSOR_PATH_TO_BINARIES);
        if (path != null && path.length() > 0) {
            path = StringUtils.stripEnd(path, File.separator) + File.separator +
                    binaryName;
        } else {
            path = binaryName;
        }
        return path;
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
                                             final Info imageInfo) {
        final List<String> args = new ArrayList<>();

        if (isUsingVersion7()) {
            args.add(getPath("magick"));
            args.add("convert");
        } else {
            args.add(getPath("convert"));
        }
        args.add(format.getPreferredExtension() + ":-"); // read from stdin

        // Normalization needs to happen before cropping to maintain the
        // intensity of cropped regions relative to the full image.
        final boolean normalize = (boolean) ops.getOptions().
                getOrDefault(Key.PROCESSOR_NORMALIZE.key(), false);
        if (normalize) {
            args.add("-normalize");
        }

        Encode encode = (Encode) ops.getFirst(Encode.class);

        // If the output format supports transparency, make the background
        // transparent. Otherwise, use a user-configurable background color.
        if (ops.getOutputFormat().supportsTransparency()) {
            args.add("-background");
            args.add("none");
        } else {
            if (encode != null) {
                final Color bgColor = encode.getBackgroundColor();
                if (bgColor != null) {
                    args.add("-background");
                    args.add(bgColor.toRGBHex());
                }
            }
        }

        final Dimension fullSize = imageInfo.getSize();

        for (Operation op : ops) {
            if (op instanceof Crop) {
                Crop crop = (Crop) op;
                crop.applyOrientation(imageInfo.getOrientation(), fullSize);
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
                    final Scale.Filter scaleFilter = scale.getFilter();
                    if (scaleFilter != null) {
                        final String imFilter = getIMFilter(scaleFilter);
                        if (imFilter != null) {
                            args.add("-filter");
                            args.add(imFilter);
                        }
                    }

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
                    args.add("-rotate");
                    args.add(Double.toString(rotate.getDegrees()));
                }
            } else if (op instanceof ColorTransform) {
                switch ((ColorTransform) op) {
                    case GRAY:
                        args.add("-colorspace");
                        args.add("Gray");
                        break;
                    case BITONAL:
                        args.add("-monochrome");
                        break;
                }
            } else if (op instanceof Sharpen) {
                if (op.hasEffect(fullSize, ops)) {
                    args.add("-unsharp");
                    args.add(Double.toString(((Sharpen) op).getAmount()));
                }
            } else if (op instanceof Encode) {
                encode = (Encode) op;
                switch (encode.getFormat()) {
                    case JPG:
                        // Quality
                        final int jpgQuality = encode.getQuality();
                        args.add("-quality");
                        args.add(String.format("%d%%", jpgQuality));
                        // Interlace
                        if (encode.isInterlacing()) {
                            args.add("-interlace");
                            args.add("Plane");
                        }
                        break;
                    case TIF:
                        // Compression
                        final Compression compression = encode.getCompression();
                        args.add("-compress");
                        args.add(getIMTIFFCompression(compression));
                        break;
                }
            }
        }

        args.add("-depth");
        args.add("8");

        // Write to stdout.
        args.add(encode.getFormat().getPreferredExtension() + ":-");

        return args;
    }

    /**
     * @param filter
     * @return String suitable for passing to convert's <code>-filter</code>
     *         argument, or <code>null</code> if an equivalent is unknown.
     */
    private String getIMFilter(Scale.Filter filter) {
        // http://www.imagemagick.org/Usage/filter/
        switch (filter) {
            case BELL:
                return "hamming";
            case BICUBIC:
                return "catrom";
            case BOX:
                return "box";
            case BSPLINE:
                return "spline";
            case HERMITE:
                return "hermite";
            case LANCZOS3:
                return "lanczos";
            case MITCHELL:
                return "mitchell";
            case TRIANGLE:
                return "triangle";
        }
        return null;
    }

    /**
     * @param compression May be <code>null</code>.
     * @return String suitable for passing to convert's <code>-compress</code>
     *         argument.
     */
    private String getIMTIFFCompression(Compression compression) {
        if (compression != null) {
            switch (compression) {
                case LZW:
                    return "LZW";
                case DEFLATE:
                    return "Zip";
                case JPEG:
                    return "JPEG";
                case RLE:
                    return "RLE";
            }
        }
        return "None";
    }

    @Override
    public void process(final OperationList ops,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(ops, imageInfo, outputStream);

        try (InputStream inputStream = streamSource.newInputStream()) {
            final List<String> args = getConvertArguments(ops, imageInfo);
            final ProcessStarter cmd = new ProcessStarter();
            cmd.setInputProvider(new Pipe(inputStream, null));
            cmd.setOutputConsumer(new Pipe(null, outputStream));
            logger.info("process(): invoking {}", StringUtils.join(args, " "));
            cmd.run(args);
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public Info readImageInfo() throws ProcessorException {
        try (InputStream inputStream = streamSource.newInputStream()) {
            final List<String> args = new ArrayList<>();
            if (isUsingVersion7()) {
                args.add(getPath("magick"));
                args.add("identify");
            } else {
                args.add(getPath("identify"));
            }
            args.add("-ping");
            args.add("-format");
            // We need to read this even when not respecting orientation,
            // because GM's crop operation is orientation-unaware.
            args.add("%w\n%h\n%[EXIF:Orientation]");
            args.add(format.getPreferredExtension() + ":-");

            final ArrayListOutputConsumer consumer =
                    new ArrayListOutputConsumer();

            final ProcessStarter cmd = new ProcessStarter();
            cmd.setInputProvider(new Pipe(inputStream, null));
            cmd.setOutputConsumer(consumer);
            logger.info("readImageInfo(): invoking {}",
                    StringUtils.join(args, " ").replace("\n", ","));
            cmd.run(args);

            final List<String> output = consumer.getOutput();
            final int width = Integer.parseInt(output.get(0));
            final int height = Integer.parseInt(output.get(1));
            // GM is not tile-aware, so set the tile size to the full
            // dimensions.
            final Info info = new Info(width, height, width, height,
                    getSourceFormat());
            // Do we have an EXIF orientation to deal with?
            if (output.size() > 2) {
                try {
                    final int exifOrientation = Integer.parseInt(output.get(2));
                    final Orientation orientation =
                            Orientation.forEXIFOrientation(exifOrientation);
                    info.getImages().get(0).setOrientation(orientation);
                } catch (IllegalArgumentException e) {
                    // whatever
                }
            }
            return info;
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

}
