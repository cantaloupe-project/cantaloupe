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
import edu.illinois.library.cantaloupe.resolver.InputStreamStreamSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
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
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Processor using the Kakadu kdu_expand and kdu_jp2info command-line
 * tools. Written against version 7.7, but should work with other versions,
 * as long as their command-line interface is compatible. (There is also a JNI
 * binding available for Kakadu, but the author does not have access to the
 * Kakadu SDK.</p>
 *
 * <p>kdu_expand is used for cropping and an initial scale reduction factor,
 * and Java 2D for all remaining processing steps. kdu_expand generates TIFF
 * output which is streamed (more or less) directly to the ImageIO reader.
 * (TIFF is used in order to preserve embedded ICC profiles.)</p>
 *
 * <p>kdu_expand reads and writes the files named in the <code>-i</code>
 * and <code>-o</code> flags passed to it, respectively. The file in the
 * <code>-o</code> flag must have a recognized image extension such as .bmp,
 * .tif, etc. This means that it's not possible to natively write into a
 * {@link ProcessBuilder} {@link InputStream}. Instead, we have to resort to
 * a trick whereby we create a symlink from /tmp/whatever.tif to /dev/stdout
 * (which only exists on Unix), which will enable us to accomplish this.
 * The temporary symlink is created in the static initializer and deleted on
 * exit.</p>
 *
 * @see <a href="http://kakadusoftware.com/wp-content/uploads/2014/06/Usage_Examples-v7_7.txt">
 *     Usage Examples for the Demonstration Applications Supplied with Kakadu
 *     V7.7</a>
 */
class KakaduProcessor extends AbstractJava2DProcessor implements FileProcessor {

    private static final Logger LOGGER = LoggerFactory.
            getLogger(KakaduProcessor.class);

    private static final short MAX_REDUCTION_FACTOR = 5;

    /** Set by {@link #initialize()} */
    private static final AtomicBoolean initializationAttempted =
            new AtomicBoolean(false);
    /** Set by {@link #initialize()} */
    private static InitializationException initializationException;
    private static Path stdoutSymlink;

    /** will cache the output of kdu_jp2info */
    private Document infoDocument;

    /**
     * Creates a unique symlink to /dev/stdout in a temporary directory, and
     * sets it to delete on exit.
     */
    private static void createStdoutSymlink() throws IOException {
        Path tempDir = Application.getTempPath();

        final Path link = tempDir.resolve("cantaloupe-" +
                KakaduProcessor.class.getSimpleName() + "-" +
                UUID.randomUUID() + ".tif");
        final Path devStdout = Paths.get("/dev/stdout");

        stdoutSymlink = Files.createSymbolicLink(link, devStdout);
        stdoutSymlink.toFile().deleteOnExit();
    }

    /**
     * @param binaryName Name of one of the kdu_* binaries
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
            // Check for the presence of kdu_jp2info.
            invoke("kdu_jp2info");
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
                logWindowsIncompatibilityError();
            }
        } catch (IOException e) {
            initializationException = new InitializationException(e);
        }
    }

    private static void logWindowsIncompatibilityError() {
        if (SystemUtils.IS_OS_WINDOWS) {
            LOGGER.error(KakaduProcessor.class.getSimpleName() +
                    " doesn't currently work in Windows. It will work (with " +
                    "impaired performance) in version 4.");
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
     * kdu_expand's -reduce argument.
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
     * Gets the size of the given image by parsing the XML output of
     * kdu_jp2info.
     */
    @Override
    public Info readImageInfo() throws IOException {
        logWindowsIncompatibilityError();
        try {
            if (infoDocument == null) {
                readImageInfoDocument();
            }
            // Run an XPath query to find the width and height
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("//codestream/width");
            final int width = (int) Math.round((double) expr.evaluate(
                    infoDocument, XPathConstants.NUMBER));
            expr = xpath.compile("//codestream/height");
            final int height = (int) Math.round((double) expr.evaluate(
                    infoDocument, XPathConstants.NUMBER));

            final Info info = new Info(width, height,
                    getSourceFormat());

            // Run another XPath query to find tile sizes
            expr = xpath.compile("//codestream/SIZ");
            String result = (String) expr.evaluate(infoDocument,
                    XPathConstants.STRING);
            // Read the tile dimensions out of the Stiles={n,n} line
            try (final Scanner scan = new Scanner(result)) {
                while (scan.hasNextLine()) {
                    String line = scan.nextLine().trim();
                    if (line.startsWith("Stiles=")) {
                        String[] parts = StringUtils.split(line, ",");
                        if (parts.length == 2) {
                            final int dim1 = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                            final int dim2 = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                            int tileWidth, tileHeight;
                            if (width > height) {
                                tileWidth = Math.max(dim1, dim2);
                                tileHeight = Math.min(dim1, dim2);
                            } else {
                                tileWidth = Math.min(dim1, dim2);
                                tileHeight = Math.max(dim1, dim2);
                            }
                            info.getImages().get(0).tileWidth = tileWidth;
                            info.getImages().get(0).tileHeight = tileHeight;
                        }
                    }
                }
            }
            return info;
        } catch (SAXException | ParserConfigurationException |
                XPathExpressionException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Executes kdu_jp2info and parses the output into a Document object,
     * saved in an instance variable.
     */
    private void readImageInfoDocument()
            throws SAXException, IOException, ParserConfigurationException {
        final List<String> command = new ArrayList<>();
        command.add(getPath("kdu_jp2info"));
        command.add("-i");
        command.add(sourceFile.toString());
        command.add("-siz");

        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        LOGGER.info("Invoking {}", String.join(" ", pb.command()));
        Process process = pb.start();
        ByteArrayOutputStream outputBucket = new ByteArrayOutputStream();

        try (InputStream processInputStream =
                     new BufferedInputStream(process.getInputStream())) {
            IOUtils.copy(processInputStream, outputBucket);
            // This will be an XML string if all went well, otherwise it will
            // be non-XML text.
            final String kduOutput = toString(outputBucket).trim();

            // A typical error message looks like:
            // -------------
            // Kakadu Error:
            // Input file is neither a raw codestream nor a box-structured file.  Not a
            // JPEG2000 file.
            if (kduOutput.startsWith("--")) {
                final String kduMessage =
                        kduOutput.substring(kduOutput.lastIndexOf("Kakadu Error:") + 13).
                                replace("\n", " ").trim();
                throw new IOException("Failed to read the source file. " +
                        "(kdu_jp2info output: " + kduMessage + ")");
            } else {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                infoDocument = db.parse(new InputSource(new StringReader(kduOutput)));
            }
        }
    }

    @Override
    public void process(final OperationList opList,
                        final Info imageInfo,
                        final OutputStream outputStream) throws ProcessorException {
        logWindowsIncompatibilityError();
        super.process(opList, imageInfo, outputStream);

        // Will receive stderr output from kdu_expand.
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ReductionFactor reductionFactor = new ReductionFactor();

            // If we are normalizing, we need to read the entire image region.
            final boolean normalize = (opList.getFirst(Normalize.class) != null);

            final ProcessBuilder pb = getProcessBuilder(
                    opList, imageInfo.getSize(), reductionFactor, normalize);
            LOGGER.debug("Invoking {}", String.join(" ", pb.command()));
            final Process process = pb.start();

            try (final InputStream processInputStream =
                         new BufferedInputStream(process.getInputStream());
                 final InputStream processErrorStream = process.getErrorStream()) {
                ThreadPool.getInstance().submit(
                        new StreamCopier(processErrorStream, errorBucket));

                final ImageReader reader = new ImageReader(
                        new InputStreamStreamSource(processInputStream),
                        Format.TIF);
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

    @Override
    public void setSourceFile(Path sourceFile) {
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
     * @return kdu_expand command invocation string
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
                    // N.B.: this broke sometime between KDU v7.6 and v7.10.4,
                    // and kdu_expand now unpredictably returns an extra pixel.
                    // Too bad, but Java2DUtil.crop() will take care of it.
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

    private void reset() {
        infoDocument = null;
    }

}
