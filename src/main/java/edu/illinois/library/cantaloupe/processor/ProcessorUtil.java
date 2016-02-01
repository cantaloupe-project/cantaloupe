package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

abstract class ProcessorUtil {

    private static Logger logger = LoggerFactory.getLogger(ProcessorUtil.class);

    /**
     * Gets a reduction factor where the corresponding scale is 1/(2^rf).
     *
     * @param scalePercent Scale percentage between 0 and 1
     * @param maxFactor 0 for no max
     * @return
     * @see #getScale
     */
    public static ReductionFactor getReductionFactor(double scalePercent,
                                                     int maxFactor) {
        if (maxFactor == 0) {
            maxFactor = 999999;
        }
        short factor = 0;
        double nextPct = 0.5f;
        while (scalePercent <= nextPct && factor < maxFactor) {
            nextPct /= 2.0f;
            factor++;
        }
        return new ReductionFactor(factor);
    }

    /**
     * @param reductionFactor Reduction factor
     * @return Scale corresponding to the given reduction factor (1/(2^rf)).
     * @see #getReductionFactor
     */
    public static double getScale(ReductionFactor reductionFactor) {
        double scale = 1f;
        for (int i = 0; i < reductionFactor.factor; i++) {
            scale /= 2;
        }
        return scale;
    }

    /**
     * Efficiently reads the dimensions of an image.
     *
     * @param inputFile
     * @param sourceFormat
     * @return Dimensions in pixels
     * @throws ProcessorException
     */
    public static Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException {
        try {
            return doGetSize(new FileImageInputStream(inputFile), sourceFormat);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * Efficiently reads the dimensions of an image.
     *
     * @param streamSource StreamSource from which to obtain a stream to read
     *                     the size.
     * @param sourceFormat
     * @return Dimensions in pixels
     * @throws ProcessorException
     */
    public static Dimension getSize(StreamSource streamSource,
                                    SourceFormat sourceFormat)
            throws ProcessorException {
        ImageInputStream iis = null;
        try {
            iis = streamSource.newImageInputStream();
            return doGetSize(iis, sourceFormat);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            try {
                if (iis != null) {
                    iis.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * @param inputStream
     * @param sourceFormat
     * @return
     * @throws ProcessorException
     */
    private static Dimension doGetSize(ImageInputStream inputStream,
                                       SourceFormat sourceFormat)
            throws ProcessorException {
        Iterator<ImageReader> iter = ImageIO.
                getImageReadersBySuffix(sourceFormat.getPreferredExtension());
        if (iter.hasNext()) {
            ImageReader reader = iter.next();
            int width, height;
            try {
                reader.setInput(inputStream);
                width = reader.getWidth(reader.getMinIndex());
                height = reader.getHeight(reader.getMinIndex());
            } catch (IOException e) {
                throw new ProcessorException(e.getMessage(), e);
            } finally {
                reader.dispose();
            }
            return new Dimension(width, height);
        }
        return null;
    }

    /**
     * @return Watermark position, or null if
     *         {@link Processor#WATERMARK_POSITION_CONFIG_KEY} is not set.
     */
    public static Position getWatermarkPosition() {
        final Configuration config = Application.getConfiguration();
        final String configValue = config.
                getString(Processor.WATERMARK_POSITION_CONFIG_KEY, "");
        if (configValue.length() > 0) {
            final String enumStr = StringUtils.replace(configValue, " ", "_").
                    toUpperCase();
            try {
                return Position.valueOf(enumStr);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid {} value: {}",
                        Processor.WATERMARK_POSITION_CONFIG_KEY, configValue);
            }
        }
        return null;
    }

}
