package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Processor using the OpenJPEG {@literal opj_decompress} command-line
 * tool.</p>
 *
 * <p>{@literal opj_decompress} is used for cropping and an initial scale
 * reduction factor, and Java 2D is used for all remaining processing steps.
 * It produces BMP output which is streamed into an ImageIO reader. (BMP does
 * not copy embedded ICC profiles into output images, but {@literal
 * opj_decompress} converts the RGB source data itself. {@literal
 * opj_decompress} does not produce BMPs with more than 8 bits per sample,
 * which means that this processor can't respect {@link
 * Encode#getMaxSampleSize()}, and all output is &le; 8 bits.)</p>
 *
 * <p>{@literal opj_decompress} reads and writes the files named in the
 * {@literal -i} and {@literal -o} arguments passed to it, respectively. The
 * file in the {@literal -o} argument must have a {@literal .bmp} extension.
 * This means that it's not possible to natively write to a {@link
 * Process#getInputStream() process input stream}. The way this is dealt with
 * differs between Windows and Unix:</p>
 *
 * <dl>
 *     <dt>Unix</dt>
 *     <dd>A symlink is created by {@link #initialize()} from {@literal
 *     /tmp/whatever.bmp} to {@literal /dev/stdout}, and set to delete on exit.
 *     {@literal opj_decompress} then effectively writes to standard output,
 *     which can be read from a {@link Process#getInputStream() process' input
 *     stream}.</dd>
 *     <dt>Windows</dt>
 *     <dd>Windows doesn't have anything like {@literal /dev/stdout}&mdash;
 *     actually it has {@literal CON}, but experimentation reveals it won't
 *     work&mdash;so {@literal opj_decompress} instead writes to a temporary
 *     file which is deleted after being read. This is slower than the Unix
 *     technique.</dd>
 * </dl>
 *
 * <p>Although {@literal opj_decompress} is used for reading images,
 * {@literal opj_dump} is <strong>not</strong> used for reading metadata.
 * The ImageIO JPEG2000 plugin is used instead, as this doesn't require
 * invoking another process and is therefore more efficient.</p>
 *
 * <p><strong>opj_decompress version 2.2.0 is highly recommended.</strong>
 * Earlier versions echo log messages to stdout, which can cause problems with
 * some images.</p>
 */
class OpenJpegProcessor extends AbstractJava2DProcessor
        implements FileProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(OpenJpegProcessor.class);

    /**
     * Number of decomposition levels assumed to be contained in the image when
     * that information cannot be obtained for some reason. 5 is the default
     * used by most JP2 encoders. Setting this to a value higher than that could
     * cause decoding errors, and setting it to lower could have a performance
     * cost.
     */
    private static final short FALLBACK_NUM_DWT_LEVELS = 5;

    /**
     * Used only in Windows.
     */
    private static final String WINDOWS_SCRATCH_DIR_NAME =
            OpenJpegProcessor.class.getSimpleName() + "-scratch";

    /**
     * Set by {@link #isQuietModeSupported()}.
     */
    private static boolean checkedForQuietMode = false;

    /**
     * Set by {@link #isQuietModeSupported()}.
     */
    private static boolean isQuietModeSupported = true;

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
     * @see <a href="https://github.com/medusa-project/cantaloupe/issues/190">
     *     OpenJpegProcessor operating on low bit-depth images</a>
     */
    private final static Format intermediateFormat = Format.BMP;

    /**
     * @param binaryName Name of one of the OpenJPEG binaries.
     * @return           Absolute path to the given binary.
     */
    private static String getPath(String binaryName) {
        String path = Configuration.getInstance().
                getString(Key.OPENJPEGPROCESSOR_PATH_TO_BINARIES);
        if (path != null && !path.isEmpty()) {
            path = StringUtils.stripEnd(path, File.separator) +
                    File.separator + binaryName;
        } else {
            path = binaryName;
        }
        return path;
    }

    /**
     * Used only in Windows.
     *
     * @return Thread-safe path of an intermediate image from {@literal
     *         opj_decompress} based on the given operation list.
     */
    private static Path getIntermediateImageFile(OperationList opList) {
        final String name = opList.toFilename() + "-" +
                Thread.currentThread().getName() + "." +
                intermediateFormat.getPreferredExtension();
        return getScratchDir().resolve(name);
    }

    /**
     * Used only in Windows.
     *
     * @return Path to the scratch directory that stores output images from
     *         {@literal opj_decompress}.
     */
    private static Path getScratchDir() {
        Path tempPath = Application.getTempPath();
        return tempPath.resolve(WINDOWS_SCRATCH_DIR_NAME);
    }

    private static synchronized void initialize() {
        initializationAttempted.set(true);

        try {
            // Check for the presence of opj_decompress.
            invoke("opj_decompress");

            if (isWindows()) {
                initializeForWindows();
            }
            // Unix doesn't need any initialization.
        } catch (IOException e) {
            initializationException = new InitializationException(e);
        }
    }

    private static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    private static void initializeForWindows() throws IOException {
        final Path scratchDir = getScratchDir();

        if (!Files.exists(scratchDir)) {
            LOGGER.debug("Creating {}", scratchDir);
            Files.createDirectories(scratchDir);
        }
    }

    private static void invoke(String opjBinary) throws IOException {
        final ProcessBuilder pb = new ProcessBuilder();
        List<String> command = new ArrayList<>();
        command.add(getPath(opjBinary));
        pb.command(command);
        String commandString = String.join(" ", pb.command());
        LOGGER.info("invoke(): {}", commandString);
        pb.start();
    }

    static synchronized boolean isQuietModeSupported() {
        if (!checkedForQuietMode) {
            final List<String> command = new ArrayList<>();
            command.add(getPath("opj_decompress"));
            command.add("-h");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            LOGGER.debug("isQuietModeSupported(): invoking {}",
                    String.join(" ", pb.command()));
            try {
                Process process = pb.start();
                try (InputStream processInputStream =
                             new BufferedInputStream(process.getInputStream())) {
                    String opjOutput = IOUtils.toString(processInputStream, "UTF-8");

                    // We are looking for the following line in the output of
                    // opj_decompress -h:
                    // "It has been compiled against openjp2 library vX.X.X."
                    // (Where X.X.X is 2.2.0 or later.)
                    Pattern pattern = Pattern.compile("[ ]v\\d+.\\d+.\\d+");
                    Matcher matcher = pattern.matcher(opjOutput);

                    if (matcher.find()) {
                        String version = matcher.group(0).substring(2); // after " v"
                        LOGGER.info("opj_decompress reports version {}", version);

                        String[] parts = StringUtils.split(version, ".");
                        if (parts.length == 3) {
                            int major = Integer.parseInt(parts[0]);
                            int minor = Integer.parseInt(parts[1]);
                            isQuietModeSupported =
                                    ((major >= 2 && minor >= 2) || major > 2);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("isQuietModeSupported(): {}", e.getMessage());
            }

            if (!isQuietModeSupported) {
                LOGGER.warn("This version of opj_decompress doesn't support " +
                        "quiet mode. Please upgrade OpenJPEG to version 2.2.0 "+
                        "or later.");
            }

            checkedForQuietMode = true;
        }
        return isQuietModeSupported;
    }

    /**
     * For testing only!
     */
    static synchronized void resetInitialization() {
        initializationAttempted.set(false);
        initializationException = null;
    }

    /**
     * For testing only.
     */
    static synchronized void setQuietModeSupported(boolean trueOrFalse) {
        isQuietModeSupported = trueOrFalse;
    }

    private static String toString(ByteArrayOutputStream os) {
        try {
            return new String(os.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
            return "";
        }
    }

    OpenJpegProcessor() {
        if (!initializationAttempted.get()) {
            initialize();
        }
    }

    /**
     * <p>Creates a symlink to {@literal /dev/stdout} in a temporary directory.
     * The symlink is for the exclusive use of the instance and should be
     * cleaned up when no longer needed.</p>
     *
     * <p>Not used in Windows.</p>
     */
    private Path createStdoutSymlink() throws IOException {
        final String name = OpenJpegProcessor.class.getSimpleName() + "-" +
                UUID.randomUUID() + "." +
                intermediateFormat.getPreferredExtension();
        final Path link = Application.getTempPath().resolve(name);
        final Path devStdout = Paths.get("/dev/stdout");

        LOGGER.debug("Creating link from {} to {}", link, devStdout);
        return Files.createSymbolicLink(link, devStdout);
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        final Set<Format> outputFormats;
        if (Format.JP2.equals(format)) {
            outputFormats = ImageWriterFactory.supportedFormats();
        } else {
            outputFormats = Collections.unmodifiableSet(Collections.emptySet());
        }
        return outputFormats;
    }

    /**
     * Computes the effective size of an image after all crop operations are
     * applied but excluding any scale operations, in order to use
     * {@literal opj_decompress}' {@literal -r} (reduce) argument.
     */
    private static Dimension getCroppedSize(OperationList opList,
                                            Dimension fullSize) {
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
     * Override that disposes the reader since it won't be needed for anything
     * else.
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
    public List<String> getWarnings() {
        List<String> warnings = new ArrayList<>();
        if (!isQuietModeSupported()) {
            warnings.add("This version of opj_decompress doesn't support " +
                    "quiet mode. Please upgrade OpenJPEG to version 2.2.0 "+
                    "or later.");
        }
        return warnings;
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
        // Will receive stdin output from opj_decompress (but none is expected).
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
                LOGGER.warn("opj_decompress returned with code {}", code);
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
            pool.submit(() -> {
                LOGGER.debug("Deleting {}", intermediateFile);
                Files.delete(intermediateFile);
                return null;
            }, ThreadPool.Priority.LOW);
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

        final Path stdoutSymlink = createStdoutSymlink();
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
                final Set<ReaderHint> hints = EnumSet.noneOf(ReaderHint.class);
                if (!normalize) {
                    hints.add(ReaderHint.ALREADY_CROPPED);
                }
                postProcess(image, hints, opList, info,
                        reductionFactor, outputStream);

                final int code = process.waitFor();
                if (code != 0) {
                    LOGGER.warn("opj_decompress returned with code {}", code);
                    String errorStr = toString(errorOutput);
                    errorStr += "\nPathname: " + getSourceFile();
                    throw new IOException(errorStr);
                }
            } finally {
                reader.dispose();
            }
        } finally {
            process.destroy();

            ThreadPool.getInstance().submit(() -> {
                LOGGER.debug("Deleting {}", stdoutSymlink);
                Files.delete(stdoutSymlink);
                return null;
            }, ThreadPool.Priority.LOW);
        }
    }

    /**
     * Returns an instance corresponding to the given arguments.
     *
     * @param opList
     * @param imageSize  Full size of the source image.
     * @param reduction  The {@link ReductionFactor#factor} property will be
     *                   modified.
     * @param ignoreCrop Ignore any cropping directives provided in {@literal
     *                   opList}.
     * @param outputFile Symlink (Unix) or intermediate file (Windows) to write
     *                   to.
     * @return           {@literal opj_decompress} command invocation string.
     */
    private ProcessBuilder getProcessBuilder(final OperationList opList,
                                             final Dimension imageSize,
                                             final int numResolutions,
                                             final ReductionFactor reduction,
                                             final boolean ignoreCrop,
                                             final Path outputFile) {
        final List<String> command = new ArrayList<>(30);
        command.add(getPath("opj_decompress"));

        if (isQuietModeSupported()) {
            command.add("-quiet");
        }

        command.add("-i");
        command.add(sourceFile.toString());

        for (Operation op : opList) {
            if (op instanceof Crop && !ignoreCrop) {
                final Crop crop = (Crop) op;
                if (crop.hasEffect()) {
                    Rectangle rect = crop.getRectangle(imageSize);
                    command.add("-d");
                    command.add(String.format("%d,%d,%d,%d",
                            rect.x, rect.y, rect.x + rect.width,
                            rect.y + rect.height));
                }
            } else if (op instanceof Scale) {
                // opj_decompress is not capable of arbitrary scaling, but it
                // does offer a -r (reduce) argument which is capable of
                // downscaling of factors of 2, significantly speeding
                // decompression. We can use it if the scale mode is
                // ASPECT_FIT_* and either the percent is <=50, or the
                // height/width are <=50% of full size.
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
                        command.add("-r");
                        command.add(reduction.factor + "");
                    } else if (reduction.factor < 0) {
                        // Don't allow a negative factor because opj_decompress
                        // can only reduce, not enlarge.
                        reduction.factor = 0;
                    }
                }
            }
        }

        command.add("-o");
        command.add(outputFile.toString());

        return new ProcessBuilder(command);
    }

}
