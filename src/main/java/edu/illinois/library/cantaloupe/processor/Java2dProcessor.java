package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Processor using the Java 2D and ImageIO frameworks.</p>
 *
 * <p>Because they both use ImageIO, this processor has a lot in common with
 * {@link JaiProcessor} and so common functionality has been extracted into a
 * base class.</p>
 */
class Java2dProcessor extends AbstractJava2dProcessor
        implements StreamProcessor, FileProcessor {

    private static Logger logger = LoggerFactory.
            getLogger(Java2dProcessor.class);

    static final String DOWNSCALE_FILTER_CONFIG_KEY =
            "Java2dProcessor.downscale_filter";
    static final String NORMALIZE_CONFIG_KEY = "Java2dProcessor.normalize";
    static final String SHARPEN_CONFIG_KEY = "Java2dProcessor.sharpen";
    static final String UPSCALE_FILTER_CONFIG_KEY =
            "Java2dProcessor.upscale_filter";

    Scale.Filter getDownscaleFilter() {
        final String upscaleFilterStr = ConfigurationFactory.getInstance().
                getString(DOWNSCALE_FILTER_CONFIG_KEY);
        try {
            return Scale.Filter.valueOf(upscaleFilterStr.toUpperCase());
        } catch (Exception e) {
            logger.warn("Invalid value for {}", DOWNSCALE_FILTER_CONFIG_KEY);
        }
        return null;
    }

    Scale.Filter getUpscaleFilter() {
        final String upscaleFilterStr = ConfigurationFactory.getInstance().
                getString(UPSCALE_FILTER_CONFIG_KEY);
        try {
            return Scale.Filter.valueOf(upscaleFilterStr.toUpperCase());
        } catch (Exception e) {
            logger.warn("Invalid value for {}", UPSCALE_FILTER_CONFIG_KEY);
        }
        return null;
    }

    @Override
    public void process(final OperationList ops,
                        final ImageInfo imageInfo,
                        final OutputStream outputStream)
            throws ProcessorException {
        if (!getAvailableOutputFormats().contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        final ImageReader reader = getReader();
        try {
            final Orientation orientation = getEffectiveOrientation();

            final ReductionFactor rf = new ReductionFactor();
            final Set<ImageReader.Hint> hints = new HashSet<>();
            BufferedImage image = reader.read(ops, orientation, rf, hints);

            final Configuration config = ConfigurationFactory.getInstance();
            postProcess(image, hints, ops, imageInfo, rf,
                    orientation,
                    config.getBoolean(NORMALIZE_CONFIG_KEY, false),
                    getUpscaleFilter(),
                    getDownscaleFilter(),
                    config.getFloat(SHARPEN_CONFIG_KEY, 0f),
                    outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            reader.dispose();
        }
    }

}
