package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
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
import edu.illinois.library.cantaloupe.operation.overlay.ImageOverlay;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.overlay.Position;
import edu.illinois.library.cantaloupe.process.ArrayListOutputConsumer;
import edu.illinois.library.cantaloupe.process.Pipe;
import edu.illinois.library.cantaloupe.process.ProcessStarter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
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

    enum IMVersion {
        VERSION_PRE_7, VERSION_7
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageMagickProcessor.class);

    static final String OVERLAY_TEMP_FILE_PREFIX = Application.NAME + "-" +
            ImageMagickProcessor.class.getSimpleName() + "-overlay";

    private static final AtomicBoolean initializationAttempted =
            new AtomicBoolean(false);
    private static InitializationException initializationException;

    private static final AtomicBoolean checkedVersion =
            new AtomicBoolean(false);

    // ImageMagick 7 uses a `magick` command. Earlier versions use `convert`
    // and `identify`. IM7 may provide aliases for these.
    private static IMVersion imVersion;

    /** Map of overlay images downloaded from web servers. Files are temp files
    set to delete-on-exit. */
    private static final Map<URI,File> overlays = new ConcurrentHashMap<>();

    /** Lazy-initialized by readFormats(). */
    protected static final Map<Format, Set<Format>> supportedFormats =
            new HashMap<>();

    /**
     * <p>Checks the ImageMagick version by attempting to invoke the `magick`
     * command. If the invocation fails, we assume that we are using version
     * &le; 6.</p>
     *
     * <p>The result is cached.</p>
     *
     * @return ImageMagick version.
     */
    static synchronized IMVersion getIMVersion() {
        if (!checkedVersion.get()) {
            checkedVersion.set(true);

            // Search for the IM 7+ `magick` command
            final ProcessBuilder pb = new ProcessBuilder();
            List<String> command = new ArrayList<>();
            command.add(getPath("magick"));
            pb.command(command);
            try {
                final String commandString = StringUtils.join(pb.command(), " ");
                LOGGER.debug("getIMVersion(): trying to invoke {}",
                        commandString);
                final Process process = pb.start();
                process.waitFor();
                LOGGER.info("getIMVersion(): found magick command; assuming " +
                        "ImageMagick 7+");
                imVersion = IMVersion.VERSION_7;
            } catch (Exception e) {
                LOGGER.info("getIMVersion(): couldn't find magick command; " +
                        "checking for IM <7");

                // Search for the IM <7 `identify` command
                command = new ArrayList<>();
                command.add(getPath("identify"));
                pb.command(command);
                try {
                    final String commandString = StringUtils.join(pb.command(), " ");
                    LOGGER.debug("getIMVersion(): trying to invoke {}",
                            commandString);
                    final Process process = pb.start();
                    process.waitFor();
                    LOGGER.info("getIMVersion(): found identify command; " +
                            "assuming ImageMagick <7");
                    imVersion = IMVersion.VERSION_PRE_7;
                } catch (Exception e2) {
                    LOGGER.error("getIMVersion(): couldn't find an " +
                            "ImageMagick binary");
                }
            }
        }
        return imVersion;
    }

    /**
     * @param binaryName Name of an executable.
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
     * Performs one-time class-level/shared initialization.
     */
    private static synchronized void initialize() {
        initializationAttempted.set(true);
        readFormats();
    }

    /**
     * @return Map of available output formats for all known source formats,
     *         based on information reported by {@literal
     *         identify -list format}.
     */
    private static synchronized Map<Format, Set<Format>> readFormats() {
        if (supportedFormats.isEmpty()) {
            final Set<Format> sourceFormats = EnumSet.noneOf(Format.class);
            final Set<Format> outputFormats = EnumSet.noneOf(Format.class);

            final ProcessBuilder pb = new ProcessBuilder();
            final List<String> command = new ArrayList<>();
            try {
                IMVersion version = getIMVersion();
                if (version != null) {
                    switch (getIMVersion()) {
                        case VERSION_PRE_7:
                            command.add(getPath("identify"));
                            break;
                        default:
                            command.add(getPath("magick"));
                            command.add("identify");
                            break;
                    }
                } else {
                    throw new IOException("Can't find `magick` or `identify` binaries");
                }

                command.add("-list");
                command.add("format");
                pb.command(command);
                final String commandString = String.join(" ", pb.command());

                LOGGER.info("readFormats(): invoking {}", commandString);
                final Process process = pb.start();
                final InputStream pis = process.getInputStream();
                final InputStreamReader isReader =
                        new InputStreamReader(pis, "UTF-8");
                try (final BufferedReader bReader = new BufferedReader(isReader)) {
                    String s;
                    while ((s = bReader.readLine()) != null) {
                        s = s.trim();
                        if (s.startsWith("BMP")) {
                            sourceFormats.add(Format.BMP);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.BMP);
                            }
                        } else if (s.startsWith("DCM")) {
                            sourceFormats.add(Format.DCM);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.DCM);
                            }
                        } else if (s.startsWith("GIF")) {
                            sourceFormats.add(Format.GIF);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.GIF);
                            }
                        } else if (s.startsWith("JP2")) {
                            sourceFormats.add(Format.JP2);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.JP2);
                            }
                        } else if (s.startsWith("JPEG")) {
                            sourceFormats.add(Format.JPG);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.JPG);
                            }
                        } else if (s.startsWith("PDF") && s.contains("  r")) {
                            sourceFormats.add(Format.PDF);
                        } else if (s.startsWith("PNG")) {
                            sourceFormats.add(Format.PNG);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.PNG);
                            }
                        } else if (s.startsWith("TIFF")) {
                            sourceFormats.add(Format.TIF);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.TIF);
                            }
                        } else if (s.startsWith("WEBP")) {
                            sourceFormats.add(Format.WEBP);
                            if (s.contains(" rw")) {
                                outputFormats.add(Format.WEBP);
                            }
                        }
                    }
                    process.waitFor();

                    for (Format format : sourceFormats) {
                        supportedFormats.put(format, outputFormats);
                    }
                }
            } catch (IOException | InterruptedException e) {
                initializationException = new InitializationException(e);
                // This is safe to swallow.
            }
        }
        return supportedFormats;
    }

    /**
     * For testing only!
     */
    static synchronized void resetInitialization() {
        initializationAttempted.set(false);
        initializationException = null;
        supportedFormats.clear();
        checkedVersion.set(false);
        imVersion = null;
    }

    /**
     * For testing only!
     */
    static synchronized void setIMVersion(IMVersion version) {
        imVersion = version;
        checkedVersion.set(true);
    }

    ImageMagickProcessor() {
        if (!initializationAttempted.get()) {
            initialize();
        }
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        Set<Format> formats = readFormats().get(format);
        if (formats == null) {
            formats = Collections.unmodifiableSet(Collections.emptySet());
        }
        return formats;
    }

    private List<String> getConvertArguments(final OperationList ops,
                                             final Info imageInfo) {
        final List<String> args = new ArrayList<>();

        if (IMVersion.VERSION_7.equals(getIMVersion())) {
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
                        args.add((scale.getPercent() * 100) + "%");
                    } else {
                        switch (scale.getMode()) {
                            case ASPECT_FIT_WIDTH:
                                args.add(scale.getWidth().toString() + "x");
                                break;
                            case ASPECT_FIT_HEIGHT:
                                args.add("x" + scale.getHeight().toString());
                                break;
                            case NON_ASPECT_FILL:
                                String arg = String.format("%dx%d!",
                                        scale.getWidth(), scale.getHeight());
                                args.add(arg);
                                break;
                            case ASPECT_FIT_INSIDE:
                                arg = String.format("%dx%d",
                                        scale.getWidth(), scale.getHeight());
                                args.add(arg);
                                break;
                        }
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
                    File file = getOverlayTempFile(overlay);
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
                        if (overlay.getURI() != null) {
                            LOGGER.warn("getConvertArguments(): overlay not found: {}",
                                    overlay.getURI());
                        } else {
                            LOGGER.error("getConvertArguments(): overlay source not set");
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("getConvertArguments(): overlay error: {}",
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
     * @return String suitable for passing to convert's {@literal -filter}
     *         argument, or {@literal null} if an equivalent is unknown.
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
     * @param pageStr      Client-provided page number.
     * @param sourceFormat Format of the source image.
     * @return             ImageMagick image index argument.
     */
    private int getIMImageIndex(String pageStr, Format sourceFormat) {
        int index = 0;
        if (pageStr != null && Format.PDF.equals(sourceFormat)) {
            try {
                index = Integer.parseInt(pageStr) - 1;
            } catch (NumberFormatException e) {
                LOGGER.info("Page number from URI query string is not " +
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
     * @param compression May be {@literal null}.
     * @return            String suitable for passing to {@literal convert}'s
     *                    {@literal -compress} argument.
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
        final URI url = overlay.getURI();

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
        final List<String> warnings = new ArrayList<>();
        if (IMVersion.VERSION_PRE_7.equals(getIMVersion())) {
            warnings.add("Support for ImageMagick <7 will be removed in a " +
                    "future release. Please upgrade to version 7.");
        }
        return Collections.unmodifiableList(warnings);
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
            LOGGER.info("process(): invoking {}", String.join(" ", args));
            cmd.run(args);
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public Info readImageInfo() throws IOException {
        try (InputStream inputStream = streamSource.newInputStream()) {
            final List<String> args = new ArrayList<>();
            if (IMVersion.VERSION_7.equals(getIMVersion())) {
                args.add(getPath("magick"));
                args.add("identify");
            } else {
                args.add(getPath("identify"));
            }
            args.add("-ping");
            args.add("-format");
            // N.B. 1: We need to read this even when not respecting
            // orientation, because GM's crop operation is orientation-unaware.
            // N.B. 2: IM (7.0.6-7) seems to have some kind of issue with
            // retrieving EXIF tags by name. The glob works around it. This
            // should be OK, as I don't think there are any other EXIF tags
            // that would match this. Another benefit of the glob is that it
            // suppresses an "unknown image property" warning when the source
            // image has no Orientation tag.
            args.add("%w\n%h\n%[EXIF:*Orientation]");
            args.add(format.getPreferredExtension() + ":-");

            final ArrayListOutputConsumer consumer =
                    new ArrayListOutputConsumer();

            final ProcessStarter cmd = new ProcessStarter();
            cmd.setInputProvider(new Pipe(inputStream, null));
            cmd.setOutputConsumer(consumer);
            final String cmdString = String.join(" ", args).replace("\n", ",");
            LOGGER.info("readImageInfo(): invoking {}", cmdString);
            cmd.run(args);

            final List<String> output = consumer.getOutput();
            if (output.size() > 0) {
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
                        LOGGER.warn("readImageInfo(): {}", e.getMessage());
                    }
                }
                return info;
            }
            throw new IOException("readImageInfo(): nothing received on " +
                    "stdout from command: " + cmdString);
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    @Override
    public void validate(OperationList opList,
                         Dimension fullSize) throws ProcessorException {
        StreamProcessor.super.validate(opList, fullSize);

        // Check the format of the "page" option, if present.
        // TODO: move this to OperationList.validate()
        final String pageStr = (String) opList.getOptions().get("page");
        if (pageStr != null) {
            try {
                final int page = Integer.parseInt(pageStr);
                if (page < 1) {
                    throw new IllegalArgumentException(
                            "Page number is out-of-bounds.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid page number.");
            }
        }
    }

}
