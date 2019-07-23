package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.async.TaskQueue;
import edu.illinois.library.cantaloupe.async.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.iptc.Reader;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFacade;
import edu.illinois.library.cantaloupe.processor.codec.jpeg2000.JPEG2000MetadataReader;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.source.stream.BufferedImageInputStream;
import edu.illinois.library.cantaloupe.util.CommandLocator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.FileImageInputStream;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
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
 * opj_decompress} converts the RGB source data itself.)</p>
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
class OpenJpegProcessor extends AbstractProcessor implements FileProcessor {

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

    private static final String OPJ_DECOMPRESS_NAME = "opj_decompress";

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
    private static final AtomicBoolean IS_INITIALIZATION_ATTEMPTED =
            new AtomicBoolean(false);

    /**
     * Set by {@link #initialize()}.
     */
    private static String initializationError;

    /**
     * @see <a href="https://github.com/cantaloupe-project/cantaloupe/issues/190">
     *     OpenJpegProcessor operating on low bit-depth images</a>
     */
    private final static Format intermediateFormat = Format.BMP;

    private Path sourceFile;

    /**
     * {@literal opj_decompress} has problems with some files that are missing
     * a JP2 filename extension. When {@link #sourceFile} does not contain one,
     * this will be set by {@link #setSourceFile(Path)} and used instead of
     * {@link #sourceFile}.
     *
     * N.B.: the symlink must be cleaned up when processing is complete.
     */
    private Path sourceSymlink;

    private static String getPath() {
        String searchPath = Configuration.getInstance().
                getString(Key.OPENJPEGPROCESSOR_PATH_TO_BINARIES);
        return CommandLocator.locate(OPJ_DECOMPRESS_NAME, searchPath);
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
        return Application.getTempPath().resolve(WINDOWS_SCRATCH_DIR_NAME);
    }

