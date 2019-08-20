package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Normalize;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.util.CommandLocator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Processor using the Kakadu {@literal kdu_expand} command-line tool.
 * Written against version 7.7, but should work with other versions, as long as
 * their command-line interface is compatible.</p>
 *
 * <p>{@literal kdu_expand} is used for cropping and an initial scale reduction
 * factor, and Java 2D for differential scaling and all other remaining
 * processing steps. It generates TIFF output which is streamed (with some
 * buffering) to an ImageIO reader. (TIFF is used in order to preserve
 * the ICC profiles that {@literal kdu_expand} embeds in the files it
 * generates.)</p>
 *
 * <p>{@literal kdu_expand} reads and writes the files named in the {@literal
 * -i} and {@literal -o} flags passed to it, respectively. The file in the
 * {@literal -o} flag must have a recognized image extension such as {@literal
 * .bmp}, {@literal .tif}, etc. This means that it's not possible to natively
 * write into an {@link InputStream} from a {@link Process}. The way this is
 * dealt with differs between Windows and Unix:</p>
 *
 * <dl>
 *     <dt>Unix</dt>
 *     <dd>A symlink is created by {@link #initialize()} from {@literal
 *     /tmp/whatever.tif} to {@literal /dev/stdout}, and set to delete on exit.
 *     {@literal kdu_expand} effectively writes to standard output, which can
 *     be read from a {@link Process#getInputStream() process' input
 *     stream}.</dd>
 *     <dt>Windows</dt>
 *     <dd>Windows doesn't have anything like {@literal /dev/stdout}&mdash;
 *     actually it has {@literal CON}, but experimentation reveals it won't
 *     work&mdash;so {@literal kdu_expand} instead writes to a temporary file
 *     which is deleted after being read. This is slower than the Unix
 *     technique.</dd>
 * </dl>
 *
 * <p>Although {@literal kdu_expand} is used for reading images,
 * {@literal kdu_jp2info} is <strong>not</strong> used for reading metadata.
 * The ImageIO JPEG2000 plugin is used instead, as this doesn't require
 * invoking another process and is therefore more efficient.</p>
 *
 * @see <a href="http://kakadusoftware.com/wp-content/uploads/2014/06/Usage_Examples-v7_7.txt">
 *     Usage Examples for the Demonstration Applications Supplied with Kakadu
 *     V7.7</a>
 */
