package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.processor.imageio.Compression;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Processor using the Java Advanced Imaging (JAI) framework.</p>
 *
 * <p>Because they both use ImageIO, this processor has a lot in common with
 * {@link Java2dProcessor} and so common functionality has been extracted into
 * a base class.</p>
 *
 * @see <a href="http://docs.oracle.com/cd/E19957-01/806-5413-10/806-5413-10.pdf">
 *     Programming in Java Advanced Imaging</a>
 */
class JaiProcessor extends AbstractImageIOProcessor
        implements FileProcessor, StreamProcessor {

    private static Logger logger = LoggerFactory.getLogger(JaiProcessor.class);

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
    public void process(final OperationList opList,
                        final Info imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        super.process(opList, imageInfo, outputStream);

        final ImageReader reader = getReader();
        try {
            final Orientation orientation = getEffectiveOrientation();
            final Dimension fullSize = imageInfo.getSize();
            final ReductionFactor rf = new ReductionFactor();
            final Set<ImageReader.Hint> hints = new HashSet<>();

            final boolean normalize = (boolean) opList.getOptions().
                    getOrDefault(NORMALIZE_CONFIG_KEY, false);
            if (normalize) {
                // When normalizing, the reader needs to read the entire image
                // so that its histogram can be sampled accurately. This will
                // preserve the luminance across tiles.
                hints.add(ImageReader.Hint.IGNORE_CROP);
            }

            final RenderedImage renderedImage = reader.readRendered(opList,
                    orientation, rf, hints);
            RenderedOp renderedOp = JAIUtil.getAsRenderedOp(
                    RenderedOp.wrapRenderedImage(renderedImage));

            // Normalize the image, if specified in the configuration.
            if (normalize) {
                renderedOp = JAIUtil.stretchContrast(renderedOp);
            }
            renderedOp = JAIUtil.rescalePixels(renderedOp);
            renderedOp = JAIUtil.convertTo8Bits(renderedOp);

            for (Operation op : opList) {
                if (op.hasEffect(fullSize, opList)) {
                    if (op instanceof Crop) {
                        renderedOp = JAIUtil.cropImage(renderedOp, (Crop) op, rf);
                    } else if (op instanceof Scale) {
                        /*
                        JAI has a bug that causes it to fail on right-edge
                        deflate-compressed tiles when using the
                        SubsampleAverage operation, as well as the scale
                        operation with any interpolation other than nearest-
                        neighbor. The error is an ArrayIndexOutOfBoundsException
                        in PlanarImage.cobbleByte().
                        Example: /iiif/2/56324x18006-pyramidal-tiled-deflate.tif/32768,0,23556,18006/737,/0/default.jpg
                        So, the strategy is:
                        1) if the TIFF is deflate-compressed, use the scale
                           operation with nearest-neighbor interpolation, which
                           is horrible, but better than nothing.
                        2) otherwise, use the SubsampleAverage operation.
                        */
                        if (getSourceFormat().equals(Format.TIF) &&
                                reader.getCompression(0).equals(Compression.ZLIB)) {
                            logger.debug("process(): detected " +
                                    "ZLib-compressed TIFF; using the scale " +
                                    "operator with nearest-neighbor " +
                                    "interpolation.");
                            renderedOp = JAIUtil.scaleImage(renderedOp, (Scale) op,
                                    Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                    rf);
                        } else if (renderedOp.getWidth() < 3 ||
                                renderedOp.getHeight() < 3) {
                            // SubsampleAverage requires the image to be at
                            // least 3 pixels on a side. So, again use the
                            // Scale operation, with a better (but still bad
                            // [but it doesn't matter because of the tiny
                            // dimension(s)]) filter.
                            renderedOp = JAIUtil.scaleImage(renderedOp, (Scale) op,
                                    Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                                    rf);
                        } else {
                            // All clear to use SubsampleAverage.
                            renderedOp = JAIUtil.scaleImageUsingSubsampleAverage(
                                    renderedOp, (Scale) op, rf);
                        }
                    } else if (op instanceof Transpose) {
                        renderedOp = JAIUtil.
                                transposeImage(renderedOp, (Transpose) op);
                    } else if (op instanceof Rotate) {
                        Rotate rotate = (Rotate) op;
                        rotate.addDegrees(orientation.getDegrees());
                        renderedOp = JAIUtil.rotateImage(renderedOp, rotate);
                    } else if (op instanceof ColorTransform) {
                        renderedOp = JAIUtil.
                                transformColor(renderedOp, (ColorTransform) op);
                    } else if (op instanceof Sharpen) {
                        renderedOp = JAIUtil.
                                sharpenImage(renderedOp, (Sharpen) op);
                    }
                }
            }

            // Apply remaining operations.
            BufferedImage image = null;
            for (Operation op : opList) {
                if (op instanceof Overlay && op.hasEffect(fullSize, opList)) {
                    // Let's cheat and apply the overlay using Java 2D.
                    // There seems to be minimal performance penalty in doing
                    // this, and doing it in JAI is harder (or impossible in
                    // the case of drawing text).
                    image = renderedOp.getAsBufferedImage();
                    try {
                        image = Java2DUtil.applyOverlay(image,
                                (Overlay) op);
                    } catch (ConfigurationException e) {
                        logger.error(e.getMessage());
                    }
                }
            }
            final ImageWriter writer = new ImageWriter(opList,
                    reader.getMetadata(0));

            if (image != null) {
                writer.write(image, opList.getOutputFormat(), outputStream);
            } else {
                writer.write(renderedOp, opList.getOutputFormat(), outputStream);
            }
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            reader.dispose();
        }
    }

}