    private static synchronized void initialize() {
        IS_INITIALIZATION_ATTEMPTED.set(true);

        try {
            // Check for the presence of opj_decompress.
            invoke();

            if (isWindows()) {
                initializeForWindows();
            }
            // Unix doesn't need any initialization.
        } catch (IOException e) {
            initializationError = e.getMessage();
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

    private static void invoke() throws IOException {
        final ProcessBuilder pb = new ProcessBuilder();
        List<String> command = new ArrayList<>();
        command.add(getPath());
        pb.command(command);
        String commandString = String.join(" ", pb.command());
        LOGGER.info("invoke(): {}", commandString);
        pb.start();
    }

    static synchronized boolean isQuietModeSupported() {
        if (!checkedForQuietMode) {
            final List<String> command = new ArrayList<>();
            command.add(getPath());
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
                        LOGGER.info("{} reports version {}",
                                OPJ_DECOMPRESS_NAME, version);

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
                LOGGER.warn("This version of " + OPJ_DECOMPRESS_NAME +
                        " doesn't support quiet mode. Please upgrade" +
                        " OpenJPEG to version 2.2.0 or later.");
            }

            checkedForQuietMode = true;
        }
        return isQuietModeSupported;
    }

    /**
     * For testing only!
     */
    static synchronized void resetInitialization() {
        IS_INITIALIZATION_ATTEMPTED.set(false);
        initializationError = null;
    }

    /**
     * For testing only.
     */
    static synchronized void setQuietModeSupported(boolean trueOrFalse) {
        isQuietModeSupported = trueOrFalse;
    }

    private static String toString(ByteArrayOutputStream os) {
        return new String(os.toByteArray(), StandardCharsets.UTF_8);
    }

    OpenJpegProcessor() {
        if (!IS_INITIALIZATION_ATTEMPTED.get()) {
            initialize();
        }
    }

    @Override
    public void close() {
        if (sourceSymlink != null) {
            TaskQueue.getInstance().submit(() -> {
                LOGGER.trace("Deleting {}", sourceSymlink);
                Files.deleteIfExists(sourceSymlink);
                return null;
            });
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
        if (Format.JP2.equals(getSourceFormat())) {
            outputFormats = ImageWriterFactory.supportedFormats();
        } else {
            outputFormats = Collections.unmodifiableSet(Collections.emptySet());
        }
        return outputFormats;
    }

    @Override
    public String getInitializationError() {
        if (!IS_INITIALIZATION_ATTEMPTED.get()) {
            initialize();
        }
        return initializationError;
    }

    @Override
    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public void setSourceFile(Path sourceFile) {
        this.sourceFile = sourceFile;

        // N.B.: As of version 2.3.0, opj_decompress fails to open certain
        // files without a .jp2 extension. This is most notably an issue when
        // reading from the source cache, as those files don't have extensions.
        //
        // Our workaround, when we are reading a file without a recognized
        // extension, is to create a symlink to the file to read that has the
        // extension opj_decompress needs, remembering to delete it in close().
        //
        // We still must "touch" the source file, so that FilesystemCache knows
        // it's been accessed.
        final String filename = sourceFile.toString().toLowerCase();
        if (!filename.endsWith(".jp2") && !filename.endsWith(".jpx") &&
                !filename.endsWith(".j2k")) {
            // Touch the file (in the background since we don't care about
            // the result).
            TaskQueue.getInstance().submit(() -> {
                try {
                    Files.setLastModifiedTime(sourceFile,
                            FileTime.from(Instant.now()));
                } catch (IOException e) {
                    LOGGER.error("setSourceFile(): failed to touch file: {}",
                            e.getMessage());
                }
            });

            // Create the symlink.
            try {
                final String name = OpenJpegProcessor.class.getSimpleName() +
                        "-" + UUID.randomUUID() + ".jp2";
                sourceSymlink = Application.getTempPath().resolve(name);

                LOGGER.trace("Creating link from {} to {}",
                        sourceSymlink, sourceFile);
                Files.createSymbolicLink(sourceSymlink, sourceFile);
            } catch (IOException e) {
                LOGGER.error("setSourceFile(): {}", e.getMessage());
                sourceSymlink = null;
            }
        }
    }

    @Override
    public Info readInfo() throws IOException {
        final Info info = new Info();
        info.setSourceFormat(getSourceFormat());

        try (final JPEG2000MetadataReader reader = new JPEG2000MetadataReader()) {
            reader.setSource(new BufferedImageInputStream(
                    new FileImageInputStream(getSourceFile().toFile())));

            Metadata metadata = new Metadata();
            byte[] iptc = reader.getIPTC();
            if (iptc != null) {
                try (Reader iptcReader = new Reader()) {
                    iptcReader.setSource(iptc);
                    metadata.setIPTC(iptcReader.read());
                }
            }
            metadata.setXMP(reader.getXMP());
            info.setMetadata(metadata);
            info.setNumResolutions(reader.getNumDecompositionLevels() + 1);

            Info.Image image = info.getImages().get(0);
            image.setSize(new Dimension(reader.getWidth(), reader.getHeight()));
            image.setTileSize(new Dimension(reader.getTileWidth(), reader.getTileHeight()));
            // JP2 tile dimensions are inverted, so swap them
            if ((image.width > image.height && image.tileWidth < image.tileHeight) ||
                    (image.width < image.height && image.tileWidth > image.tileHeight)) {
                int tmp = image.tileWidth;
                //noinspection SuspiciousNameCombination
                image.tileWidth = image.tileHeight;
                image.tileHeight = tmp;
            }
            return info;
        }
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
                        final OutputStream outputStream) throws FormatException, ProcessorException {
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
            final String errorStr = toString(errorBucket);
            if (errorStr.contains("Unknown input file format")) {
                throw new SourceFormatException(getSourceFormat());
            }
            throw new ProcessorException(
                    e.getMessage() + " (command output: " + errorStr + ")", e);
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

        final ProcessBuilder pb = getProcessBuilder(
                opList, info.getSize(), info.getNumResolutions(),
                reductionFactor, intermediateFile);
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
                final Set<ReaderHint> hints =
                        EnumSet.of(ReaderHint.ALREADY_CROPPED);

                Java2DPostProcessor.postProcess(
                        image, hints, opList, info, reductionFactor);

                ImageWriterFacade.write(image,
                        (Encode) opList.getFirst(Encode.class),
                        outputStream);
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

        final Path stdoutSymlink = createStdoutSymlink();
        final ProcessBuilder pb = getProcessBuilder(
                opList, info.getSize(), info.getNumResolutions(),
                reductionFactor, stdoutSymlink);
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
                final Set<ReaderHint> hints =
                        EnumSet.of(ReaderHint.ALREADY_CROPPED);

                BufferedImage image = reader.read();
                image = Java2DPostProcessor.postProcess(
                        image, hints, opList, info, reductionFactor);

                ImageWriterFacade.write(image,
                        (Encode) opList.getFirst(Encode.class),
                        outputStream);

                final int code = process.waitFor();
                if (code != 0) {
                    LOGGER.warn("{} returned with code {}",
                            OPJ_DECOMPRESS_NAME, code);
                    String errorStr = toString(errorOutput);
                    errorStr += "\nPathname: " + getSourceFile();
                    throw new IOException(errorStr);
                }
            } finally {
                reader.dispose();
            }
        } finally {
            process.destroy();

            TaskQueue.getInstance().submit(() -> {
                LOGGER.debug("Deleting {}", stdoutSymlink);
                Files.delete(stdoutSymlink);
                return null;
            });
        }
    }

