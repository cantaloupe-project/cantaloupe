package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;

import edu.illinois.library.cantaloupe.request.Size;
import info.freelibrary.djatoka.io.PNMImage;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
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

    /**
     * Used to return a reduction factor from getProcessBuilder() by reference.
     */
    private class ReductionFactor {
        public short factor = 0;
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
    private static final Set<Quality> SUPPORTED_QUALITIES = new HashSet<>();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();

    static {
        SUPPORTED_QUALITIES.add(Quality.BITONAL);
        SUPPORTED_QUALITIES.add(Quality.COLOR);
        SUPPORTED_QUALITIES.add(Quality.DEFAULT);
        SUPPORTED_QUALITIES.add(Quality.GRAY);

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
    public Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException {
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

    public Set<ProcessorFeature> getSupportedFeatures(
            final SourceFormat sourceFormat) {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    public Set<Quality> getSupportedQualities(final SourceFormat sourceFormat) {
        Set<Quality> qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_QUALITIES);
        }
        return qualities;
    }

    public void process(final Parameters params, final SourceFormat sourceFormat,
                        final File inputFile, final OutputStream outputStream)
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(params.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }
        try {
            final Dimension fullSize = getSize(inputFile, sourceFormat);
            final ReductionFactor reduction = new ReductionFactor();
            final ProcessBuilder pb = getProcessBuilder(inputFile, params,
                    fullSize, reduction);
            final Process process = pb.start();

            final ByteArrayOutputStream outputBucket = new ByteArrayOutputStream();
            final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
            new Thread(new StreamCopier(process.getInputStream(), outputBucket)).start();
            new Thread(new StreamCopier(process.getErrorStream(), errorBucket)).start();

            try {
                process.waitFor();
                final String errorStr = errorBucket.toString();
                if (errorStr != null && errorStr.length() > 0) {
                    throw new ProcessorException(errorStr);
                }
                final ByteArrayInputStream bais = new ByteArrayInputStream(
                        outputBucket.toByteArray());
                BufferedImage image = new PNMImage(bais).getBufferedImage();
                image = scaleImageWithG2d(image, params.getSize(), reduction);
                image = ProcessorUtil.rotateImage(image, params.getRotation());
                image = ProcessorUtil.filterImage(image, params.getQuality());
                ProcessorUtil.outputImage(image, params.getOutputFormat(),
                        outputStream);
                image.flush();
            } finally {
                process.getInputStream().close();
                process.getOutputStream().close();
                process.getErrorStream().close();
                process.destroy();
            }
        } catch (IOException | InterruptedException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * Gets a ProcessBuilder corresponding to the given parameters.
     *
     * @param inputFile
     * @param params
     * @param fullSize The full size of the source image
     * @param reduction Modified by reference
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(File inputFile, Parameters params,
                                             Dimension fullSize,
                                             ReductionFactor reduction) {
        final List<String> command = new ArrayList<>();
        command.add(getPath("kdu_expand"));
        command.add("-quiet");
        command.add("-no_alpha");
        command.add("-i");
        command.add(inputFile.getAbsolutePath());

        final Region region = params.getRegion();
        if (!region.isFull()) {
            final double x = region.getX() / fullSize.width;
            final double y = region.getY() / fullSize.height;
            final double width = region.getWidth() / fullSize.width;
            final double height = region.getHeight() / fullSize.height;
            command.add("-region");
            command.add(String.format("{%.7f,%.7f},{%.7f,%.7f}",
                    y, x, height, width));
        }

        // kdu_expand is not capable of arbitrary scaling, but it does offer a
        // -reduce option which is capable of downscaling by factors of 2,
        // significantly speeding decompression. We can use it if the scale mode
        // is ASPECT_FIT_* and either the percent is <=50, or the height/width
        // are <=50% of full size.
        final Size size = params.getSize();
        if (size.getScaleMode() != Size.ScaleMode.FULL) {
            if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_WIDTH) {
                double scale = (double) size.getWidth() /
                        (double) fullSize.width;
                reduction.factor = getReductionFactor(scale);
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_HEIGHT) {
                double scale = (double) size.getHeight() /
                        (double) fullSize.height;
                reduction.factor = getReductionFactor(scale);
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_INSIDE) {
                double hScale = (double) size.getWidth() /
                        (double) fullSize.width;
                double vScale = (double) size.getHeight() /
                        (double) fullSize.height;
                reduction.factor = getReductionFactor(Math.min(hScale, vScale));
            } else if (size.getPercent() != null) {
                reduction.factor = getReductionFactor(size.getPercent() / 100.0f);
            } else {
                reduction.factor = 0;
            }
            if (reduction.factor > 0) {
                command.add("-reduce");
                command.add(reduction.factor + "");
            }
        }

        command.add("-o");
        command.add(quote(getStdoutSymlinkPath()));

        return new ProcessBuilder(command);
    }

    /**
     * Gets a reduction factor for the kdu_expand -reduce flag. 0 is no
     * reduction.
     *
     * @param scalePercent Scale percentage between 0 and 1
     * @return
     */
    public short getReductionFactor(double scalePercent) {
        short factor = 0;
        double nextPct = 0.5f;
        while (scalePercent <= nextPct && factor < MAX_REDUCTION_FACTOR) {
            nextPct /= 2.0f;
            factor++;
        }
        return factor;
    }

    /**
     * Scales an image using Graphics2D, taking an already-applied reduction
     * factor into account.
     *
     * @param inputImage
     * @param size
     * @param reductionFactor
     * @return
     */
    private BufferedImage scaleImageWithG2d(final BufferedImage inputImage,
                                            final Size size,
                                            final ReductionFactor reductionFactor) {
        int sourceWidth = inputImage.getWidth();
        int sourceHeight = inputImage.getHeight();

        BufferedImage scaledImage;
        if (size.getScaleMode() == Size.ScaleMode.FULL) {
            scaledImage = inputImage;
        } else {
            int width = 0, height = 0;
            if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_WIDTH) {
                width = size.getWidth();
                height = sourceHeight * width / sourceWidth;
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_HEIGHT) {
                height = size.getHeight();
                width = sourceWidth * height / sourceHeight;
            } else if (size.getScaleMode() == Size.ScaleMode.NON_ASPECT_FILL) {
                width = size.getWidth();
                height = size.getHeight();
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_INSIDE) {
                double hScale = (double) size.getWidth() / (double) sourceWidth;
                double vScale = (double) size.getHeight() / sourceHeight;
                width = (int) Math.round(sourceWidth *
                        Math.min(hScale, vScale));
                height = (int) Math.round(sourceHeight *
                        Math.min(hScale, vScale));
            } else if (size.getPercent() != null) {
                double pct = (size.getPercent() / 100.0);
                if (reductionFactor.factor > 0) {
                    pct = (size.getPercent() / 100.0) +
                            (1 / (double) (reductionFactor.factor + 1));
                }
                width = (int) Math.round(sourceWidth * pct);
                height = (int) Math.round(sourceHeight * pct);
            }
            scaledImage = new BufferedImage(width, height,
                    inputImage.getType());
            Graphics2D g2d = scaledImage.createGraphics();
            RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHints(hints);
            g2d.drawImage(inputImage, 0, 0, width, height, null);
            g2d.dispose();
        }
        return scaledImage;
    }

}
