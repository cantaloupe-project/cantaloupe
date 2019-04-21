package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.processor.codec.BufferedImageSequence;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.processor.codec.jpeg.TurboJPEGImageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

final class WriterFacade {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(WriterFacade.class);

    /**
     * Writes the given already-processed image to the given stream. {@link
     * edu.illinois.library.cantaloupe.processor.codec.jpeg.TurboJPEGImageWriter}
     * is used if TurboJPEG is available and if writing to a JPEG; otherwise an
     * {@link edu.illinois.library.cantaloupe.processor.codec.ImageWriter} is
     * used.
     */
    static void write(BufferedImage image,
                      Encode encode,
                      OutputStream outputStream) throws IOException {
        if (Format.JPG.equals(encode.getFormat()) &&
                TurboJPEGImageWriter.isTurboJPEGAvailable()) {
            LOGGER.debug("Writing with {}",
                    TurboJPEGImageWriter.class.getName());
            TurboJPEGImageWriter writer = new TurboJPEGImageWriter();
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
    static void write(BufferedImageSequence sequence,
                      Encode encode,
                      OutputStream outputStream) throws IOException {
        ImageWriter writer = new ImageWriterFactory().newImageWriter(encode);
        LOGGER.debug("Writing with {}", writer.getClass().getName());
        writer.write(sequence, outputStream);
    }

    private WriterFacade() {}

}
