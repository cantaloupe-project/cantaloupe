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
import edu.illinois.library.cantaloupe.operation.Normalize;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.operation.overlay.ImageOverlay;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.overlay.Position;
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final Logger logger = LoggerFactory.
            getLogger(ImageMagickProcessor.class);

    static final String OVERLAY_TEMP_FILE_PREFIX = "cantaloupe-overlay";

    private static InitializationException initializationException;
    private static boolean isInitialized = false;

    /** ImageMagick 7 uses a `magick` command. Earlier versions use `convert`
    and `identify`. */
    private static AtomicBoolean isUsingVersion7;

    /** Map of overlay images downloaded from web servers. Files are temp files
    set to delete-on-exit. */
    private static Map<URL,File> overlays = new ConcurrentHashMap<>();

    /** Lazy-initialized by getFormats(). */
    protected static Map<Format, Set<Format>> supportedFormats;

    /**
     * Performs one-time class-level/shared initialization.
     */
    private static synchronized void initialize() {
        if (!isInitialized) {
            getFormats();
            isInitialized = true;
        }
    }

    /**
     * For testing purposes only.
     */
    static synchronized void resetInitialization() {
        supportedFormats = null;
        isInitialized = false;
    }

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

                    supportedFormats = new HashMap<>();
                    for (Format format : formats) {
                        supportedFormats.put(format, outputFormats);
                    }
                } catch (InterruptedException e) {
                    initializationException = new InitializationException(e);
                    // This is safe to swallow.
                }
            } catch (IOException e) {
                initializationException = new InitializationException(e);
                // This is safe to swallow.
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
    static synchronized boolean isUsingVersion7() {
        if (isUsingVersion7 == null) {
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
                logger.warn("ImageMagick <7 support is DEPRECATED. " +
                        "Please upgrade to version 7.");
                isUsingVersion7.set(false);
            }
        }
        return isUsingVersion7.get();
    }

    /**
     * For testing only.
     */
    static synchronized void setUsingVersion7(boolean trueOrFalse) {
        isUsingVersion7.set(trueOrFalse);
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

        // If we need to rasterize, and the op list contains a scale operation,
        // see if we can use it to compute a scale-appropriate DPI.
        // This needs to be done before the source argument is added.
        if (Format.ImageType.VECTOR.equals(imageInfo.getSourceFormat().getImageType())) {
            Scale scale = (Scale) ops.getFirst(Scale.class);
            if (scale == null) {
                scale = new Scale();
            }
            args.add("-density");
            args.add("" + new RasterizationHelper().getDPI(scale,
                    imageInfo.getSize()));
        }

        int pageIndex = getIMImageIndex(
                (String) ops.getOptions().get("page"),
                imageInfo.getSourceFormat());

        // :- = read from stdin
        args.add(format.getPreferredExtension() + ":-[" + pageIndex + "]");

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
            if (op instanceof Normalize) {
                args.add("-normalize");
            } else if (op instanceof Crop) {
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
            } else if (op instanceof ImageOverlay) {
                try {
                    final ImageOverlay overlay = (ImageOverlay) op;
                    // If the overlay is a file, use that.
                    File file = overlay.getFile();
                    // If it instead resides at a URL, use that instead.
                    if (file == null && overlay.getURL() != null) {
                        file = getOverlayTempFile(overlay);
                    }
                    if (file != null) {
                        args.add(file.getAbsolutePath());
                        args.add("-compose");
                        args.add("over");
                        args.add("-gravity");
                        args.add(getIMOverlayGravity(overlay.getPosition()));
                        args.add("-geometry");
                        args.add(getIMOverlayGeometry(overlay));
                        args.add("-composite");
                    } else {
                        if (overlay.getFile() != null) {
                            logger.warn("getConvertArguments(): overlay not found: {}",
                                    overlay.getFile());
                        } else if (overlay.getURL() != null) {
                            logger.warn("getConvertArguments(): overlay not found: {}",
                                    overlay.getURL());
                        } else {
                            logger.error("getConvertArguments(): overlay source not set");
                        }
                    }
                } catch (IOException e) {
                    logger.error("getConvertArguments(): overlay error: {}",
                            e.getMessage());
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
     * @param pageStr Client-provided page number.
     * @param sourceFormat Format of the source image.
     * @return ImageMagick image index argument.
     */
    private int getIMImageIndex(String pageStr, Format sourceFormat) {
        int index = 0;
        if (pageStr != null && Format.PDF.equals(sourceFormat)) {
            try {
                index = Integer.parseInt(pageStr) - 1;
            } catch (NumberFormatException e) {
                logger.info("Page number from URI query string is not " +
                        "an integer; using page 1.");
            }
            index = Math.max(index, 0);
        }
        return index;
    }

    String getIMOverlayGeometry(Overlay overlay) {
        int x = 0, y = 0;
        switch (overlay.getPosition()) {
            case TOP_LEFT:
                x += overlay.getInset();
                y += overlay.getInset();
                break;
            case TOP_CENTER:
                y += overlay.getInset();
                break;
            case TOP_RIGHT:
                x += overlay.getInset();
                y += overlay.getInset();
                break;
            case LEFT_CENTER:
                x += overlay.getInset();
                break;
            case CENTER:
                // noop
                break;
            case RIGHT_CENTER:
                x += overlay.getInset();
                break;
            case BOTTOM_LEFT:
                x += overlay.getInset();
                y += overlay.getInset();
                break;
            case BOTTOM_CENTER:
                y += overlay.getInset();
                break;
            case BOTTOM_RIGHT:
                x += overlay.getInset();
                y += overlay.getInset();
                break;
        }
        String xStr = (x > -1) ? "+" + x : "" + x;
        String yStr = (y > -1) ? "+" + y : "" + y;
        return xStr + yStr;
    }

    String getIMOverlayGravity(Position position) {
        switch (position) {
            case TOP_LEFT:
                return "northwest";
            case TOP_CENTER:
                return "north";
            case TOP_RIGHT:
                return "northeast";
            case LEFT_CENTER:
                return "west";
            case RIGHT_CENTER:
                return "east";
            case BOTTOM_LEFT:
                return "southwest";
            case BOTTOM_CENTER:
                return "south";
            case BOTTOM_RIGHT:
                return "southeast";
            default:
                return "center";
        }
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
    public InitializationException getInitializationException() {
        initialize();
        return initializationException;
    }

    File getOverlayTempFile(ImageOverlay overlay) throws IOException {
        File overlayFile = null;
        final URL url = overlay.getURL();

        if (url != null) {
            // Try to retrieve it if it has already been downloaded.
            overlayFile = overlays.get(url);
            if (overlayFile == null) {
                // It doesn't exist, so download it.
                Path tempFile = Files.createTempFile(OVERLAY_TEMP_FILE_PREFIX, ".tmp");
                try (InputStream is = overlay.openStream()) {
                    Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    overlayFile = tempFile.toFile();
                    overlays.put(url, overlayFile);
                } finally {
                    if (overlayFile != null) {
                        overlayFile.deleteOnExit();
                    }
                }
            }
        }
        return overlayFile;
    }

    @Override
    public List<String> getWarnings() {
        List<String> warnings = new ArrayList<>();
        //if (!isUsingVersion7()) {
            warnings.add("Support for ImageMagick <7 will be removed in a " +
                    "future release. Please upgrade to version 7.");
        //}
        return warnings;
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

    @Override
    public void validate(OperationList opList, Dimension fullSize)
            throws ValidationException, ProcessorException {
        StreamProcessor.super.validate(opList, fullSize);

        // Check the format of the "page" option, if present.
        final String pageStr = (String) opList.getOptions().get("page");
        if (pageStr != null) {
            try {
                final int page = Integer.parseInt(pageStr);
                if (page < 1) {
                    throw new ValidationException(
                            "Page number is out-of-bounds.");
                }
            } catch (NumberFormatException e) {
                throw new ValidationException("Invalid page number.");
            }
        }
    }

}
