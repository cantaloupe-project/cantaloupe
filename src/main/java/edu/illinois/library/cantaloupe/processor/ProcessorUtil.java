package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
     * Efficiently reads the width & height of an image.
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
     * Efficiently reads the width & height of an image.
     *
     * @param readableChannel Will be closed.
     * @param sourceFormat
     * @return Dimensions in pixels
     * @throws ProcessorException
     */
    public static Dimension getSize(ReadableByteChannel readableChannel,
                                    SourceFormat sourceFormat)
            throws ProcessorException {
        try {
            return doGetSize(readableChannel, sourceFormat);
        } finally {
            try {
                readableChannel.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
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

    /**
     * @return Set of all output formats supported by ImageIO.
     */
    public static Set<OutputFormat> imageIoOutputFormats() {
        final String[] writerMimeTypes = ImageIO.getWriterMIMETypes();
        final Set<OutputFormat> outputFormats = new HashSet<>();
        for (OutputFormat outputFormat : OutputFormat.values()) {
            for (String mimeType : writerMimeTypes) {
                if (outputFormat.getMediaType().equals(mimeType.toLowerCase())) {
                    outputFormats.add(outputFormat);
                }
            }
        }
        return outputFormats;
    }
}
