package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Transpose;
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
import java.awt.Rectangle;
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
 * Processor that uses the ffmpeg command-line tool to extract video frames,
 * and the ffprobe tool to get video information. Works with ffmpeg 2.8 (other
 * versions untested).
 */
class FfmpegProcessor implements FileProcessor {

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
                if (!e.getMessage().startsWith("Broken pipe")) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

    }

    private static Logger logger = LoggerFactory.getLogger(FfmpegProcessor.class);

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.COLOR);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.GRAY);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.NATIVE);

        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.COLOR);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.DEFAULT);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.GRAY);

        SUPPORTED_FEATURES.add(ProcessorFeature.MIRRORING);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PERCENT);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PIXELS);
        //SUPPORTED_FEATURES.add(ProcessorFeature.ROTATION_ARBITRARY);
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
                getString("FfmpegProcessor.path_to_binaries");
        if (path != null) {
            path = StringUtils.stripEnd(path, File.separator) + File.separator +
                    binaryName;
        } else {
            path = binaryName;
        }
        return path;
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
        if (sourceFormat.getType() != null &&
                sourceFormat.getType().equals(SourceFormat.Type.VIDEO)) {
            outputFormats.add(OutputFormat.JPG);
        }
        return outputFormats;
    }

    /**
     * Gets the size of the given video by parsing the output of ffprobe.
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
        // ffprobe -v quiet -print_format xml -show_streams <file>
        command.add(getPath("ffprobe"));
        command.add("-v");
        command.add("quiet");
        command.add("-print_format");
        command.add("xml");
        command.add("-show_streams");
        command.add(inputFile.getAbsolutePath());
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            logger.debug("Executing {}", StringUtils.join(pb.command(), " "));
            Process process = pb.start();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(process.getInputStream());

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("//stream[@index=\"0\"]/@width");
            int width = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));
            expr = xpath.compile("//stream[@index=\"0\"]/@height");
            int height = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));
            return new Dimension(width, height);
        } catch (SAXException e) {
            throw new ProcessorException("Failed to parse XML. Command: " +
                    StringUtils.join(command, " "), e);
        } catch (Exception e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    public Dimension getSize(InputStream inputStream, SourceFormat sourceFormat)
            throws ProcessorException {
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        }

        final List<String> command = new ArrayList<>();
        // ffprobe -v quiet -print_format xml -show_streams pipe: < <file>
        command.add(getPath("ffprobe"));
        command.add("-v");
        command.add("quiet");
        command.add("-print_format");
        command.add("xml");
        command.add("-show_streams");
        command.add("pipe:");

        final ByteArrayOutputStream outputBucket = new ByteArrayOutputStream();
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ProcessBuilder pb = new ProcessBuilder(command);

            logger.debug("Executing {}", StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            new Thread(new StreamCopier(process.getInputStream(), outputBucket)).start();
            new Thread(new StreamCopier(process.getErrorStream(), errorBucket)).start();
            new StreamCopier(inputStream, process.getOutputStream()).run();

            try {
                int code = process.waitFor();
                if (code != 0) {
                    logger.warn("ffprobe returned with code {}", code);
                    final String errorStr = errorBucket.toString();
                    if (errorStr != null && errorStr.length() > 0) {
                        throw new ProcessorException(errorStr);
                    }
                }
                final ByteArrayInputStream bais = new ByteArrayInputStream(
                        outputBucket.toByteArray());
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(bais);
                XPath xpath = XPathFactory.newInstance().newXPath();
                XPathExpression expr = xpath.compile("//stream[@index=\"0\"]/@width");
                int width = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));
                expr = xpath.compile("//stream[@index=\"0\"]/@height");
                int height = (int) Math.round((double) expr.evaluate(doc, XPathConstants.NUMBER));
                return new Dimension(width, height);
            } finally {
                process.getInputStream().close();
                //process.getOutputStream().close();
                process.getErrorStream().close();
                process.destroy();
            }
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
    getSupportedIiif1_1Qualities(SourceFormat sourceFormat) {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
    getSupportedIiif2_0Qualities(SourceFormat sourceFormat) {
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
            final ProcessBuilder pb = getProcessBuilder(ops, fullSize,
                    inputFile);
            logger.debug("Executing {}", StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            new Thread(new StreamCopier(process.getInputStream(), outputBucket)).start();
            new Thread(new StreamCopier(process.getErrorStream(), errorBucket)).start();

            try {
                int code = process.waitFor();
                if (code != 0) {
                    logger.warn("ffmpeg returned with code {}", code);
                    final String errorStr = errorBucket.toString();
                    if (errorStr != null && errorStr.length() > 0) {
                        throw new ProcessorException(errorStr);
                    }
                }
                final ByteArrayInputStream bais = new ByteArrayInputStream(
                        outputBucket.toByteArray());
                IOUtils.copy(bais, outputStream);
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

    public void process(OperationList ops, SourceFormat sourceFormat,
                        Dimension fullSize, InputStream inputStream,
                        OutputStream outputStream)
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ProcessBuilder pb = getProcessBuilder(ops, fullSize);

            logger.debug("Executing {}", StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            new Thread(new StreamCopier(process.getInputStream(), outputStream)).start();
            new Thread(new StreamCopier(process.getErrorStream(), errorBucket)).start();
            new StreamCopier(inputStream, process.getOutputStream()).run();

            try {
                int code = process.waitFor();
                if (code != 0) {
                    logger.warn("ffmpeg returned with code " + code);
                    final String errorStr = errorBucket.toString();
                    if (errorStr != null && errorStr.length() > 0) {
                        throw new ProcessorException(errorStr);
                    }
                }
            } finally {
                process.getInputStream().close();
                //process.getOutputStream().close();
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
     * @param ops
     * @param fullSize The full size of the source image
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(OperationList ops,
                                             Dimension fullSize) {
        return getProcessBuilder(ops, fullSize, "pipe:0");
    }

    /**
     * @param ops
     * @param fullSize The full size of the source image
     * @param inputFile
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(OperationList ops,
                                             Dimension fullSize,
                                             File inputFile) {
        return getProcessBuilder(ops, fullSize,
                quote(inputFile.getAbsolutePath()));
    }

    /**
     * @param ops
     * @param fullSize
     * @param inputArg Either an absolute pathname or <code>pipe:</code>
     * @return
     */
    private ProcessBuilder getProcessBuilder(OperationList ops,
                                             Dimension fullSize,
                                             String inputArg) {
        // ffmpeg -i pipe:0 -nostdin -v quiet -vframes 1 -an -vf [ops] -f image2pipe pipe:1 < video.mpg > out.jpg
        final List<String> command = new ArrayList<>();
        command.add(getPath("ffmpeg"));

        // Seeking (to a particular time) is supported via a "time" URL query
        // parameter which gets injected into an -ss flag. FFmpeg supports
        // additional syntax, but this will do for now.
        // https://trac.ffmpeg.org/wiki/Seeking
        if (ops.getOptions().size() > 0) {
            String time = (String) ops.getOptions().get("time");
            // prevent arbitrary input
            if (time != null &&
                    time.matches("[0-9][0-9]:[0-5][0-9]:[0-5][0-9]")) {
                command.add("-ss");
                command.add(time);
            }
        }

        command.add("-i");
        command.add(inputArg);
        command.add("-nostdin");
        command.add("-v");
        command.add("quiet");
        command.add("-vframes");
        command.add("1");
        command.add("-an"); // disable audio

        List<String> filters = new ArrayList<>();
        for (Operation op : ops) {
            if (op instanceof Crop) {
                Crop crop = (Crop) op;
                if (!crop.isFull()) {
                    Rectangle cropArea = crop.getRectangle(fullSize);
                    // ffmpeg will complain if given an out-of-bounds crop area
                    cropArea.width = Math.min(cropArea.width, fullSize.width - cropArea.x);
                    cropArea.height = Math.min(cropArea.height, fullSize.height - cropArea.y);
                    filters.add(String.format("crop=%d:%d:%d:%d", cropArea.width,
                            cropArea.height, cropArea.x, cropArea.y));
                }
            } else if (op instanceof Scale) {
                Scale scale = (Scale) op;
                if (scale.getMode() != Scale.Mode.FULL) {
                    if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        filters.add(String.format("scale=%d:-1", scale.getWidth()));
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        filters.add(String.format("scale=-1:%d", scale.getHeight()));
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        int min = Math.min(scale.getWidth(), scale.getHeight());
                        filters.add(String.format("scale=min(%d\\, iw):-1", min));
                    } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                        filters.add(String.format("scale=%d:%d", scale.getWidth(),
                                scale.getHeight()));
                    } else if (scale.getPercent() != 0) {
                        int width = Math.round(fullSize.width * scale.getPercent());
                        int height = Math.round(fullSize.height * scale.getPercent());
                        filters.add(String.format("scale=%d:%d", width, height));
                    }
                }
            } else if (op instanceof Transpose) {
                Transpose transpose = (Transpose) op;
                switch (transpose) {
                    case HORIZONTAL:
                        filters.add("hflip");
                        break;
                    case VERTICAL:
                        filters.add("vflip");
                        break;
                }
            } else if (op instanceof Rotate) {
                Rotate rotate = (Rotate) op;
                if (rotate.getDegrees() > 0) {
                    // 0 = 90CounterClockwise and Vertical Transpose (default)
                    // 1 = 90Clockwise
                    // 2 = 90CounterClockwise
                    // 3 = 90Clockwise and Vertical Transpose
                    switch (Math.round(rotate.getDegrees())) {
                        case 90:
                            filters.add("transpose=1");
                            break;
                        case 180:
                            filters.add("transpose=1");
                            filters.add("transpose=1");
                            break;
                        case 270:
                            filters.add("transpose=2");
                            break;
                    }
                }
            } else if (op instanceof Filter) {
                Filter filter = (Filter) op;
                if (filter.equals(Filter.GRAY)) {
                    filters.add("colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3");
                }
            }
        }
        if (filters.size() > 0) {
            command.add("-vf");
            command.add(StringUtils.join(filters, ","));
        }

        command.add("-f"); // source or output format depending on position
        command.add("image2pipe");
        command.add("pipe:1");

        return new ProcessBuilder(command);
    }

}
