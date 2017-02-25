package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.ThreadPool;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resolver.InputStreamStreamSource;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

/**
 * <p>Processor using the OpenJPEG opj_decompress and opj_dump command-line
 * tools. Written against version 2.1.0, but should work with other versions,
 * as long as their command-line interface is compatible. (There is also a JNI
 * binding available, but it is broken as of this writing.)</p>
 *
 * <p>opj_decompress is used for cropping and an initial scale reduction
 * factor. (Java 2D is used for all remaining processing steps.)
 * opj_decompress generates BMP output which is streamed to an ImageIO reader.
 * (BMP does not support embedded ICC profiles, but this is not a problem
 * because opj_decompress converts the RGB source data itself.)</p>
 *
 * <p>opj_decompress reads and writes the files named in the <code>-i</code>
 * and <code>-o</code> flags passed to it, respectively. The file in the
 * <code>-o</code> flag must have a recognized image extension such as .bmp,
 * .tif, etc. This means that it's not possible to natively write to a
 * {@link ProcessBuilder} {@link InputStream}. Instead, we have to resort to
 * a trick whereby we create a symlink from /tmp/whatever.bmp to /dev/stdout
 * (which only exists on Unix), which will enable us to accomplish this.
 * The temporary symlink is created in the static initializer and deleted on
 * exit.</p>
 */
