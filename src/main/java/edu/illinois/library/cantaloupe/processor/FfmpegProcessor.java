package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resolver.InputStreamStreamSource;
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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Processor that uses the ffmpeg command-line tool to extract video frames,
 * and the ffprobe tool to get video information. Works with ffmpeg 2.8 (other
 * versions untested).
 */
class FfmpegProcessor extends AbstractJava2dProcessor implements FileProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(FfmpegProcessor.class);

    static final String PATH_TO_BINARIES_CONFIG_KEY =
            "FfmpegProcessor.path_to_binaries";

    private static final ExecutorService executorService =
            Executors.newCachedThreadPool();

    private File sourceFile;

    /**
     * @param binaryName Name of one of the ffmpeg binaries
     * @return
     */
    private static String getPath(String binaryName) {
        String path = Configuration.getInstance().
                getString(PATH_TO_BINARIES_CONFIG_KEY);
        if (path != null && path.length() > 0) {
            path = StringUtils.stripEnd(path, File.separator) + File.separator +
                    binaryName;
        } else {
            path = binaryName;
        }
        return path;
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        final Set<Format> outputFormats = new HashSet<>();
        if (format.isVideo()) {
            outputFormats.addAll(ImageWriter.supportedFormats());
        }
        return outputFormats;
    }

    /**
     * Gets information about the video by parsing the output of ffprobe.
     *
     * @return
     * @throws ProcessorException
     */
    @Override
    public Info readImageInfo() throws ProcessorException {
        final List<String> command = new ArrayList<>();
        // ffprobe -v quiet -print_format xml -show_streams <file>
        command.add(getPath("ffprobe"));
        command.add("-v");
        command.add("quiet");
        command.add("-print_format");
        command.add("xml");
        command.add("-show_streams");
        command.add(sourceFile.getAbsolutePath());
        InputStream processInputStream = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
            Process process = pb.start();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            processInputStream = process.getInputStream();
            Document doc = db.parse(processInputStream);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("//stream[@index=\"0\"]/@width");
            int width = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));
            expr = xpath.compile("//stream[@index=\"0\"]/@height");
            int height = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));

            return new Info(width, height, width, height,
                    getSourceFormat());
        } catch (SAXException e) {
            throw new ProcessorException("Failed to parse XML. Command: " +
                    StringUtils.join(command, " "), e);
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            if (processInputStream != null) {
                try {
                    processInputStream.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public File getSourceFile() {
        return this.sourceFile;
    }

    @Override
    public void process(final OperationList opList,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(opList, imageInfo, outputStream);

        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ProcessBuilder pb = getProcessBuilder(opList);
            logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            try (final InputStream processInputStream = process.getInputStream();
                 final InputStream processErrorStream = process.getErrorStream()) {
                executorService.submit(
                        new StreamCopier(processErrorStream, errorBucket));

                final ImageReader reader = new ImageReader(
                        new InputStreamStreamSource(processInputStream),
                        Format.BMP);
                final BufferedImage image = reader.read();
                try {
                    postProcess(image, null, opList, imageInfo, null,
                            Orientation.ROTATE_0, false, outputStream);
                    final int code = process.waitFor();
                    if (code != 0) {
                        logger.error("ffmpeg returned with code {}", code);
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
        } catch (IOException | InterruptedException e) {
            String msg = e.getMessage();
            final String errorStr = errorBucket.toString();
            if (errorStr != null && errorStr.length() > 0) {
                msg += " (command output: " + msg + ")";
            }
            throw new ProcessorException(msg, e);
        }
    }

    @Override
    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    /**
     * @param opList
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(OperationList opList) {
        final List<String> command = new ArrayList<>();
        command.add(getPath("ffmpeg"));
        command.add("-i");
        command.add(sourceFile.getAbsolutePath());

        // Seeking (to a particular time) is supported via a "time" URL query
        // parameter which gets injected into an -ss flag. FFmpeg supports
        // additional syntax, but this will do for now.
        // https://trac.ffmpeg.org/wiki/Seeking
        if (opList.getOptions().size() > 0) {
            String time = (String) opList.getOptions().get("time");
            // prevent arbitrary input
            if (time != null &&
                    time.matches("[0-9][0-9]:[0-5][0-9]:[0-5][0-9]")) {
                command.add("-ss");
                command.add(time);
            }
        }

        command.add("-nostdin");
        command.add("-v");
        command.add("quiet");
        command.add("-vframes");
        command.add("1");
        command.add("-an"); // disable audio
        command.add("-vcodec");
        command.add("bmp");
        command.add("-f"); // source or output format depending on position
        command.add("image2pipe");
        command.add("pipe:1");

        return new ProcessBuilder(command);
    }

}
