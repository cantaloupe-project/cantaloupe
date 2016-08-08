package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Color;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Sharpen;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.redaction.Redaction;
import edu.illinois.library.cantaloupe.image.watermark.Watermark;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resolver.InputStreamStreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
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
import java.util.Arrays;
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
 * as long as their command-line interface is compatible.</p>
 *
 * <p>kdu_expand is used for cropping and an initial scale reduction factor,
 * and Java 2D for all remaining processing steps. kdu_expand generates TIFF
 * output which is streamed (more or less) directly to the ImageIO reader.
 * (TIFF is used in order to preserve any embedded source ICC profile.)</p>
 *
 * @see <a href="http://kakadusoftware.com/wp-content/uploads/2014/06/Usage_Examples-v7_7.txt">
 *     Usage Examples for the Demonstration Applications Supplied with Kakadu
 *     V7.7</a>
 */
class KakaduProcessor extends AbstractProcessor  implements FileProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(KakaduProcessor.class);

    static final String DOWNSCALE_FILTER_CONFIG_KEY =
            "KakaduProcessor.downscale_filter";
    static final String PATH_TO_BINARIES_CONFIG_KEY =
            "KakaduProcessor.path_to_binaries";
    static final String SHARPEN_CONFIG_KEY = "KakaduProcessor.sharpen";
    static final String UPSCALE_FILTER_CONFIG_KEY =
            "KakaduProcessor.upscale_filter";

    private static final short MAX_REDUCTION_FACTOR = 5;
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

    private static final ExecutorService executorService =
            Executors.newCachedThreadPool();

    private static Path stdoutSymlink;

    /** will cache the output of kdu_jp2info */
    private Document infoDocument;
    private File sourceFile;

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));
        SUPPORTED_IIIF_2_0_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY));
        SUPPORTED_FEATURES.addAll(Arrays.asList(
                ProcessorFeature.MIRRORING,
                ProcessorFeature.REGION_BY_PERCENT,
                ProcessorFeature.REGION_BY_PIXELS,
                ProcessorFeature.REGION_SQUARE,
                ProcessorFeature.ROTATION_ARBITRARY,
                ProcessorFeature.ROTATION_BY_90S,
                ProcessorFeature.SIZE_ABOVE_FULL,
                ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_HEIGHT,
                ProcessorFeature.SIZE_BY_PERCENT,
                ProcessorFeature.SIZE_BY_WIDTH,
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));

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

    Scale.Filter getDownscaleFilter() {
        final String upscaleFilterStr = Configuration.getInstance().
                getString(DOWNSCALE_FILTER_CONFIG_KEY);
        try {
            return Scale.Filter.valueOf(upscaleFilterStr.toUpperCase());
        } catch (Exception e) {
            logger.warn("Invalid value for {}", DOWNSCALE_FILTER_CONFIG_KEY);
        }
        return null;
    }

    Scale.Filter getUpscaleFilter() {
        final String upscaleFilterStr = Configuration.getInstance().
                getString(UPSCALE_FILTER_CONFIG_KEY);
        try {
            return Scale.Filter.valueOf(upscaleFilterStr.toUpperCase());
        } catch (Exception e) {
            logger.warn("Invalid value for {}", UPSCALE_FILTER_CONFIG_KEY);
        }
        return null;
    }

    /**
     * Gets the size of the given image by parsing the XML output of
     * kdu_jp2info.
     *
     * @return
     * @throws ProcessorException
     */
    @Override
    public ImageInfo getImageInfo() throws ProcessorException {
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

            final ImageInfo info = new ImageInfo(width, height,
                    getSourceFormat());

            // Run another XPath query to find tile sizes
            expr = xpath.compile("//codestream/SIZ");
            String result = (String) expr.evaluate(infoDocument,
                    XPathConstants.STRING);
            // Read the tile dimensions out of the Stiles={n,n} line
            final Scanner scan = new Scanner(result);
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
            throws SAXException, IOException,ParserConfigurationException {
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
            // Ideally we could just call
            // DocumentBuilder.parse(process.getInputStream()), but the XML
            // output of kdu_jp2info may contain leading whitespace that
            // causes a SAXParseException. So, read into a byte array in
            // order to trim it, and then parse that. TODO: use a FilterInputStream
            IOUtils.copy(processInputStream, outputBucket);
            final String xml = outputBucket.toString("UTF-8").trim();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            infoDocument = db.parse(new InputSource(new StringReader(xml)));
        }
    }

    @Override
    public File getSourceFile() {
        return this.sourceFile;
    }

    @Override
    public Set<ProcessorFeature> getSupportedFeatures() {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIiif1_1Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_2_0_QUALITIES);
        }
        return qualities;
    }

    @Override
    public void process(final OperationList ops,
                        final ImageInfo imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        if (!getAvailableOutputFormats().contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        // will receive stderr output from kdu_expand
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ReductionFactor reductionFactor = new ReductionFactor();
            final ProcessBuilder pb = getProcessBuilder(
                    ops, imageInfo.getSize(), reductionFactor);
            logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            try (final InputStream processInputStream = process.getInputStream();
                 final InputStream processErrorStream = process.getErrorStream()) {
                executorService.submit(
                        new StreamCopier(processErrorStream, errorBucket));

                final ImageReader reader = new ImageReader(
                        new InputStreamStreamSource(processInputStream),
                        Format.TIF);
                try {
                    postProcessUsingJava2d(reader, ops, imageInfo, reductionFactor,
                            outputStream);
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
            // This happens frequently in Tomcat, but appears to be harmless.
            logger.warn("EOFException: {}", e.getMessage());
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
        reset();
        this.sourceFile = sourceFile;
    }

    /**
     * Gets a ProcessBuilder corresponding to the given parameters.
     *
     * @param opList
     * @param imageSize The full size of the source image
     * @param reduction {@link ReductionFactor#factor} property modified by
     * reference
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(final OperationList opList,
                                             final Dimension imageSize,
                                             final ReductionFactor reduction) {
        final List<String> command = new ArrayList<>();
        command.add(getPath("kdu_expand"));
        command.add("-quiet");
        command.add("-resilient");
        command.add("-no_alpha");
        command.add("-i");
        command.add(sourceFile.getAbsolutePath());

        for (Operation op : opList) {
            if (op instanceof Crop) {
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
                // offer a -reduce option which is capable of downscaling by
                // factors of 2, significantly speeding decompression. We can
                // use it if the scale mode is ASPECT_FIT_* and either the
                // percent is <=50, or the height/width are <=50% of full size.
                final Scale scale = (Scale) op;
                final Dimension tileSize = getCroppedSize(opList, imageSize);
                if (scale.getMode() != Scale.Mode.FULL) {
                    if (scale.getPercent() != null) {
                        reduction.factor = ReductionFactor.forScale(
                                scale.getPercent(), MAX_REDUCTION_FACTOR).factor;
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        double hvScale = (double) scale.getWidth() /
                                (double) tileSize.width;
                        reduction.factor = ReductionFactor.forScale(
                                hvScale, MAX_REDUCTION_FACTOR).factor;
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        double hvScale = (double) scale.getHeight() /
                                (double) tileSize.height;
                        reduction.factor = ReductionFactor.forScale(
                                hvScale, MAX_REDUCTION_FACTOR).factor;
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        double hScale = (double) scale.getWidth() /
                                (double) tileSize.width;
                        double vScale = (double) scale.getHeight() /
                                (double) tileSize.height;
                        reduction.factor = ReductionFactor.forScale(
                                Math.min(hScale, vScale), MAX_REDUCTION_FACTOR).factor;
                    } else {
                        reduction.factor = 0;
                    }
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

    private void postProcessUsingJava2d(final ImageReader reader,
                                        final OperationList opList,
                                        final ImageInfo imageInfo,
                                        final ReductionFactor reductionFactor,
                                        final OutputStream outputStream)
            throws IOException, ProcessorException {
        BufferedImage image = reader.read();

        // The crop has already been applied, but we need to retain a
        // reference to it for any redactions.
        Crop crop = null;
        for (Operation op : opList) {
            if (op instanceof Crop) {
                crop = (Crop) op;
                break;
            }
        }

        // Redactions happen immediately after cropping.
        List<Redaction> redactions = new ArrayList<>();
        for (Operation op : opList) {
            if (op instanceof Redaction) {
                redactions.add((Redaction) op);
            }
        }
        image = Java2dUtil.applyRedactions(image, crop, reductionFactor,
                redactions);

        // Apply most remaining operations.
        for (Operation op : opList) {
            if (op instanceof Scale) {
                final Scale scale = (Scale) op;
                final Float upOrDown =
                        scale.getResultingScale(imageInfo.getSize());
                if (upOrDown != null) {
                    final Scale.Filter filter =
                            (upOrDown > 1) ?
                                    getUpscaleFilter() : getDownscaleFilter();
                    scale.setFilter(filter);
                }

                image = Java2dUtil.scaleImage(image, scale, reductionFactor);
            } else if (op instanceof Transpose) {
                image = Java2dUtil.transposeImage(image,
                        (Transpose) op);
            } else if (op instanceof Rotate) {
                image = Java2dUtil.rotateImage(image,
                        (Rotate) op);
            } else if (op instanceof Color) {
                image = Java2dUtil.transformColor(image,
                        (Color) op);
            }
        }

        // Apply the sharpen operation, if present.
        final Configuration config = Configuration.getInstance();
        final float sharpenValue = config.getFloat(SHARPEN_CONFIG_KEY, 0);
        final Sharpen sharpen = new Sharpen(sharpenValue);
        image = Java2dUtil.sharpenImage(image, sharpen);

        // Apply all remaining operations.
        for (Operation op : opList) {
            if (op instanceof Watermark) {
                try {
                    image = Java2dUtil.applyWatermark(image, (Watermark) op);
                } catch (ConfigurationException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        new ImageWriter(opList).
                write(image, opList.getOutputFormat(), outputStream);
        image.flush();
    }

    private void reset() {
        infoDocument = null;
    }

}
