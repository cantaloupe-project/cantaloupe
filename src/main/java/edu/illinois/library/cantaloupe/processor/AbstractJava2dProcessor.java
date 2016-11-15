package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Color;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Sharpen;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.redaction.Redaction;
import edu.illinois.library.cantaloupe.image.watermark.Watermark;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

abstract class AbstractJava2dProcessor extends AbstractProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(AbstractJava2dProcessor.class);

    /**
     * Convenience method for processors that use a Java 2D pipeline.
     *
     * @param reader
     * @param opList Operations to apply to the image.
     * @param imageInfo Information about the source image.
     * @param reductionFactor
     * @param normalize Whether to normalize the dynamic range of the resulting
     *                  image.
     * @param upscaleFilter Upscale filter to use.
     * @param downscaleFilter Downscale filter to use.
     * @param sharpenValue Sharpen amount from 0-1.
     * @param outputStream Output stream to write the resulting image to.
     * @throws IOException
     * @throws ProcessorException
     */
    void postProcessUsingJava2d(final ImageReader reader,
                                final OperationList opList,
                                final ImageInfo imageInfo,
                                final ReductionFactor reductionFactor,
                                final boolean normalize,
                                final Scale.Filter upscaleFilter,
                                final Scale.Filter downscaleFilter,
                                final float sharpenValue,
                                final OutputStream outputStream)
            throws IOException, ProcessorException {
        BufferedImage image = Java2dUtil.reduceTo8Bits(reader.read());

        if (normalize) {
            image = Java2dUtil.stretchContrast(image);
        }

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

        // Apply most remaining operations.
        for (Operation op : opList) {
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
                image = Java2dUtil.transposeImage(image,
                        (Transpose) op);
            } else if (op instanceof Rotate) {
                image = Java2dUtil.rotateImage(image,
                        (Rotate) op);
            } else if (op instanceof Color) {
                image = Java2dUtil.transformColor(image,
                        (Color) op);
            }
        }

        // Apply the sharpen operation, if present.
        final Sharpen sharpen = new Sharpen(sharpenValue);
        image = Java2dUtil.sharpenImage(image, sharpen);

        // Apply all remaining operations.
        for (Operation op : opList) {
            if (op instanceof Watermark) {
                try {
                    image = Java2dUtil.applyWatermark(image, (Watermark) op);
                } catch (ConfigurationException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        new ImageWriter(opList).
                write(image, opList.getOutputFormat(), outputStream);
        image.flush();
    }

}
