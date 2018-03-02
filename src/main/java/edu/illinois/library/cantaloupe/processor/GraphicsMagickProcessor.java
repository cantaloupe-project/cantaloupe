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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Processor using the GraphicsMagick <code>gm</code> command-line tool.
 * Tested with version 1.3.21; other versions may or may not work.</p>
 *
 * <p>Implementation notes:</p>
 *
 * <ul>
 *     <li>{@link FileProcessor} is not implemented because testing indicates
 *     that reading from streams is significantly faster.</li>
 *     <li>This processor does not respect the
 *     {@link Key#PROCESSOR_PRESERVE_METADATA} setting because telling GM not
 *     to preserve metadata means telling it not to preserve an ICC profile.
 *     Therefore, metadata always passes through.</li>
 *     <li>This processor does not respect the
 *     {@link Key#PROCESSOR_RESPECT_ORIENTATION} setting. The orientation is
 *     always respected.</li>
 * </ul>
 */
class GraphicsMagickProcessor extends AbstractMagickProcessor
        implements StreamProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GraphicsMagickProcessor.class);

    private static final String BINARY_NAME = "gm";

    private static final AtomicBoolean initializationAttempted =
            new AtomicBoolean(false);
    private static InitializationException initializationException;

    /**
     * Initialized by {@link #readFormats()}.
     */
    private static final Map<Format, Set<Format>> supportedFormats =
            new HashMap<>();

    private static String getPath() {
        String path = Configuration.getInstance().
                getString(Key.GRAPHICSMAGICKPROCESSOR_PATH_TO_BINARIES);
        if (path != null && path.length() > 0) {
            path = StringUtils.stripEnd(path, File.separator) + File.separator +
                    BINARY_NAME;
        } else {
            path = BINARY_NAME;
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
     *         based on information reported by {@literal gm version}. The
     *         result is cached.
     */
    private static synchronized Map<Format, Set<Format>> readFormats() {
        if (supportedFormats.isEmpty()) {
            final Set<Format> sourceFormats = EnumSet.noneOf(Format.class);
            final Set<Format> outputFormats = EnumSet.noneOf(Format.class);

            // Get the output of the `gm version` command, which contains
            // a list of all optional formats.
            final ProcessBuilder pb = new ProcessBuilder();
            final List<String> command = new ArrayList<>();
            command.add(getPath());
            command.add("version");
            pb.command(command);
            final String commandString = String.join(" ", pb.command());

            try {
                LOGGER.info("readFormats(): invoking {}", commandString);
                final Process process = pb.start();

                final InputStream processInputStream = process.getInputStream();
                try (BufferedReader bReader = new BufferedReader(
                        new InputStreamReader(processInputStream, "UTF-8"))) {
                    String s;
                    boolean read = false;
                    while ((s = bReader.readLine()) != null) {
                        if (s.contains("Feature Support")) {
                            read = true; // start reading
                        } else if (s.contains("Host type:")) {
                            break; // stop reading
                        }
                        if (read) {
                            s = s.trim();
                            if (s.startsWith("JPEG-2000 ") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.JP2);
                                outputFormats.add(Format.JP2);
                            } else if (s.startsWith("JPEG ") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.JPG);
                                outputFormats.add(Format.JPG);
                            } else if (s.startsWith("PNG ") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.PNG);
                                outputFormats.add(Format.PNG);
                            } else if (s.startsWith("Ghostscript") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.PDF);
                            } else if (s.startsWith("TIFF ") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.TIF);
                                outputFormats.add(Format.TIF);
                            } else if (s.startsWith("WebP ") && s.endsWith(" yes")) {
                                sourceFormats.add(Format.WEBP);
                                outputFormats.add(Format.WEBP);
                            }
                        }
                    }
                    process.waitFor();

                    // Add formats that are not listed in the output of
                    // "gm version" but are definitely available
                    // (http://www.graphicsmagick.org/formats.html)
                    sourceFormats.add(Format.BMP);
                    sourceFormats.add(Format.DCM);
                    sourceFormats.add(Format.GIF);
                    outputFormats.add(Format.GIF);

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
    }

    GraphicsMagickProcessor() {
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
        final List<String> args = new ArrayList<>(30);
        args.add(getPath());
        args.add("convert");

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

        int pageIndex = getGMImageIndex(
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
                        // GM doesn't support cropping x/y by percentage
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
                        final String gmFilter = getGMFilter(scaleFilter);
                        if (gmFilter != null) {
                            args.add("-filter");
                            args.add(gmFilter);
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
                        args.add(getGMTIFFCompression(compression));
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
     * @return String suitable for passing to {@literal gm convert}'s
     *         {@literal -filter} argument, or {@literal null} if an equivalent
     *         is unknown.
     */
    private String getGMFilter(Scale.Filter filter) {
        // http://www.graphicsmagick.org/GraphicsMagick.html#details-filter
        switch (filter) {
            case BELL:
                return "hamming";
            case BICUBIC:
                return "catrom";
            case BOX:
                return "box";
            case BSPLINE:
                return "gaussian";
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
     * @return             GraphicsMagick image index argument.
     */
    private int getGMImageIndex(String pageStr, Format sourceFormat) {
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

    /**
     * @param compression May be {@literal null}.
     * @return            String suitable for passing to {@literal
     *                    gm convert}'s {@literal -compress} argument.
     */
    private String getGMTIFFCompression(Compression compression) {
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
        if (!initializationAttempted.get()) {
            initialize();
        }
        return initializationException;
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
            args.add(getPath());
            args.add("identify");
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
            LOGGER.info("readImageInfo(): invoking {}",
                    String.join(" ", args).replace("\n", ""));
            cmd.run(args);

            final List<String> output = consumer.getOutput();
            final int width = Integer.parseInt(output.get(0));
            final int height = Integer.parseInt(output.get(1));
            // GM is not tile-aware, so set the tile size to the full
            // dimensions.
            final Info info = Info.builder()
                    .withSize(width, height)
                    .withTileSize(width, height)
                    .withFormat(getSourceFormat())
                    .build();
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
