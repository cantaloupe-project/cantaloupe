package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.processor.codec.jpeg.TurboJPEGImageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Facade class for writing images without having to know what writer to use
 * or whether it's available.
 */
public final class ImageWriterFacade {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageWriterFacade.class);

    /**
     * Writes the given already-processed image to the given stream. {@link
     * edu.illinois.library.cantaloupe.processor.codec.jpeg.TurboJPEGImageWriter}
     * is used if TurboJPEG is available and if writing to a JPEG; otherwise an
     * {@link edu.illinois.library.cantaloupe.processor.codec.ImageWriter} is
     * used.
     */
    public static void write(BufferedImage image,
                             Encode encode,
                             OutputStream outputStream) throws IOException {
        if (Format.get("jpg").equals(encode.getFormat()) &&
                TurboJPEGImageWriter.isTurboJPEGAvailable()) {
            LOGGER.debug("Writing with {}",
                    TurboJPEGImageWriter.class.getName());
            TurboJPEGImageWriter writer = new TurboJPEGImageWriter();
            writer.setBackgroundColor(encode.getBackgroundColor());
            writer.setProgressive(encode.isInterlacing());
            writer.setQuality(encode.getQuality());
            Metadata metadata = encode.getMetadata();
            if (metadata != null) {
                metadata.getXMP().ifPresent(writer::setXMP);
            }
            writer.write(image, outputStream);
        } else {
            ImageWriter writer = new ImageWriterFactory().newImageWriter(encode);
            LOGGER.debug("Writing with {}", writer.getClass().getName());
            writer.write(image, outputStream);
        }
    }

    /**
     * Writes the given already-processed image sequence to the given stream
     * using an {@link
     * edu.illinois.library.cantaloupe.processor.codec.ImageWriter}.
     */
    public static void write(BufferedImageSequence sequence,
                             Encode encode,
                             OutputStream outputStream) throws IOException {
        ImageWriter writer = new ImageWriterFactory().newImageWriter(encode);
        LOGGER.debug("Writing with {}", writer.getClass().getName());
        writer.write(sequence, outputStream);
    }

    private ImageWriterFacade() {}

}
