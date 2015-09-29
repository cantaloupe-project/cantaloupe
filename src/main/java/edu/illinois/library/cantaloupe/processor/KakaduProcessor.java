package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;

import gov.lanl.adore.djatoka.io.reader.PNMReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
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
 * Processor using the kdu_expand tool.
 *
 * @see <a href="http://kakadusoftware.com/wp-content/uploads/2014/06/Usage_Examples-v7_7.txt">
 *     Usage Examples for the Demonstration Applications Supplied with Kakadu
 *     V7.7</a>
 */
class KakaduProcessor implements Processor {

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

    private static final String BINARIES_PATH = StringUtils.stripEnd(
            Application.getConfiguration().getString("KakaduProcessor.path_to_binaries"),
            File.separator);
    private static final String STDIN = "/dev/stdin";
    private static final String STDOUT = StringUtils.stripEnd(
            Application.getConfiguration().getString("KakaduProcessor.path_to_stdout_symlink"),
            File.separator);
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
     * @param inputStream Source image
     * @param sourceFormat Format of the source image
     * @return
     * @throws ProcessorException
     */
    public Dimension getSize(ImageInputStream inputStream,
                             SourceFormat sourceFormat)
            throws ProcessorException {
        try {
            // TODO: use the inputstream
            List<String> command = new ArrayList<>();
            command.add(BINARIES_PATH + File.separator + "kdu_jp2info");
            command.add("-i");
            //command.add(quote(new File(STDIN).getAbsolutePath()));
            command.add("/Volumes/Data/alexd/Sites/iiif-test/orion-hubble-4096.jp2");

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
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    public Set<ProcessorFeature> getSupportedFeatures(SourceFormat sourceFormat) {
        return SUPPORTED_FEATURES;
    }

    public Set<Quality> getSupportedQualities(SourceFormat sourceFormat) {
        return SUPPORTED_QUALITIES;
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        ImageInputStream inputStream, OutputStream outputStream)
            throws ProcessorException {
        try {
            final Dimension fullSize = getSize(inputStream, sourceFormat);
            final ProcessBuilder pb = getProcessBuilder(params, fullSize);
            final Process process = pb.start();

            final ByteArrayOutputStream outputBucket = new ByteArrayOutputStream();
            final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();

            DataInputStream stdoutData = new DataInputStream(process.getInputStream());
            DataInputStream errorData = new DataInputStream(process.getErrorStream());
            new Thread(new StreamCopier(stdoutData, outputBucket)).start();
            new Thread(new StreamCopier(errorData, errorBucket)).start();
            //IOUtils.copy(stdoutData, outputBucket);
            //IOUtils.copy(errorData, errorBucket);

            try {
                process.waitFor();
                final String errorStr = errorBucket.toString();
                if (errorStr != null && errorStr.length() > 0) {
                    throw new ProcessorException(errorStr);
                }
                final ByteArrayInputStream bais = new ByteArrayInputStream(
                        outputBucket.toByteArray());
                BufferedImage image = new PNMReader().open(bais);
                image = ProcessorUtil.scaleImageWithG2d(image, params.getSize());
                image = ProcessorUtil.rotateImage(image, params.getRotation());
                image = ProcessorUtil.filterImage(image, params.getQuality());
                ProcessorUtil.outputImage(image, params.getOutputFormat(),
                        outputStream);
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
     * @param params
     * @param fullSize The full size of the source image
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(Parameters params,
                                             Dimension fullSize) {
        final List<String> command = new ArrayList<>();
        command.add(StringUtils.stripEnd(BINARIES_PATH, "/") + File.separator +
                "kdu_expand");
        command.add("-quiet");
        command.add("-i");
        //command.add(quote(new File(STDIN).getAbsolutePath())); // TODO: fix
        command.add("/Volumes/Data/alexd/Sites/iiif-test/orion-hubble-4096.jp2");

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

        command.add("-o");
        command.add(quote(new File(STDOUT).getAbsolutePath()));

        return new ProcessBuilder(command);
    }

}
