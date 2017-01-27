package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
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
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
class KakaduProcessor extends AbstractJava2dProcessor implements FileProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(KakaduProcessor.class);

    private static final String NORMALIZE_CONFIG_KEY =
            "KakaduProcessor.normalize";
    private static final String PATH_TO_BINARIES_CONFIG_KEY =
            "KakaduProcessor.path_to_binaries";

    private static final short MAX_REDUCTION_FACTOR = 5;

    private static final ExecutorService executorService =
            Executors.newCachedThreadPool();

    private static Path stdoutSymlink;

    /** will cache the output of kdu_jp2info */
    private Document infoDocument;

    static {
        // Due to a quirk of kdu_expand, this processor requires access to
        // /dev/stdout.
        final File devStdout = new File("/dev/stdout");
        if (devStdout.exists() && devStdout.canWrite()) {
            // Due to another quirk of kdu_expand, we need to create a symlink
            // from {temp path}/stdout.tif to /dev/stdout, to tell kdu_expand
            // what format to write.
            try {
                stdoutSymlink = createStdoutSymlink();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            logger.error("Sorry, but KakaduProcessor won't work on this " +
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
                UUID.randomUUID() + ".tif");
        link.deleteOnExit();
        final File devStdout = new File("/dev/stdout");
        return Files.createSymbolicLink(Paths.get(link.getAbsolutePath()),
                Paths.get(devStdout.getAbsolutePath()));
    }

    /**
     * @param binaryName Name of one of the kdu_* binaries
     * @return
     */
    private static String getPath(String binaryName) {
        String path = ConfigurationFactory.getInstance().
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
     * kdu_expand's -reduce argument.
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
     * Gets the size of the given image by parsing the XML output of
     * kdu_jp2info.
     *
     * @return
     * @throws ProcessorException
     */
    @Override
    public Info readImageInfo() throws ProcessorException {
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
            try(final Scanner scan = new Scanner(result)) {
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
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * Executes kdu_jp2info and parses the output into a Document object,
     * saved in an instance variable.
     *
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private void readImageInfoDocument()
            throws SAXException, IOException, ParserConfigurationException {
        final List<String> command = new ArrayList<>();
        command.add(getPath("kdu_jp2info"));
        command.add("-i");
        command.add(sourceFile.getAbsolutePath());
        command.add("-siz");

        final ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
        Process process = pb.start();
        ByteArrayOutputStream outputBucket = new ByteArrayOutputStream();

        try (InputStream processInputStream = process.getInputStream()) {
            IOUtils.copy(processInputStream, outputBucket);
            // This will be an XML string if all went well, otherwise it will
            // be non-XML text.
            final String kduOutput = outputBucket.toString("UTF-8").trim();

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
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(opList, imageInfo, outputStream);

        // will receive stderr output from kdu_expand
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ReductionFactor reductionFactor = new ReductionFactor();

            final Configuration config = ConfigurationFactory.getInstance();
            // If we are normalizing, we need to read the entire image region.
            final boolean normalize =
                    config.getBoolean(NORMALIZE_CONFIG_KEY, false);

            final ProcessBuilder pb = getProcessBuilder(
                    opList, imageInfo.getSize(), reductionFactor, normalize);
            logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            try (final InputStream processInputStream =
                         new BufferedInputStream(process.getInputStream());
                 final InputStream processErrorStream = process.getErrorStream()) {
                executorService.submit(
                        new StreamCopier(processErrorStream, errorBucket));

                final ImageReader reader = new ImageReader(
                        new InputStreamStreamSource(processInputStream),
                        Format.TIF);
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
                        logger.warn("kdu_expand returned with code {}", code);
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
            // This will generally not have a message.
            String msg = "process(): EOFException";
            logger.error(msg, e);
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
        command.add(sourceFile.getAbsolutePath());

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