    /**
     * Returns an instance corresponding to the given arguments.
     *
     * @param opList
     * @param fullSize       Full size of the source image.
     * @param numResolutions Number of resolutions (DWT levels + 1) available
     *                       in the source image.
     * @param reduction      The {@link ReductionFactor#factor} property will
     *                       be modified.
     * @param outputFile     File to write to.
     * @return               {@link ProcessBuilder} for invoking {@literal
     *                       opj_decompress} with arguments corresponding to
     *                       the given arguments.
     */
    private ProcessBuilder getProcessBuilder(final OperationList opList,
                                             final Dimension fullSize,
                                             final int numResolutions,
                                             final ReductionFactor reduction,
                                             final Path outputFile) {
        final List<String> command = new ArrayList<>(30);
        command.add(getPath());

        if (isQuietModeSupported()) {
            command.add("-quiet");
        }

        command.add("-i");
        command.add(getSourceFile().toString());

        for (Operation op : opList) {
            if (!op.hasEffect(fullSize, opList)) {
                continue;
            }
            if (op instanceof Crop) {
                final Crop crop = (Crop) op;
                final Rectangle region = crop.getRectangle(
                        fullSize, opList.getScaleConstraint());
                command.add("-d");
                command.add(String.format("%d,%d,%d,%d",
                        region.intX(), region.intY(),
                        region.intX() + region.intWidth(),
                        region.intY() + region.intHeight()));
            } else if (op instanceof Scale) {
                // opj_decompress is not capable of arbitrary scaling, but it
                // does offer a -r (reduce) argument to select a
                // decomposition level, significantly speeding decompression.
                // We can use it if the scale mode is ASPECT_FIT_* and either
                // the percent is <=50, or the height/width are <=50% of full
                // size.
                final Scale scale = (Scale) op;
                final Dimension tileSize = getROISize(opList, fullSize);

                int numDWTLevels = numResolutions - 1;
                if (numDWTLevels < 0) {
                    numDWTLevels = FALLBACK_NUM_DWT_LEVELS;
                }
                reduction.factor = scale.getReductionFactor(
                        tileSize, opList.getScaleConstraint(),
                        numDWTLevels).factor;

                if (reduction.factor > 0) {
                    command.add("-r");
                    command.add(reduction.factor + "");
                }
            }
        }

        command.add("-o");
        command.add(outputFile.toString());

        return new ProcessBuilder(command);
    }

    /**
     * @return Size of the region of interest.
     */
    private static Dimension getROISize(OperationList opList,
                                        Dimension fullSize) {
        Dimension size = new Dimension(fullSize);
        for (Operation op : opList) {
            if (op instanceof Crop) {
                size = ((Crop) op).getRectangle(
                        size, opList.getScaleConstraint()).size();
            }
        }
        return size;
    }

}
