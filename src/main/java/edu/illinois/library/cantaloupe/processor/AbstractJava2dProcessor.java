package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract class AbstractJava2dProcessor extends AbstractImageIoProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractJava2dProcessor.class);

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

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
    }

    public Set<ProcessorFeature> getSupportedFeatures() {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIiif1_1Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIiif2_0Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats().size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_2_0_QUALITIES);
        }
        return qualities;
    }

    /**
     * Convenience method for processors that use a Java 2D pipeline. Generally
     * a processor will read an image, and then pass it to this method to
     * process it and write the result.
     *
     * @param image Image to process.
     * @param readerHints Hints from the image reader. May be <code>null</code>.
     * @param opList Operations to apply to the image.
     * @param imageInfo Information about the source image.
     * @param reductionFactor May be <code>null</code>.
     * @param normalize Whether to normalize the dynamic range of the resulting
     *                  image.
     * @param upscaleFilter Upscale filter to use.
     * @param downscaleFilter Downscale filter to use.
     * @param sharpenValue Sharpen amount from 0-1.
     * @param outputStream Output stream to write the resulting image to.
     * @throws IOException
     * @throws ProcessorException
     */
    void postProcess(BufferedImage image,
                     Set<ImageReader.Hint> readerHints,
                     final OperationList opList,
                     final ImageInfo imageInfo,
                     ReductionFactor reductionFactor,
                     final Orientation orientation,
                     final boolean normalize,
                     final Scale.Filter upscaleFilter,
                     final Scale.Filter downscaleFilter,
                     final float sharpenValue,
                     final OutputStream outputStream)
            throws IOException, ProcessorException {
        if (reductionFactor == null) {
            reductionFactor = new ReductionFactor();
        }
        if (readerHints == null) {
            readerHints = new HashSet<>();
        }

        if (normalize) {
            image = Java2dUtil.stretchContrast(image);
        }
        image = Java2dUtil.reduceTo8Bits(image);

        final Dimension fullSize = imageInfo.getSize();

        // Apply the crop operation, if present, and maintain a reference
        // to it for subsequent operations to refer to.
        Crop crop = new Crop(0, 0, image.getWidth(), image.getHeight(),
                orientation, imageInfo.getSize());
        for (Operation op : opList) {
            if (op instanceof Crop) {
                crop = (Crop) op;
                if (crop.hasEffect(fullSize, opList) &&
                        !readerHints.contains(ImageReader.Hint.ALREADY_CROPPED)) {
                    image = Java2dUtil.cropImage(image, crop, reductionFactor);
                }
            }
        }

        // Redactions happen immediately after cropping.
        List<Redaction> redactions = new ArrayList<>();
        for (Operation op : opList) {
            if (op instanceof Redaction) {
                if (op.hasEffect(fullSize, opList)) {
                    redactions.add((Redaction) op);
                }
            }
        }
        image = Java2dUtil.applyRedactions(image, crop, reductionFactor,
                redactions);

        // Apply most remaining operations.
        for (Operation op : opList) {
            if (op.hasEffect(fullSize, opList)) {
                if (op instanceof Scale) {
                    final Scale scale = (Scale) op;
                    final Float upOrDown =
                            scale.getResultingScale(imageInfo.getSize());
                    if (upOrDown != null) {
                        final Scale.Filter filter =
                                (upOrDown > 1) ? upscaleFilter : downscaleFilter;
                        scale.setFilter(filter);
                    }
                    image = Java2dUtil.scaleImage(image, scale, reductionFactor);
                } else if (op instanceof Transpose) {
                    image = Java2dUtil.transposeImage(image, (Transpose) op);
                } else if (op instanceof Rotate) {
                    Rotate rotation = (Rotate) op;
                    rotation.addDegrees(orientation.getDegrees());
                    image = Java2dUtil.rotateImage(image, rotation);
                } else if (op instanceof Color) {
                    image = Java2dUtil.transformColor(image, (Color) op);
                }
            }
        }

        // Apply the sharpen operation, if present.
        final Sharpen sharpen = new Sharpen(sharpenValue);
        if (sharpen.hasEffect(fullSize, opList)) {
            image = Java2dUtil.sharpenImage(image, sharpen);
        }

        // Apply all remaining operations.
        for (Operation op : opList) {
            if (op instanceof Overlay && op.hasEffect(fullSize, opList)) {
                try {
                    image = Java2dUtil.applyOverlay(image, (Overlay) op);
                } catch (ConfigurationException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        new ImageWriter(opList).
                write(image, opList.getOutputFormat(), outputStream);
    }

}
