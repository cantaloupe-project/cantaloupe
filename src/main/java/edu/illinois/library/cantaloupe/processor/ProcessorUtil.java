package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
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

}
