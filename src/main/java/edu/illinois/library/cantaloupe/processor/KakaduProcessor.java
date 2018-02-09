package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
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
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Processor using the Kakadu {@literal kdu_expand} command-line tool.
 * Written against version 7.7, but should work with other versions, as long as
 * their command-line interface is compatible.</p>
 *
 * <p>{@literal kdu_expand} is used for cropping and an initial scale reduction
 * factor, and Java 2D for all remaining processing steps. {@literal
 * kdu_expand} generates TIFF output which is streamed (with some buffering) to
 * an ImageIO reader. (TIFF is used in order to preserve embedded ICC
 * profiles.)</p>
 *
 * <p>{@literal kdu_expand} reads and writes the files named in the
 * {@literal -i} and {@literal -o} flags passed to it, respectively. The file
 * in the {@literal -o} flag must have a recognized image extension such as
 * {@literal .bmp}, {@literal .tif}, etc. This means that it's not possible to
 * natively write into an {@link InputStream} from a {@link Process}. Instead,
 * we have to resort to a trick whereby we create a symlink from {@literal
 * /tmp/whatever.tif} to {@literal /dev/stdout} (which only exists on Unix).
 * The temporary symlink is created by {@link #initialize()} and deleted on
 * exit.</p>
 *
 * <p>Although {@literal kdu_expand} is used for reading images,
 * {@literal kdu_jp2info} is <strong>not</strong> used for reading metadata.
 * ImageIO is used instead, as it is more efficient.</p>
 *
 * @see <a href="http://kakadusoftware.com/wp-content/uploads/2014/06/Usage_Examples-v7_7.txt">
 *     Usage Examples for the Demonstration Applications Supplied with Kakadu
 *     V7.7</a>
 */
class KakaduProcessor extends AbstractJava2DProcessor implements FileProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KakaduProcessor.class);

    private static final short MAX_REDUCTION_FACTOR = 5;

    /**
     * Set by {@link #initialize()}.
     */
    private static final AtomicBoolean initializationAttempted =
            new AtomicBoolean(false);

    /**
     * Set by {@link #initialize()}.
     */
    private static InitializationException initializationException;
    private static Path stdoutSymlink;

    /**
     * Creates a unique symlink to {@literal /dev/stdout} in a temporary
     * directory, and sets it to delete on exit.
     */
    private static void createStdoutSymlink() throws IOException {
        Path tempDir = Application.getTempPath();

        final Path link = tempDir.resolve(Application.NAME + "-" +
                KakaduProcessor.class.getSimpleName() + "-" +
                UUID.randomUUID() + ".tif");
        final Path devStdout = Paths.get("/dev/stdout");

        stdoutSymlink = Files.createSymbolicLink(link, devStdout);
        stdoutSymlink.toFile().deleteOnExit();
    }

    /**
     * @param binaryName Name of one of the Kakadu binaries.
     * @return Absolute path to the given binary.
     */
    private static String getPath(String binaryName) {
        String path = Configuration.getInstance().
                getString(Key.KAKADUPROCESSOR_PATH_TO_BINARIES);
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
            // Check for the presence of kdu_expand.
            invoke("kdu_expand");

            // Due to a quirk of kdu_expand, this processor requires access to
            // /dev/stdout.
            final Path devStdout = Paths.get("/dev/stdout");
            if (Files.exists(devStdout) && Files.isWritable(devStdout)) {
                // Due to another quirk of kdu_expand, we need to create a
                // symlink from {temp path}/stdout.tif to /dev/stdout, to tell
                // kdu_expand what format to write.
                createStdoutSymlink();
            } else {
                LOGGER.error(KakaduProcessor.class.getSimpleName() +
                        " won't work on this platform as it requires access " +
                        "to /dev/stdout.");
            }
        } catch (IOException e) {
            initializationException = new InitializationException(e);
        }
    }

    private static void invoke(String kduBinary) throws IOException {
        final ProcessBuilder pb = new ProcessBuilder();
        List<String> command = new ArrayList<>();
        command.add(getPath(kduBinary));
        pb.command(command);
        String commandString = String.join(" ", pb.command());
        LOGGER.info("invoke(): {}", commandString);
        pb.start();
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

    KakaduProcessor() {
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
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(opList, imageInfo, outputStream);

        // Will receive stderr output from kdu_expand.
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ReductionFactor reductionFactor = new ReductionFactor();

            // If we are normalizing, we need to read the entire image region.
            final boolean normalize = (opList.getFirst(Normalize.class) != null);

            final ProcessBuilder pb = getProcessBuilder(
                    opList, imageInfo.getSize(), reductionFactor, normalize);
            LOGGER.info("Invoking {}", String.join(" ", pb.command()));
            final Process process = pb.start();

            try (final InputStream processInputStream =
                         new BufferedInputStream(process.getInputStream());
                 final InputStream processErrorStream = process.getErrorStream()) {
                ThreadPool.getInstance().submit(
                        new StreamCopier(processErrorStream, errorBucket));

                final ImageReader reader = new ImageReader(
                        processInputStream, Format.TIF);
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
                        LOGGER.warn("kdu_expand returned with code {}", code);
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
    }

    /**
     * @param opList
     * @param imageSize  The full size of the source image.
     * @param reduction  The {@link ReductionFactor#factor} property will be
     *                   modified.
     * @param ignoreCrop Ignore any cropping directives provided in
     *                   {@literal opList}.
     * @return {@link ProcessBuilder} for invoking {@literal kdu_expand} with
     *         arguments corresponding to the given arguments.
     */
    private ProcessBuilder getProcessBuilder(final OperationList opList,
                                             final Dimension imageSize,
                                             final ReductionFactor reduction,
                                             final boolean ignoreCrop) {
        final List<String> command = new ArrayList<>();
        command.add(getPath("kdu_expand"));
        command.add("-quiet");
        command.add("-resilient");
        command.add("-no_alpha");
        command.add("-i");
        command.add(sourceFile.toString());

        for (Operation op : opList) {
            if (op instanceof Crop && !ignoreCrop) {
                final Crop crop = (Crop) op;
                if (!crop.isFull()) {
                    // Truncate coordinates to (num digits) + 1 decimal places
                    // to prevent kdu_expand from returning an extra pixel of
                    // width/height.
                    final int xDecimalPlaces =
                            Integer.toString(imageSize.width).length() + 1;
                    final int yDecimalPlaces =
                            Integer.toString(imageSize.height).length() + 1;
                    final String xFormat = "#." + StringUtils.repeat("#",
                            xDecimalPlaces);
                    final String yFormat = "#." + StringUtils.repeat("#",
                            yDecimalPlaces);
                    final DecimalFormat xDecFormat = new DecimalFormat(xFormat);
                    xDecFormat.setRoundingMode(RoundingMode.DOWN);
                    final DecimalFormat yDecFormat = new DecimalFormat(yFormat);
                    yDecFormat.setRoundingMode(RoundingMode.DOWN);

                    double x, y, width, height; // These are all percentages.
                    if (crop.getShape().equals(Crop.Shape.SQUARE)) {
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
                        if (crop.getUnit().equals(Crop.Unit.PIXELS)) {
                            x /= imageSize.width;
                            y /= imageSize.height;
                            width /= imageSize.width;
                            height /= imageSize.height;
                        }
                    }
                    command.add("-region");
                    command.add(String.format("{%s,%s},{%s,%s}",
                            yDecFormat.format(y),
                            xDecFormat.format(x),
                            yDecFormat.format(height),
                            xDecFormat.format(width)));
                }
            } else if (op instanceof Scale) {
                // kdu_expand is not capable of arbitrary scaling, but it does
                // offer a -reduce argument which is capable of downscaling by
                // factors of 2, significantly speeding decompression. We can
                // use it if the scale mode is ASPECT_FIT_* and either the
                // percent is <=50, or the height/width are <=50% of full size.
                final Scale scale = (Scale) op;
                final Dimension tileSize = getCroppedSize(opList, imageSize);
                if (!ignoreCrop) {
                    reduction.factor = scale.getReductionFactor(
                            tileSize, MAX_REDUCTION_FACTOR).factor;
                    if (reduction.factor > 0) {
                        command.add("-reduce");
                        command.add(reduction.factor + "");
                    } else if (reduction.factor < 0) {
                        // Don't allow a negative factor because kdu_expand
                        // can only reduce, not enlarge.
                        reduction.factor = 0;
                    }
                }
            }
        }

        command.add("-o");
        command.add(stdoutSymlink.toString());

        return new ProcessBuilder(command);
    }

}
