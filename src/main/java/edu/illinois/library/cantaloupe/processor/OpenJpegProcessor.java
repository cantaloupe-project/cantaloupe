package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.redaction.Redaction;
import edu.illinois.library.cantaloupe.image.watermark.Watermark;
import edu.illinois.library.cantaloupe.resolver.InputStreamStreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
 * Processor using the OpenJPEG opj_decompress and opj_dump command-line tools.
 * Written for version 2.1.0, but may work with other versions. Uses
 * opj_decompress for cropping and an initial scale reduction factor, and
 * either Java 2D or JAI for all remaining processing steps. opj_decompress
 * generates BMP output which is streamed directly to the ImageIO or JAI
 * reader, which are really fast with BMP for some reason.
 */
class OpenJpegProcessor extends AbstractProcessor implements FileProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(OpenJpegProcessor.class);

    static final String JAVA2D_SCALE_MODE_CONFIG_KEY =
            "OpenJpegProcessor.post_processor.java2d.scale_mode";
    static final String PATH_TO_BINARIES_CONFIG_KEY =
            "OpenJpegProcessor.path_to_binaries";
    static final String POST_PROCESSOR_CONFIG_KEY =
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

    // will cache opj_dump output
    private String imageInfo;
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
                ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_HEIGHT,
                ProcessorFeature.SIZE_BY_PERCENT,
                ProcessorFeature.SIZE_BY_WIDTH,
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));

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
            outputFormats.addAll(ImageIoImageWriter.supportedFormats());
        }
        return outputFormats;
    }

    /**
     * Gets the size of the given image by parsing the output of opj_dump.
     *
     * @return
     * @throws ProcessorException
     */
    @Override
    public ImageInfo getImageInfo() throws ProcessorException {
        try {
            if (imageInfo == null) {
                readImageInfo();
            }
            final ImageInfo.Image image = new ImageInfo.Image();

            final Scanner scan = new Scanner(imageInfo);
            while (scan.hasNextLine()) {
                String line = scan.nextLine().trim();
                if (line.startsWith("x1=")) {
                    String[] parts = StringUtils.split(line, ",");
                    for (int i = 0; i < 2; i++) {
                        String[] kv = StringUtils.split(parts[i], "=");
                        if (kv.length == 2) {
                            if (i == 0) {
                                image.width = Integer.parseInt(kv[1].trim());
                            } else {
                                image.height = Integer.parseInt(kv[1].trim());
                            }
                        }
                    }
                } else if (line.startsWith("tdx=")) {
                    String[] parts = StringUtils.split(line, ",");
                    if (parts.length == 2) {
                        image.tileWidth = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                        image.tileHeight = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                    }
                }
            }
            final ImageInfo info = new ImageInfo();
            info.setSourceFormat(getSourceFormat());
            info.getImages().add(image);
            return info;
        } catch (IOException e) {
            throw new ProcessorException("Failed to parse size", e);
        }
    }

    private void readImageInfo() throws IOException {
        final List<String> command = new ArrayList<>();
        command.add(getPath("opj_dump"));
        command.add("-i");
        command.add(sourceFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        logger.info("Invoking {}", StringUtils.join(pb.command(), " "));
        Process process = pb.start();

        try (InputStream processInputStream = process.getInputStream()) {
            imageInfo = IOUtils.toString(processInputStream, "UTF-8");
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
                executorService.submit(new StreamCopier(
                        processErrorStream, errorBucket));

                final ImageIoImageReader reader = new ImageIoImageReader();
                reader.setFormat(Format.BMP);
                reader.setSource(
                        new InputStreamStreamSource(processInputStream));

                Configuration config = Configuration.getInstance();
                switch (config.getString(POST_PROCESSOR_CONFIG_KEY, "java2d").toLowerCase()) {
                    case "jai":
                        logger.info("Post-processing using JAI ({} = jai)",
                                POST_PROCESSOR_CONFIG_KEY);
                        postProcessUsingJai(reader, ops, reductionFactor,
                                outputStream);
                        break;
                    default:
                        logger.info("Post-processing using Java 2D ({} = java2d)",
                                POST_PROCESSOR_CONFIG_KEY);
                        postProcessUsingJava2d(reader, ops, reductionFactor,
                                outputStream);
                        break;
                }

                final int code = process.waitFor();
                if (code != 0) {
                    logger.warn("opj_decompress returned with code {}", code);
                    final String errorStr = errorBucket.toString();
                    if (errorStr != null && errorStr.length() > 0) {
                        throw new ProcessorException(errorStr);
                    }
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
                msg += " (command output: " + msg + ")";
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
     *                  reference
     * @return Command string
     */
    private ProcessBuilder getProcessBuilder(final OperationList opList,
                                             final Dimension imageSize,
                                             final ReductionFactor reduction) {
        final List<String> command = new ArrayList<>();
        command.add(getPath("opj_decompress"));
        command.add("-i");
        command.add(sourceFile.getAbsolutePath());

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

    private void postProcessUsingJai(final ImageIoImageReader reader,
                                     final OperationList opList,
                                     final ReductionFactor reductionFactor,
                                     final OutputStream outputStream)
            throws IOException, ProcessorException {
        BufferedImage image = null;
        RenderedImage renderedImage = reader.readRendered();
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
            } else if (op instanceof Watermark) {
                // Let's cheat and apply the watermark using Java 2D.
                // There seems to be minimal performance penalty in doing
                // this, and doing it in JAI is harder.
                image = renderedOp.getAsBufferedImage();
                try {
                    image = Java2dUtil.applyWatermark(image, (Watermark) op);
                } catch (ConfigurationException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        ImageIoImageWriter writer = new ImageIoImageWriter();

        if (image != null) {
            writer.write(image, opList.getOutputFormat(), outputStream);
            image.flush();
        } else {
            writer.write(renderedOp, opList.getOutputFormat(),
                    outputStream);
        }
    }

    private void postProcessUsingJava2d(final ImageIoImageReader reader,
                                        final OperationList opList,
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

        // Perform all remaining operations.
        for (Operation op : opList) {
            if (op instanceof Scale) {
                final boolean highQuality = Configuration.getInstance().
                        getString(JAVA2D_SCALE_MODE_CONFIG_KEY, "speed").
                        equals("quality");
                image = Java2dUtil.scaleImage(image,
                        (Scale) op, reductionFactor, highQuality);
            } else if (op instanceof Transpose) {
                image = Java2dUtil.transposeImage(image, (Transpose) op);
            } else if (op instanceof Rotate) {
                image = Java2dUtil.rotateImage(image, (Rotate) op);
            } else if (op instanceof Filter) {
                image = Java2dUtil.filterImage(image, (Filter) op);
            } else if (op instanceof Watermark) {
                try {
                    image = Java2dUtil.applyWatermark(image, (Watermark) op);
                } catch (ConfigurationException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        new ImageIoImageWriter().write(image, opList.getOutputFormat(),
                outputStream);
        image.flush();
    }

    private void reset() {
        imageInfo = null;
    }

}
