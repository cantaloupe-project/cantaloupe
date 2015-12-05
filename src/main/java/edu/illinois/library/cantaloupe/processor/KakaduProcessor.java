package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import info.freelibrary.djatoka.io.PNMImage;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Processor using the Kakadu kdu_expand and kdu_jp2info command-line tools.
 *
 * @see <a href="http://kakadusoftware.com/wp-content/uploads/2014/06/Usage_Examples-v7_7.txt">
 *     Usage Examples for the Demonstration Applications Supplied with Kakadu
 *     V7.7</a>
 */
class KakaduProcessor implements FileProcessor {

    private enum PostProcessor {
        JAI, JAVA2D
    }

    private class StreamCopier implements Runnable {

        private final InputStream inputStream;
        private final OutputStream outputStream;

        public StreamCopier(InputStream is, OutputStream os) {
            inputStream = is;
            outputStream = os;
        }

        public void run() {
            try {
                IOUtils.copy(inputStream, outputStream);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

    }

    private static Logger logger = LoggerFactory.getLogger(KakaduProcessor.class);

    private static final short MAX_REDUCTION_FACTOR = 5;
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();
    private static PostProcessor postProcessor;

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.BITONAL);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.COLOR);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.GRAY);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.NATIVE);

        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.BITONAL);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.COLOR);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.DEFAULT);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.GRAY);

        SUPPORTED_FEATURES.add(ProcessorFeature.MIRRORING);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PERCENT);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PIXELS);
        SUPPORTED_FEATURES.add(ProcessorFeature.ROTATION_ARBITRARY);
        SUPPORTED_FEATURES.add(ProcessorFeature.ROTATION_BY_90S);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_ABOVE_FULL);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_HEIGHT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_PERCENT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);

        if (Application.getConfiguration().
                getString("KakaduProcessor.post_processor", "java2d").
                toLowerCase().equals("jai")) {
            postProcessor = PostProcessor.JAI;
            logger.info("Will post-process using JAI");
        } else {
            postProcessor = PostProcessor.JAVA2D;
            logger.info("Will post-process using Java2D");
        }
    }

    /**
     * @param binaryName Name of one of the kdu_* binaries
     * @return
     */
    private static String getPath(String binaryName) {
        String path = Application.getConfiguration().
                getString("KakaduProcessor.path_to_binaries");
        if (path != null) {
            path = StringUtils.stripEnd(path, File.separator) + File.separator +
                    binaryName;
        } else {
            path = binaryName;
        }
        return path;
    }

    private static String getStdoutSymlinkPath() {
        return StringUtils.stripEnd(
                Application.getConfiguration().getString("KakaduProcessor.path_to_stdout_symlink"),
                File.separator);
    }

    /**
     * Quotes command-line parameters with spaces.
     *
     * @param path
     * @return
     */
    private static String quote(String path) {
        if (path.contains(" ")) {
            path = "\"" + path + "\"";
        }
        return path;
    }

    @Override
    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> outputFormats = new HashSet<>();
        if (sourceFormat == SourceFormat.JP2) {
            outputFormats.addAll(ProcessorUtil.imageIoOutputFormats());
        }
        return outputFormats;
    }

    /**
     * Gets the size of the given image by parsing the XML output of
     * kdu_jp2info.
     *
     * @param inputFile Source image
     * @param sourceFormat Format of the source image
     * @return
     * @throws ProcessorException
     */
    @Override
    public Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException {
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        }
        final List<String> command = new ArrayList<>();
        command.add(getPath("kdu_jp2info"));
        command.add("-i");
        command.add(inputFile.getAbsolutePath());
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(process.getInputStream());

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("//codestream/width");
            int width = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));
            expr = xpath.compile("//codestream/height");
            int height = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));
            return new Dimension(width, height);
        } catch (SAXException e) {
            throw new ProcessorException("Failed to parse XML. Command: " +
                    StringUtils.join(command, " "), e);
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    @Override
    public Set<ProcessorFeature> getSupportedFeatures(SourceFormat sourceFormat) {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
    getSupportedIiif1_1Qualities(final SourceFormat sourceFormat) {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
    getSupportedIiif2_0Qualities(final SourceFormat sourceFormat) {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_2_0_QUALITIES);
        }
        return qualities;
    }

    @Override
    public void process(OperationList ops, SourceFormat sourceFormat,
                        Dimension fullSize, File inputFile,
                        OutputStream outputStream) throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        final ByteArrayOutputStream outputBucket = new ByteArrayOutputStream();
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ReductionFactor reduction = new ReductionFactor();
            final ProcessBuilder pb = getProcessBuilder(inputFile, ops,
                    fullSize, reduction);
            final Process process = pb.start();

            new Thread(new StreamCopier(process.getInputStream(), outputBucket)).start();
            new Thread(new StreamCopier(process.getErrorStream(), errorBucket)).start();

            try {
                int code = process.waitFor();
                if (code != 0) {
                    logger.warn("kdu_expand returned with code {}", code);
                    final String errorStr = errorBucket.toString();
                    if (errorStr != null && errorStr.length() > 0) {
                        throw new ProcessorException(errorStr);
                    }
                }
                final ByteArrayInputStream bais = new ByteArrayInputStream(
                        outputBucket.toByteArray());
                if (postProcessor == PostProcessor.JAI) {
                    RenderedOp image = (RenderedOp) PlanarImage.wrapRenderedImage(
                            new PNMImage(bais).getBufferedImage());
                    for (Operation op : ops) {
                        if (op instanceof Scale) {
                            image = ProcessorUtil.scaleImage(image, (Scale) op,
                                    reduction.factor);
                        } else if (op instanceof Transpose) {
                            image = ProcessorUtil.transposeImage(image,
                                    (Transpose) op);
                        } else if (op instanceof Rotate) {
                            image = ProcessorUtil.rotateImage(image,
                                    (Rotate) op);
                        } else if (op instanceof Filter) {
                            image = ProcessorUtil.filterImage(image,
                                    (Filter) op);
                        }
                    }
                    ImageIO.write(image, ops.getOutputFormat().getExtension(),
                            outputStream);
                } else {
                    BufferedImage image = new PNMImage(bais).getBufferedImage();
                    for (Operation op : ops) {
                        if (op instanceof Scale) {
                            image = ProcessorUtil.scaleImageWithG2d(image,
                                    (Scale) op, reduction.factor);
                        } else if (op instanceof Transpose) {
                            image = ProcessorUtil.transposeImage(image,
                                    (Transpose) op);
                        } else if (op instanceof Rotate) {
                            image = ProcessorUtil.rotateImage(image,
                                    (Rotate) op);
                        } else if (op instanceof Filter) {
                            image = ProcessorUtil.filterImage(image,
                                    (Filter) op);
                        }
                    }
                    ProcessorUtil.writeImage(image, ops.getOutputFormat(),
                            outputStream);
                    image.flush();
                }
            } finally {
                process.getInputStream().close();
                process.getOutputStream().close();
                process.getErrorStream().close();
                process.destroy();
            }
        } catch (IOException | InterruptedException e) {
            String msg = e.getMessage();
            final String errorStr = errorBucket.toString();
            if (errorStr != null && errorStr.length() > 0) {
                msg += " (command output: " + msg + ")";
            }
            throw new ProcessorException(msg, e);
        }
    }

    /**
     * Gets a ProcessBuilder corresponding to the given parameters.
     *
     * @param inputFile
     * @param ops
     * @param fullSize The full size of the source image
     * @param reduction Modified by reference
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(File inputFile, OperationList ops,
                                             Dimension fullSize,
                                             ReductionFactor reduction) {
        final List<String> command = new ArrayList<>();
        command.add(getPath("kdu_expand"));
        command.add("-quiet");
        command.add("-no_alpha");
        command.add("-i");
        command.add(inputFile.getAbsolutePath());

        for (Operation op : ops) {
            if (op instanceof Crop) {
                final Crop crop = (Crop) op;
                if (!crop.isFull()) {
                    final double x = crop.getX() / fullSize.width;
                    final double y = crop.getY() / fullSize.height;
                    final double width = crop.getWidth() / fullSize.width;
                    final double height = crop.getHeight() / fullSize.height;
                    command.add("-region");
                    command.add(String.format("{%.7f,%.7f},{%.7f,%.7f}",
                            y, x, height, width));
                }
            } else if (op instanceof Scale) {
                // kdu_expand is not capable of arbitrary scaling, but it does
                // offer a -reduce option which is capable of downscaling by
                // factors of 2, significantly speeding decompression. We can
                // use it if the scale mode is ASPECT_FIT_* and either the
                // percent is <=50, or the height/width are <=50% of full size.
                // The smaller the scale, the bigger the win.
                final Scale scale = (Scale) op;
                if (scale.getMode() != Scale.Mode.FULL) {
                    if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        double hvScale = (double) scale.getWidth() /
                                (double) fullSize.width;
                        reduction.factor = ProcessorUtil.getReductionFactor(
                                hvScale, MAX_REDUCTION_FACTOR);
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        double hvScale = (double) scale.getHeight() /
                                (double) fullSize.height;
                        reduction.factor = ProcessorUtil.getReductionFactor(
                                hvScale, MAX_REDUCTION_FACTOR);
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        double hScale = (double) scale.getWidth() /
                                (double) fullSize.width;
                        double vScale = (double) scale.getHeight() /
                                (double) fullSize.height;
                        reduction.factor = ProcessorUtil.getReductionFactor(
                                Math.min(hScale, vScale), MAX_REDUCTION_FACTOR);
                    } else if (scale.getPercent() != null) {
                        reduction.factor = ProcessorUtil.getReductionFactor(
                                scale.getPercent(), MAX_REDUCTION_FACTOR);
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
        command.add(quote(getStdoutSymlinkPath()));

        return new ProcessBuilder(command);
    }

}
