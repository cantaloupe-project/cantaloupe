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
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
 * It produces BMP output which is streamed to an ImageIO reader. (BMP does not
 * copy embedded ICC profiles into output images, but {@literal opj_decompress}
 * converts the RGB source data itself. BMP also doesn't support more than 8
 * bits per sample, which means that this processor can't respect {@link
 * Encode#getMaxSampleSize()}, and all output is &le; 8 bits.)</p>
 *
 * <p>{@literal opj_decompress} reads and writes the files named in the
 * {@literal -i} and {@literal -o} arguments passed to it, respectively. The
 * file in the {@literal -o} argument must have a {@literal .bmp} extension.
 * This means that it's not possible to natively write to a {@link
 * Process#getInputStream() process input stream}. Instead, we have to resort
 * to a special trick whereby we create a symlink from
 * {@literal /tmp/whatever.bmp} to {@literal /dev/stdout}, which will enable us
 * to accomplish this. The temporary symlink is created in the static
 * initializer and deleted on exit.</p>
 *
 * <p>Unfortunately, Windows doesn't have anything like {@literal /dev/stdout},
 * so this processor won't work there.</p>
 *
 * <p><strong>opj_decompress version 2.2.0 is highly recommended.</strong>
 * Earlier versions echo log messages to stdout, which can cause problems with
 * some images.</p>
 *
 * <p>Although {@literal opj_decompress} is used for reading images,
 * {@literal opj_dump} is <strong>not</strong> used for reading metadata.
 * ImageIO is used instead, as it is more efficient.</p>
 */
class OpenJpegProcessor extends AbstractJava2DProcessor
        implements FileProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(OpenJpegProcessor.class);

    private static final short MAX_REDUCTION_FACTOR = 5;

    /** Lazy-set by {@link #isQuietModeSupported()} */
    private static boolean checkedForQuietMode = false;

    /** Set by {@link #initialize()} */
    private static final AtomicBoolean initializationAttempted =
            new AtomicBoolean(false);
    /** Set by {@link #initialize()} */
    private static InitializationException initializationException;

    /** Lazy-set by {@link #isQuietModeSupported()} */
    private static boolean isQuietModeSupported = true;

    private final static Format intermediateFormat = Format.BMP;

    private static Path devStdout;

    /**
     * Creates a unique symlink to /dev/stdout in a temporary directory.
     */
    private static Path createStdoutSymlink() throws IOException {
        final Path link = Application.getTempPath().resolve(String.format("%s-%s-%s.%s",
                Application.NAME,
                OpenJpegProcessor.class.getSimpleName(),
                UUID.randomUUID(),
                intermediateFormat.getPreferredExtension()));

        return Files.createSymbolicLink(link, devStdout);
    }

    /**
     * @param binaryName Name of one of the opj_* binaries.
     * @return Absolute path to the given binary.
     */
    private static String getPath(String binaryName) {
        String path = Configuration.getInstance().
                getString(Key.OPENJPEGPROCESSOR_PATH_TO_BINARIES);
        if (path != null && path.length() > 0) {
            path = StringUtils.stripEnd(path, File.separator) +
                    File.separator + binaryName;
        } else {
            path = binaryName;
        }
        return path;
    }

    private static synchronized void initialize() {
        initializationAttempted.set(true);

        try {
            // Check for the presence of opj_decompress.
            invoke("opj_decompress");

            // Due to a quirk of opj_decompress, this processor requires access to
            // /dev/stdout.
            devStdout = Paths.get("/dev/stdout");
            if (!Files.isWritable(devStdout)) {
                LOGGER.error(OpenJpegProcessor.class.getSimpleName() +
                        " won't work on this platform as it requires access " +
                        "to /dev/stdout.");
            }
        } catch (IOException e) {
            initializationException = new InitializationException(e);
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

    @Override
    public Set<Format> getAvailableOutputFormats() {
        final Set<Format> outputFormats;
        if (Format.JP2.equals(format)) {
            outputFormats = ImageWriter.supportedFormats();
        } else {
            outputFormats = Collections.unmodifiableSet(Collections.emptySet());
        }
        return outputFormats;
    }

    /**
     * Computes the effective size of an image after all crop operations are
     * applied but excluding any scale operations, in order to use
     * opj_decompress' -r (reduce) argument.
     */
    private static Dimension getCroppedSize(OperationList opList, Dimension fullSize) {
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
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(opList, imageInfo, outputStream);

        // Will receive stderr output from opj_decompress.
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();

        Path intermediateOutput = null;
        try {
            try {
                intermediateOutput = createStdoutSymlink();
            } catch (IOException e1) {
                throw new ProcessorException("Failed to create stdout symlink.", e1);
            }

            try {
                final ReductionFactor reductionFactor = new ReductionFactor();

                // If we are normalizing, we need to read the entire image region.
                final boolean normalize = (opList.getFirst(Normalize.class) != null);

                final ProcessBuilder pb = getProcessBuilder(
                        opList, imageInfo.getSize(), reductionFactor, normalize,
                        sourceFile, intermediateOutput);
                LOGGER.info("Invoking {}", String.join(" ", pb.command()));
                final Process process = pb.start();

                try (final InputStream processInputStream =
                             new BufferedInputStream(process.getInputStream());
                     final InputStream processErrorStream = process.getErrorStream()) {
                    ThreadPool.getInstance().submit(
                            new StreamCopier(processErrorStream, errorBucket));

                    final ImageReader reader = new ImageReader(
                            processInputStream, intermediateFormat);
                    final BufferedImage image = reader.read();
                    try {
                        Set<ImageReader.Hint> hints =
                                EnumSet.noneOf(ImageReader.Hint.class);
                        if (!normalize) {
                            hints.add(ImageReader.Hint.ALREADY_CROPPED);
                        }
                        postProcess(image, hints, opList, imageInfo,
                                reductionFactor, outputStream);
                        final int code = process.waitFor();
                        if (code != 0) {
                            LOGGER.warn("opj_decompress returned with code {}", code);
                            String errorStr = toString(errorBucket);
                            errorStr += "\nPathname: " + getSourceFile();
                            throw new ProcessorException(errorStr);
                        }
                    } finally {
                        reader.dispose();
                    }
                } finally {
                    process.destroy();
                }
            } catch (EOFException e) {
                // This is usually caused by the connection closing.
                String msg = e.getMessage();
                msg = String.format("process(): %s (%s)",
                        (msg != null && msg.length() > 0) ? msg : "EOFException",
                        opList.toString());
                LOGGER.info(msg, e);
                throw new ProcessorException(msg, e);
            } catch (IOException | InterruptedException e) {
                String msg = e.getMessage();
                final String errorStr = toString(errorBucket);
                if (errorStr.length() > 0) {
                    msg += " (command output: " + errorStr + ")";
                }
                throw new ProcessorException(msg, e);
            }
        } finally {
            try {
                Files.deleteIfExists(intermediateOutput);
            } catch (IOException e) {
                throw new ProcessorException("Failed to delete stdout symlink.", e);
            }
        }
    }

    /**
     * Gets a ProcessBuilder corresponding to the given parameters.
     *
     * @param opList
     * @param imageSize  The full size of the source image.
     * @param reduction  The {@link ReductionFactor#factor} property will be
     *                   modified.
     * @param ignoreCrop Ignore any cropping directives provided in
     *                   <code>opList</code>.
     * @return opj_decompress command invocation string
     */
    private static ProcessBuilder getProcessBuilder(final OperationList opList,
                                             final Dimension imageSize,
                                             final ReductionFactor reduction,
                                             final boolean ignoreCrop,
                                             final Path input,
                                             final Path output) {
        final List<String> command = new ArrayList<>(30);

        command.add(getPath("opj_decompress"));

        if (isQuietModeSupported()) {
            command.add("-quiet");
        }

        command.add("-i");
        command.add(input.toString());

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
                    reduction.factor = scale.getReductionFactor(
                            tileSize, MAX_REDUCTION_FACTOR).factor;
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
        command.add(output.toString());

        return new ProcessBuilder(command);
    }

}
