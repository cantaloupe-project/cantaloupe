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
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Processor using the OpenJPEG opj_decompress and opj_dump command-line tools.
 * Written for version 2.1.0, but may work with other versions. Uses
 * opj_decompress for cropping and an initial scale reduction factor, and
 * either Java 2D or JAI for all remaining processing steps. opj_decompress
 * generates BMP output which is streamed directly to the ImageIO or JAI
 * reader, which are really fast with BMP for some reason.
 */
class OpenJpegProcessor implements FileProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(OpenJpegProcessor.class);

    public static final String JAVA2D_SCALE_MODE_CONFIG_KEY =
            "OpenJpegProcessor.post_processor.java2d.scale_mode";
    public static final String PATH_TO_BINARIES_CONFIG_KEY =
            "OpenJpegProcessor.path_to_binaries";
    public static final String POST_PROCESSOR_CONFIG_KEY =
            "OpenJpegProcessor.post_processor";

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

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE);

        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY);

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
        String path = Application.getConfiguration().
                getString(PATH_TO_BINARIES_CONFIG_KEY);
        if (path != null) {
            path = StringUtils.stripEnd(path, File.separator) +
                    File.separator + binaryName;
        } else {
            path = binaryName;
        }
        return path;
    }

    @Override
    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> outputFormats = new HashSet<>();
        if (sourceFormat == SourceFormat.JP2) {
            outputFormats.addAll(ImageIoImageWriter.supportedFormats());
        }
        return outputFormats;
    }

    /**
     * Gets the size of the given image by parsing the output of opj_dump.
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
        command.add(getPath("opj_dump"));
        command.add("-i");
        command.add(inputFile.getAbsolutePath());
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
            Process process = pb.start();

            BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String s;
            int width = 0, height = 0;
            while ((s = stdInput.readLine()) != null) {
                if (s.trim().startsWith("x1=")) {
                    String[] parts = StringUtils.split(s.trim(), ",");
                    for (int i = 0; i < 2; i++) {
                        String[] kv = StringUtils.split(parts[i], "=");
                        if (kv.length == 2) {
                            if (i == 0) {
                                width = Integer.parseInt(kv[1].trim());
                            } else {
                                height = Integer.parseInt(kv[1].trim());
                            }
                        }
                    }
                    return new Dimension(width, height);
                }
            }
            throw new ProcessorException("Failsed to parse output. Command: " +
                            StringUtils.join(command, " "));
        } catch (IOException e) {
            throw new ProcessorException("Failed to parse output. Command: " +
                    StringUtils.join(command, " "), e);
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
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIiif1_1Qualities(final SourceFormat sourceFormat) {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities(final SourceFormat sourceFormat) {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_2_0_QUALITIES);
        }
        return qualities;
    }

    @Override
    public void process(final OperationList ops,
                        final SourceFormat sourceFormat,
                        final Dimension fullSize,
                        final File inputFile,
                        final OutputStream outputStream)
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        class StreamCopier implements Runnable {
            private final InputStream inputStream;
            private final OutputStream outputStream;

            public StreamCopier(InputStream in, OutputStream out) {
                inputStream = in;
                outputStream = out;
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

        // will receive stderr output from kdu_expand
        final ByteArrayOutputStream errorBucket = new ByteArrayOutputStream();
        try {
            final ReductionFactor reductionFactor = new ReductionFactor();
            final ProcessBuilder pb = getProcessBuilder(inputFile, ops,
                    fullSize, reductionFactor);
            logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
            final Process process = pb.start();

            executorService.submit(new StreamCopier(
                    process.getErrorStream(), errorBucket));

            Configuration config = Application.getConfiguration();
            switch (config.getString(POST_PROCESSOR_CONFIG_KEY, "java2d").toLowerCase()) {
                case "jai":
                    logger.info("Post-processing using JAI ({} = jai)",
                            POST_PROCESSOR_CONFIG_KEY);
                    postProcessUsingJai(
                            process.getInputStream(), ops,
                            reductionFactor, outputStream);
                    break;
                default:
                    logger.info("Post-processing using Java 2D ({} = java2d)",
                            POST_PROCESSOR_CONFIG_KEY);
                    postProcessUsingJava2d(
                            process.getInputStream(), ops,
                            reductionFactor, outputStream);
                    break;
            }
            try {
                final int code = process.waitFor();
                if (code != 0) {
                    logger.warn("opj_decompress returned with code {}", code);
                    final String errorStr = errorBucket.toString();
                    if (errorStr != null && errorStr.length() > 0) {
                        throw new ProcessorException(errorStr);
                    }
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
     * @param opList
     * @param imageSize The full size of the source image
     * @param reduction {@link ReductionFactor#factor} property modified by
     * reference
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(final File inputFile,
                                             final OperationList opList,
                                             final Dimension imageSize,
                                             final ReductionFactor reduction) {
        final List<String> command = new ArrayList<>();
        command.add(getPath("opj_decompress"));
        command.add("-i");
        command.add(inputFile.getAbsolutePath());

        for (Operation op : opList) {
            if (op instanceof Crop) {
                final Crop crop = (Crop) op;
                if (!crop.isNoOp()) {
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
                // height/width are <=50% of full size. The smaller the scale,
                // the bigger the win.
                final Scale scale = (Scale) op;
                final Dimension tileSize = getCroppedSize(opList, imageSize);
                if (scale.getMode() != Scale.Mode.FULL) {
                    if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        double hvScale = (double) scale.getWidth() /
                                (double) tileSize.width;
                        reduction.factor = ProcessorUtil.getReductionFactor(
                                hvScale, MAX_REDUCTION_FACTOR).factor;
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        double hvScale = (double) scale.getHeight() /
                                (double) tileSize.height;
                        reduction.factor = ProcessorUtil.getReductionFactor(
                                hvScale, MAX_REDUCTION_FACTOR).factor;
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        double hScale = (double) scale.getWidth() /
                                (double) tileSize.width;
                        double vScale = (double) scale.getHeight() /
                                (double) tileSize.height;
                        reduction.factor = ProcessorUtil.getReductionFactor(
                                Math.min(hScale, vScale), MAX_REDUCTION_FACTOR).factor;
                    } else if (scale.getPercent() != null) {
                        reduction.factor = ProcessorUtil.getReductionFactor(
                                scale.getPercent(), MAX_REDUCTION_FACTOR).factor;
                    } else {
                        reduction.factor = 0;
                    }
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

    private void postProcessUsingJai(final InputStream inputStream,
                                     final OperationList opList,
                                     final ReductionFactor reductionFactor,
                                     final OutputStream outputStream)
            throws IOException, ProcessorException {
        RenderedImage renderedImage = new ImageIoImageReader().
                readRendered(inputStream, SourceFormat.BMP);
        RenderedOp renderedOp = JaiUtil.reformatImage(
                RenderedOp.wrapRenderedImage(renderedImage),
                new Dimension(512, 512));
        for (Operation op : opList) {
            if (op instanceof Scale) {
                renderedOp = JaiUtil.scaleImage(renderedOp, (Scale) op,
                        reductionFactor);
            } else if (op instanceof Transpose) {
                renderedOp = JaiUtil.transposeImage(renderedOp,
                        (Transpose) op);
            } else if (op instanceof Rotate) {
                renderedOp = JaiUtil.rotateImage(renderedOp, (Rotate) op);
            } else if (op instanceof Filter) {
                renderedOp = JaiUtil.filterImage(renderedOp, (Filter) op);
            }
        }
        if (WatermarkService.isEnabled()) {
            renderedOp = JaiUtil.applyWatermark(renderedOp);
        }

        ImageIO.write(renderedOp, opList.getOutputFormat().getExtension(),
                ImageIO.createImageOutputStream(outputStream));
    }

    private void postProcessUsingJava2d(final InputStream inputStream,
                                        final OperationList opList,
                                        final ReductionFactor reductionFactor,
                                        final OutputStream outputStream)
            throws IOException, ProcessorException {
        BufferedImage image = new ImageIoImageReader().read(inputStream);
        for (Operation op : opList) {
            if (op instanceof Scale) {
                final boolean highQuality = Application.getConfiguration().
                        getString(JAVA2D_SCALE_MODE_CONFIG_KEY, "speed").
                        equals("quality");
                image = Java2dUtil.scaleImageWithG2d(image,
                        (Scale) op, reductionFactor, highQuality);
            } else if (op instanceof Transpose) {
                image = Java2dUtil.transposeImage(image,
                        (Transpose) op);
            } else if (op instanceof Rotate) {
                image = Java2dUtil.rotateImage(image,
                        (Rotate) op);
            } else if (op instanceof Filter) {
                image = Java2dUtil.filterImage(image,
                        (Filter) op);
            }
        }
        if (WatermarkService.isEnabled()) {
            image = Java2dUtil.applyWatermark(image);
        }

        new ImageIoImageWriter().write(image, opList.getOutputFormat(),
                outputStream);
        image.flush();
    }

}