class KakaduDemoProcessor extends AbstractJava2DProcessor implements FileProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KakaduDemoProcessor.class);

    /**
     * Number of decomposition levels assumed to be contained in the image when
     * that information cannot be obtained for some reason. 5 is the default
     * used by most JP2 encoders. Setting this to a value higher than that could
     * cause decoding errors, and setting it to lower could have a performance
     * cost.
     */
    private static final short FALLBACK_NUM_DWT_LEVELS = 5;

    private static final String KDU_EXPAND_NAME = "kdu_expand";

    /**
     * Used only in Windows.
     */
    private static final String WINDOWS_SCRATCH_DIR_NAME =
            KakaduDemoProcessor.class.getSimpleName() + "-scratch";

    /**
     * Set by {@link #initialize()}.
     */
    private static final AtomicBoolean initializationAttempted =
            new AtomicBoolean(false);

    /**
     * Set by {@link #initialize()}.
     */
    private static InitializationException initializationException;

    /**
     * Not used in Windows.
     */
    private static Path stdoutSymlink;

    /**
     * <p>Creates a symlink to {@literal /dev/stdout} in a temporary directory,
     * and sets it to delete on exit.</p>
     *
     * <p>Not used in Windows.</p>
     */
    private static void createStdoutSymlink() throws IOException {
        Path tempDir = Application.getTempPath();
        ///changed this to .bmp as .tif was throwing std:bad_alloc errors
        final String name = KakaduDemoProcessor.class.getSimpleName() + "-" +
                UUID.randomUUID() + ".bmp";
        final Path link = tempDir.resolve(name);
        final Path devStdout = Paths.get("/dev/stdout");

        stdoutSymlink = Files.createSymbolicLink(link, devStdout);
        stdoutSymlink.toFile().deleteOnExit();
    }

    private static String getPath() {
        String searchPath = Configuration.getInstance().
                getString(Key.KAKADUDEMOPROCESSOR_PATH_TO_BINARIES);
        return CommandLocator.locate(KDU_EXPAND_NAME, searchPath);
    }

    /**
     * Used only in Windows.
     *
     * @return Thread-safe path of an intermediate image from {@literal
     *         kdu_expand} based on the given operation list.
     */
    private static Path getIntermediateImageFile(OperationList opList) {
        final String name = opList.toFilename() + "-" +
                Thread.currentThread().getName() + ".bmp";
        return getScratchDir().resolve(name);
    }

    /**
     * Used only in Windows.
     *
     * @return Path to the scratch directory that stores output images from
     *         {@literal kdu_expand}.
     */
    private static Path getScratchDir() {
        Path tempPath = Application.getTempPath();
        return tempPath.resolve(WINDOWS_SCRATCH_DIR_NAME);
    }

    private static synchronized void initialize() {
        initializationAttempted.set(true);

        try {
            // Check for the presence of kdu_expand.
            invoke();

            if (isWindows()) {
                initializeForWindows();
            } else {
                initializeForUnix();
            }
        } catch (IOException e) {
            initializationException = new InitializationException(e);
        }
    }

    private static void initializeForWindows() throws IOException {
        final Path scratchDir = getScratchDir();

        if (!Files.exists(scratchDir)) {
            Files.createDirectories(scratchDir);
        }
    }

    private static void initializeForUnix() throws IOException {
        final Path devStdout = Paths.get("/dev", "stdout");
        if (Files.exists(devStdout) && Files.isWritable(devStdout)) {
            createStdoutSymlink();
        } else {
            LOGGER.error("/dev/stdout does not exist or isn't writable");
        }
    }

    private static void invoke() throws IOException {
        final ProcessBuilder pb = new ProcessBuilder();
        List<String> command = new ArrayList<>();
        command.add(getPath());
        pb.command(command);
        String commandString = String.join(" ", pb.command());
        LOGGER.info("invoke(): {}", commandString);
        pb.start();
    }

    private static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    /**
     * For testing only!
     */
    static synchronized void resetInitialization() {
        initializationAttempted.set(false);
        initializationException = null;
    }

    private static String toString(ByteArrayOutputStream os) {
        try {
            return new String(os.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
            return "";
        }
    }

    KakaduDemoProcessor() {
        if (!initializationAttempted.get()) {
            initialize();
        }
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        final Set<Format> outputFormats;
        if (Format.JP2.equals(sourceFormat)) {
            outputFormats = ImageWriterFactory.supportedFormats();
        } else {
            outputFormats = Collections.unmodifiableSet(Collections.emptySet());
        }
        return outputFormats;
    }

    /**
     * Computes the effective size of an image after all crop operations are
     * applied but excluding any scale operations, in order to use
     * {@literal kdu_expand}'s {@literal -reduce} argument.
     */
    private Dimension getCroppedSize(OperationList opList, Dimension fullSize) {
        Dimension tileSize = (Dimension) fullSize.clone();
        for (Operation op : opList) {
            if (op instanceof Crop) {
                tileSize = ((Crop) op).getRectangle(tileSize).getSize();
            }
        }
        return tileSize;
    }

    @Override
    public InitializationException getInitializationException() {
        if (!initializationAttempted.get()) {
            initialize();
        }
        return initializationException;
    }

    /**
     * <p>Reads image information using ImageIO.</p>
     *
     * <p>This override disposes the reader since it won't be used for anything
     * else.</p>
     */
    @Override
    public Info readImageInfo() throws IOException {
        Info info;
        try {
            info = super.readImageInfo();
        } finally {
            getReader().dispose();
        }
        return info;
    }

    @Override
    public void process(final OperationList opList,
                        final Info imageInfo,
                        final OutputStream outputStream) throws ProcessorException {
        super.process(opList, imageInfo, outputStream);

        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();

        try {
            if (isWindows()) {
                processInWindows(opList, imageInfo, errorBucket, outputStream);
            } else {
                processInUnix(opList, imageInfo, errorBucket, outputStream);
            }
        } catch (EOFException e) {
            // This is usually caused by the connection closing.
            String msg = e.getMessage();
            msg = String.format("process(): %s (%s)",
                    (msg != null && msg.length() > 0) ? msg : "EOFException",
                    opList.toString());
            LOGGER.debug(msg, e);
            throw new ProcessorException(msg, e);
        } catch (IOException | InterruptedException e) {
            String msg = e.getMessage();
            final String errorStr = toString(errorBucket);
            if (errorStr.length() > 0) {
                msg += " (command output: " + errorStr + ")";
            }
            throw new ProcessorException(msg, e);
        }
    }

    private void processInWindows(final OperationList opList,
                                  final Info info,
                                  final ByteArrayOutputStream errorOutput,
                                  final OutputStream outputStream)
            throws IOException, InterruptedException {
        // Will receive stdin output from kdu_expand (but we're not expecting
        // any).
        final ByteArrayOutputStream inputBucket = new ByteArrayOutputStream();
        final Path intermediateFile = getIntermediateImageFile(opList);
        final ReductionFactor reductionFactor = new ReductionFactor();
        final ThreadPool pool = ThreadPool.getInstance();

        // If we are normalizing, we need to read the entire image region.
        final boolean normalize = (opList.getFirst(Normalize.class) != null);

        final ProcessBuilder pb = getProcessBuilder(
                opList, info.getSize(), info.getNumResolutions(),
                reductionFactor, normalize, intermediateFile);
        LOGGER.debug("Invoking {}", String.join(" ", pb.command()));
        final Process process = pb.start();

        try (final InputStream processInputStream =
                     new BufferedInputStream(process.getInputStream());
             final InputStream processErrorStream = process.getErrorStream()) {
            pool.submit(new StreamCopier(processErrorStream, errorOutput));
            pool.submit(new StreamCopier(processInputStream, inputBucket));

            final int code = process.waitFor();
            if (code != 0) {
                LOGGER.warn("{} returned with code {}", KDU_EXPAND_NAME, code);
                String errorStr = toString(errorOutput);
                errorStr += "\nPathname: " + getSourceFile();
                throw new IOException(errorStr);
            }
        } finally {
            process.destroy();
        }

        try (InputStream is = Files.newInputStream(intermediateFile)) {
            final ImageReader reader =
                    new ImageReaderFactory().newImageReader(is, Format.BMP);
            try {
                final BufferedImage image = reader.read();
                Set<ReaderHint> hints =
                        EnumSet.noneOf(ReaderHint.class);
                if (!normalize) {
                    hints.add(ReaderHint.ALREADY_CROPPED);
                }
                postProcess(image, hints, opList, info,
                        reductionFactor, outputStream);
            } finally {
                reader.dispose();
            }
        } finally {
            TaskQueue.getInstance().submit(() -> {
                LOGGER.debug("Deleting {}", intermediateFile);
                Files.delete(intermediateFile);
                return null;
            });
        }
    }

    private void processInUnix(final OperationList opList,
                               final Info info,
                               final ByteArrayOutputStream errorOutput,
                               final OutputStream outputStream)
            throws IOException, InterruptedException {
        final ReductionFactor reductionFactor = new ReductionFactor();

        // If we are normalizing, we need to read the entire image region.
        final boolean normalize = (opList.getFirst(Normalize.class) != null);

        final ProcessBuilder pb = getProcessBuilder(
                opList, info.getSize(), info.getNumResolutions(),
                reductionFactor, normalize, stdoutSymlink);
        LOGGER.debug("Invoking {}", String.join(" ", pb.command()));
        final Process process = pb.start();

        try (final InputStream processInputStream =
                     new BufferedInputStream(process.getInputStream());
             final InputStream processErrorStream = process.getErrorStream()) {
            ThreadPool.getInstance().submit(
                    new StreamCopier(processErrorStream, errorOutput));

            final ImageReader reader = new ImageReaderFactory().newImageReader(
                    processInputStream, Format.BMP);
            try {
                final BufferedImage image = reader.read();
                Set<ReaderHint> hints =
                        EnumSet.noneOf(ReaderHint.class);
                if (!normalize) {
                    hints.add(ReaderHint.ALREADY_CROPPED);
                }
                postProcess(image, hints, opList, info,
                        reductionFactor, outputStream);

                final int code = process.waitFor();
                if (code != 0) {
                    LOGGER.warn("{} returned with code {}",
                            KDU_EXPAND_NAME, code);
                    String errorStr = toString(errorOutput);
                    errorStr += "\nPathname: " + getSourceFile();
                    throw new IOException(errorStr);
                }
            } finally {
                reader.dispose();
            }
        } finally {
            process.destroy();
        }
    }

    /**
     * @param opList
     * @param imageSize  The full size of the source image.
     * @param reduction  The {@link ReductionFactor#factor} property will be
     *                   modified.
     * @param ignoreCrop Ignore any cropping directives provided in
     *                   {@literal opList}.
     * @param outputFile File to write to.
     * @return           {@link ProcessBuilder} for invoking {@literal
     *                   kdu_expand} with arguments corresponding to the given
     *                   arguments.
     */
    private ProcessBuilder getProcessBuilder(final OperationList opList,
                                             final Dimension imageSize,
                                             final int numResolutions,
                                             final ReductionFactor reduction,
                                             final boolean ignoreCrop,
                                             final Path outputFile) {
        final List<String> command = new ArrayList<>(30);
        command.add(getPath());
        command.add("-quiet");
        command.add("-resilient");
        command.add("-no_alpha");
        command.add("-i");
        command.add(sourceFile.toString());

        for (Operation op : opList) {
            if (op instanceof Crop && !ignoreCrop) {
                final Crop crop = (Crop) op;
                if (!crop.isFull()) {
                    final NumberFormat xFormat =
                            NumberFormat.getInstance(Locale.US);
                    xFormat.setRoundingMode(RoundingMode.DOWN);
                    // This will always be true for Locale.US. No need to check
                    // if it isn't since the kdu_expand invocation will make
                    // that obvious.
                    if (xFormat instanceof DecimalFormat) {
                        // Truncate coordinates to (num digits) + 1 decimal
                        // places to prevent kdu_expand from returning an extra
                        // pixel of width/height.
                        // N.B.: this broke sometime between KDU v7.6 and
                        // v7.10.4, and kdu_expand now unpredictably returns an
                        // extra pixel. Too bad, but Java2DUtil.crop() will
                        // take care of it.
                        final int xDecimalPlaces =
                                Integer.toString(imageSize.width).length() + 1;
                        String xPattern =
                                "#." + StringUtils.repeat("#", xDecimalPlaces);
                        ((DecimalFormat) xFormat).applyPattern(xPattern);
                    }

                    final NumberFormat yFormat =
                            NumberFormat.getInstance(Locale.US);
                    yFormat.setRoundingMode(RoundingMode.DOWN);
                    if (yFormat instanceof DecimalFormat) {
                        final int yDecimalPlaces =
                                Integer.toString(imageSize.height).length() + 1;
                        String yPattern =
                                "#." + StringUtils.repeat("#", yDecimalPlaces);
                        ((DecimalFormat) yFormat).applyPattern(yPattern);
                    }

                    double x, y, width, height; // 0-1
                    if (Crop.Shape.SQUARE.equals(crop.getShape())) {
                        final int shortestSide =
                                Math.min(imageSize.width, imageSize.height);
                        x = (imageSize.width - shortestSide) /
                                (double) imageSize.width / 2f;
                        y = (imageSize.height - shortestSide) /
                                (double) imageSize.height / 2f;
                        width = shortestSide / (double) imageSize.width;
                        height = shortestSide / (double) imageSize.height;
                    } else {
                        x = crop.getX();
                        y = crop.getY();
                        width = crop.getWidth();
                        height = crop.getHeight();
                        if (Crop.Unit.PIXELS.equals(crop.getUnit())) {
                            x /= imageSize.width;
                            y /= imageSize.height;
                            width /= imageSize.width;
                            height /= imageSize.height;
                        }
                    }
                    command.add("-region");
                    command.add(String.format("{%s,%s},{%s,%s}",
                            yFormat.format(y),
                            xFormat.format(x),
                            yFormat.format(height),
                            xFormat.format(width)));
                }
            } else if (op instanceof Scale) {
                // kdu_expand is not capable of arbitrary scaling, but it does
                // offer a -reduce argument to select a decomposition level,
                // significantly speeding decompression. We can use it if the
                // scale mode is ASPECT_FIT_* and either the percent is <=50,
                // or the height/width are <=50% of full size.
                final Scale scale = (Scale) op;
                final Dimension tileSize = getCroppedSize(opList, imageSize);
                if (!ignoreCrop) {
                    int numDWTLevels = numResolutions - 1;
                    if (numDWTLevels < 0) {
                        numDWTLevels = FALLBACK_NUM_DWT_LEVELS;
                    }
                    reduction.factor = scale.getReductionFactor(
                            tileSize, numDWTLevels).factor;

                    if (reduction.factor > 0) {
                        command.add("-reduce");
                        command.add(reduction.factor + "");
                    }
                }
            }
        }

        command.add("-o");
        command.add(outputFile.toString());

        return new ProcessBuilder(command);
    }

}
