package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;

abstract class ProcessorUtil {

    /**
     * Gets a reduction factor where the corresponding scale is 1/(2^rf).
     *
     * @param scalePercent Scale percentage between 0 and 1
     * @param maxFactor 0 for no max
     * @return
     * @see #getScale
     */
    public static int getReductionFactor(double scalePercent, int maxFactor) {
        if (maxFactor == 0) {
            maxFactor = 999999;
        }
        short factor = 0;
        double nextPct = 0.5f;
        while (scalePercent <= nextPct && factor < maxFactor) {
            nextPct /= 2.0f;
            factor++;
        }
        return factor;
    }

    /**
     * @param reductionFactor Reduction factor (0 for no reduction)
     * @return Scale corresponding to the given reduction factor (1/(2^rf)).
     * @see #getReductionFactor
     */
    public static double getScale(int reductionFactor) {
        double scale = 1f;
        for (int i = 0; i < reductionFactor; i++) {
            scale /= 2;
        }
        return scale;
    }

    /**
     * Efficiently reads the width & height of an image without reading the
     * entire image into memory.
     *
     * @param inputFile
     * @param sourceFormat
     * @return Dimensions in pixels
     * @throws ProcessorException
     */
    public static Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException {
        return doGetSize(inputFile, sourceFormat);
    }

    /**
     * Efficiently reads the width & height of an image without reading the
     * entire image into memory.
     *
     * @param readableChannel
     * @param sourceFormat
     * @return Dimensions in pixels
     * @throws ProcessorException
     */
    public static Dimension getSize(ReadableByteChannel readableChannel,
                                    SourceFormat sourceFormat)
            throws ProcessorException {
        return doGetSize(readableChannel, sourceFormat);
    }

    /**
     * @param input Object that can be passed to
     * {@link ImageIO#createImageInputStream(Object)}
     * @param sourceFormat
     * @return
     * @throws ProcessorException
     */
    private static Dimension doGetSize(Object input, SourceFormat sourceFormat)
            throws ProcessorException {
        Iterator<ImageReader> iter = ImageIO.
                getImageReadersBySuffix(sourceFormat.getPreferredExtension());
        if (iter.hasNext()) {
            ImageReader reader = iter.next();
            int width, height;
            try {
                reader.setInput(ImageIO.createImageInputStream(input));
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

}