class OpenJpegProcessor extends AbstractJava2DProcessor
        implements FileProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(OpenJpegProcessor.class);

    private static final String PATH_TO_BINARIES_CONFIG_KEY =
            "OpenJpegProcessor.path_to_binaries";

    private static final short MAX_REDUCTION_FACTOR = 5;

    private static Path stdoutSymlink;

    // will cache opj_dump output
    private String imageInfo;

    static {
        // Due to a quirk of opj_decompress, this processor requires access to
        // /dev/stdout.
        final File devStdout = new File("/dev/stdout");
        if (devStdout.exists() && devStdout.canWrite()) {
            // Due to another quirk of opj_decompress, we need to create a
            // symlink from {temp path}/stdout.bmp to /dev/stdout, to tell
            // opj_decompress what format to write.
            try {
                stdoutSymlink = createStdoutSymlink();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            logger.error("Sorry, but OpenJpegProcessor won't work on this " +
                    "platform as it requires access to /dev/stdout.");
        }
    }

    /**
     * Creates a unique symlink to /dev/stdout in a temporary directory, and
     * sets it to delete on exit.
     *
     * @return Path to the symlink.
     * @throws IOException
     */
    private static Path createStdoutSymlink() throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        final File link = new File(tempDir.getAbsolutePath() + "/cantaloupe-" +
                UUID.randomUUID() + ".bmp");
        link.deleteOnExit();
        final File devStdout = new File("/dev/stdout");
        return Files.createSymbolicLink(Paths.get(link.getAbsolutePath()),
                Paths.get(devStdout.getAbsolutePath()));
    }

    /**
     * @param binaryName Name of one of the opj_* binaries
     * @return
     */
    private static String getPath(String binaryName) {
        String path = Configuration.getInstance().
                getString(PATH_TO_BINARIES_CONFIG_KEY);
        if (path != null && path.length() > 0) {
            path = StringUtils.stripEnd(path, File.separator) +
                    File.separator + binaryName;
        } else {
            path = binaryName;
        }
        return path;
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        final Set<Format> outputFormats = new HashSet<>();
        if (format == Format.JP2) {
            outputFormats.addAll(ImageWriter.supportedFormats());
        }
        return outputFormats;
    }

    /**
     * Computes the effective size of an image after all crop operations are
     * applied but excluding any scale operations, in order to use
     * opj_decompress' -r (reduce) argument.
     *
     * @param opList
     * @param fullSize
     * @return
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

    /**
     * Gets the size of the given image by parsing the output of opj_dump.
     *
     * @return
     * @throws ProcessorException
     */
    @Override
    public Info readImageInfo() throws ProcessorException {
        try {
            if (imageInfo == null) {
                doReadImageInfo();
            }
            final Info.Image image = new Info.Image();

            try (final Scanner scan = new Scanner(imageInfo)) {
                while (scan.hasNextLine()) {
                    String line = scan.nextLine().trim();
                    if (line.startsWith("x1=")) {
                        String[] parts = StringUtils.split(line, ",");
                        for (int i = 0; i < 2; i++) {
                            String[] kv = StringUtils.split(parts[i], "=");
                            if (kv.length == 2) {
                                if (i == 0) {
                                    image.width = Integer.parseInt(kv[1].trim());
                                } else {
                                    image.height = Integer.parseInt(kv[1].trim());
                                }
                            }
                        }
                    } else if (line.startsWith("tdx=")) {
                        String[] parts = StringUtils.split(line, ",");
                        if (parts.length == 2) {
                            image.tileWidth = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                            image.tileHeight = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                        }
                    }
                }
            }
            final Info info = new Info();
            info.setSourceFormat(getSourceFormat());
            info.getImages().add(image);
            return info;
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    private void doReadImageInfo() throws IOException {
        final List<String> command = new ArrayList<>();
        command.add(getPath("opj_dump"));
        command.add("-i");
        command.add(sourceFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
        Process process = pb.start();

        try (InputStream processInputStream = process.getInputStream()) {
            String opjOutput = IOUtils.toString(processInputStream, "UTF-8");

            // A typical error message looks like:
            // [ERROR] Unknown input file format: /path/to/file.jp2
            // Known file formats are *.j2k, *.jp2, *.jpc or *.jpt
            if (opjOutput.startsWith("[ERROR]")) {
                final String opjMessage =
                        opjOutput.substring(opjOutput.lastIndexOf("ERROR]") + 7,
                                opjOutput.indexOf("\n")).trim();
                throw new IOException("Failed to read the source file. " +
                        "(opj_dump says: " + opjMessage + ")");
            } else {
                imageInfo = opjOutput;
            }
        }
    }

    @Override
    public void process(final OperationList opList,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(opList, imageInfo, outputStream);

        // will receive stderr output from kdu_expand
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ReductionFactor reductionFactor = new ReductionFactor();

            // If we are normalizing, we need to read the entire image region.
            final boolean normalize = (boolean) opList.getOptions().
                    getOrDefault(NORMALIZE_CONFIG_KEY, false);

            final ProcessBuilder pb = getProcessBuilder(
                    opList, imageInfo.getSize(), reductionFactor, normalize);
            logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            try (final InputStream processInputStream =
                         new BufferedInputStream(process.getInputStream());
                 final InputStream processErrorStream = process.getErrorStream()) {
                ThreadPool.getInstance().submit(
                        new StreamCopier(processErrorStream, errorBucket));

                final ImageReader reader = new ImageReader(
                        new InputStreamStreamSource(processInputStream),
                        Format.BMP);
                final BufferedImage image = reader.read();
                try {
                    Set<ImageReader.Hint> hints = new HashSet<>();
                    if (!normalize) {
                        hints.add(ImageReader.Hint.ALREADY_CROPPED);
                    }
                    postProcess(image, hints, opList, imageInfo,
                            reductionFactor, normalize, outputStream);
                    final int code = process.waitFor();
                    if (code != 0) {
                        logger.warn("opj_decompress returned with code {}", code);
                        final String errorStr = errorBucket.toString();
                        if (errorStr != null && errorStr.length() > 0) {
                            throw new ProcessorException(errorStr);
                        }
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
            logger.warn(msg, e);
            throw new ProcessorException(msg, e);
        } catch (IOException | InterruptedException e) {
            String msg = e.getMessage();
            final String errorStr = errorBucket.toString();
            if (errorStr != null && errorStr.length() > 0) {
                msg += " (command output: " + errorStr + ")";
            }
            throw new ProcessorException(msg, e);
        }
    }

    @Override
    public void setSourceFile(File sourceFile) {
        super.setSourceFile(sourceFile);
        reset();
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
    private ProcessBuilder getProcessBuilder(final OperationList opList,
                                             final Dimension imageSize,
                                             final ReductionFactor reduction,
                                             final boolean ignoreCrop) {
        final List<String> command = new ArrayList<>();
        command.add(getPath("opj_decompress"));
        command.add("-i");
        command.add(sourceFile.getAbsolutePath());

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
                    }
                }
            }
        }

        command.add("-o");
        command.add(stdoutSymlink.toString());

        return new ProcessBuilder(command);
    }

    private void reset() {
        imageInfo = null;
    }

}
