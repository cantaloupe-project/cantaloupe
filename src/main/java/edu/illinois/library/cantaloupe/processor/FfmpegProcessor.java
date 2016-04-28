package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.watermark.Position;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.watermark.Watermark;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
class FfmpegProcessor extends AbstractProcessor implements FileProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(FfmpegProcessor.class);

    static final String PATH_TO_BINARIES_CONFIG_KEY =
            "FfmpegProcessor.path_to_binaries";

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

    private static final ExecutorService executorService =
            Executors.newCachedThreadPool();

    private File sourceFile;

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));
        SUPPORTED_IIIF_2_0_QUALITIES.addAll(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY));
        SUPPORTED_FEATURES.addAll(Arrays.asList(
                ProcessorFeature.MIRRORING,
                ProcessorFeature.REGION_BY_PERCENT,
                ProcessorFeature.REGION_BY_PIXELS,
                ProcessorFeature.REGION_SQUARE,
                ProcessorFeature.ROTATION_BY_90S,
                ProcessorFeature.SIZE_ABOVE_FULL,
                ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_HEIGHT,
                ProcessorFeature.SIZE_BY_PERCENT,
                ProcessorFeature.SIZE_BY_WIDTH,
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));
    }

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

    private static String getWatermarkFilterPosition(final Watermark watermark) {
        final Position position = watermark.getPosition();
        final int edgeMargin = watermark.getInset();
        if (position != null) {
            switch (position) {
                case TOP_LEFT:
                    return String.format("%d:%d", edgeMargin, edgeMargin);
                case TOP_RIGHT:
                    return String.format("main_w-overlay_w-%d:%d",
                            edgeMargin, edgeMargin);
                case BOTTOM_LEFT:
                    return String.format("%d:main_h-overlay_h-%d",
                            edgeMargin, edgeMargin);
                case BOTTOM_RIGHT:
                    return String.format(
                            "main_w-overlay_w-%d:main_h-overlay_h-%d",
                            edgeMargin, edgeMargin);
                case TOP_CENTER:
                    return String.format("(main_w-overlay_w)/2:%d", edgeMargin);
                case BOTTOM_CENTER:
                    return String.format(
                            "(main_w-overlay_w)/2:main_h-overlay_h-%d",
                            edgeMargin);
                case LEFT_CENTER:
                    return String.format("%d:(main_h-overlay_h)/2", edgeMargin);
                case RIGHT_CENTER:
                    return String.format(
                            "main_w-overlay_w-%d:(main_h-overlay_h)/2",
                            edgeMargin);
                case CENTER:
                    return "(main_w-overlay_w)/2:(main_h-overlay_h)/2";
            }
        }
        return null;
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        final Set<Format> outputFormats = new HashSet<>();
        if (format.isVideo()) {
            outputFormats.add(Format.JPG);
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
    public ImageInfo getImageInfo() throws ProcessorException {
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

            logger.info("Executing {}", StringUtils.join(pb.command(), " "));
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

            return new ImageInfo(width, height, width, height,
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

        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ProcessBuilder pb = getProcessBuilder(ops,
                    imageInfo.getSize());
            logger.info("Executing {}", StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            try (final InputStream processInputStream = process.getInputStream();
                 final InputStream processErrorStream = process.getErrorStream()) {
                executorService.submit(
                        new StreamCopier(processInputStream, outputStream));
                executorService.submit(
                        new StreamCopier(processErrorStream, errorBucket));
                try {
                    int code = process.waitFor();
                    if (code != 0) {
                        logger.error("ffmpeg returned with code {}", code);
                        final String errorStr = errorBucket.toString();
                        if (errorStr != null && errorStr.length() > 0) {
                            throw new ProcessorException(errorStr);
                        }
                    }
                } finally {
                    process.destroy();
                }
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
     * @param ops
     * @param fullSize The full size of the source image
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(OperationList ops,
                                             Dimension fullSize) {
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
        command.add(sourceFile.getAbsolutePath());
        command.add("-nostdin");
        command.add("-v");
        command.add("quiet");
        command.add("-vframes");
        command.add("1");
        command.add("-an"); // disable audio

        final List<String> filters = new ArrayList<>();
        String filterId = "in";
        for (Operation op : ops) {
            if (op instanceof Crop) {
                Crop crop = (Crop) op;
                if (!crop.isNoOp()) {
                    Rectangle cropArea = crop.getRectangle(fullSize);
                    // ffmpeg will complain if given an out-of-bounds crop area
                    cropArea.width = Math.min(cropArea.width, fullSize.width - cropArea.x);
                    cropArea.height = Math.min(cropArea.height, fullSize.height - cropArea.y);
                    filters.add(String.format("[%s] crop=%d:%d:%d:%d [crop]",
                            filterId, cropArea.width, cropArea.height,
                            cropArea.x, cropArea.y));
                    filterId = "crop";
                }
            } else if (op instanceof Scale) {
                final Scale scale = (Scale) op;
                if (!scale.isNoOp()) {
                    if (scale.getPercent() != null) {
                        int width = Math.round(fullSize.width * scale.getPercent());
                        int height = Math.round(fullSize.height * scale.getPercent());
                        filters.add(String.format("[%s] scale=%d:%d [scale]",
                                filterId, width, height));
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        filters.add(String.format("[%s] scale=%d:-1 [scale]",
                                filterId, scale.getWidth()));
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        filters.add(String.format("[%s] scale=-1:%d [scale]",
                                filterId, scale.getHeight()));
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        int min = Math.min(scale.getWidth(), scale.getHeight());
                        filters.add(String.format("[%s] scale=min(%d\\, iw):-1 [scale]",
                                filterId, min));
                    } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                        filters.add(String.format("[%s] scale=%d:%d [scale]",
                                filterId, scale.getWidth(), scale.getHeight()));
                    }
                    filterId = "scale";
                }
            } else if (op instanceof Transpose) {
                final Transpose transpose = (Transpose) op;
                switch (transpose) {
                    case HORIZONTAL:
                        filters.add(String.format("[%s] hflip [transpose]",
                                filterId));
                        break;
                    case VERTICAL:
                        filters.add(String.format("[%s] vflip [transpose]",
                                filterId));
                        break;
                }
                filterId = "transpose";
            } else if (op instanceof Rotate) {
                final Rotate rotate = (Rotate) op;
                if (!rotate.isNoOp()) {
                    // 0 = 90CounterClockwise and Vertical Transpose (default)
                    // 1 = 90Clockwise
                    // 2 = 90CounterClockwise
                    // 3 = 90Clockwise and Vertical Transpose
                    switch (Math.round(rotate.getDegrees())) {
                        case 90:
                            filters.add(String.format("[%s] transpose=1 [rotate]",
                                    filterId));
                            filterId = "rotate";
                            break;
                        case 180:
                            filters.add(String.format("[%s] transpose=1 [rotate1]",
                                    filterId));
                            filters.add("[rotate1] transpose=1 [rotate2]");
                            filterId = "rotate2";
                            break;
                        case 270:
                            filters.add(String.format("[%s] transpose=2 [rotate]",
                                    filterId));
                            filterId = "rotate";
                            break;
                    }
                }
            } else if (op instanceof Filter) {
                final Filter filter = (Filter) op;
                if (filter.equals(Filter.GRAY)) {
                    filters.add(String.format(
                            "[%s] colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3 [filter]",
                            filterId));
                    filterId = "filter";
                }
            } else if (op instanceof Watermark) {
                final Watermark watermark = (Watermark) op;
                final File watermarkFile = watermark.getImage();
                filters.add(0, String.format("movie=%s [wm]",
                        watermarkFile.getAbsolutePath()));
                filters.add(String.format("[%s][wm] overlay=%s [out]",
                        filterId, getWatermarkFilterPosition(watermark)));
            }
        }

        if (filters.size() > 0) {
            command.add("-vf");
            command.add(StringUtils.join(filters, "; "));
        }

        command.add("-f"); // source or output format depending on position
        command.add("image2pipe");
        command.add("pipe:1");

        return new ProcessBuilder(command);
    }

}
