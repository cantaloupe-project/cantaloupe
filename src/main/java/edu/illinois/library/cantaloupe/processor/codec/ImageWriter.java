package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.operation.OperationList;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

public interface ImageWriter {

    /**
     * Releases all resources.
     */
    void dispose();

    /**
     * @param metadata Source metadata.
     */
    void setMetadata(Metadata metadata);

    /**
     * @param opList Operation list describing the derivative image.
     */
    void setOperationList(OperationList opList);

    /**
     * Writes the given image to the given output stream.
     *
     * @param image        Image to write.
     * @param outputStream Stream to write the image to.
     */
    void write(RenderedImage image,
               OutputStream outputStream) throws IOException;

    /**
     * Writes the given image sequence to the given output stream.
     *
     * @param sequence     Image sequence to write.
     * @param outputStream Stream to write the image to.
     */
    void write(BufferedImageSequence sequence,
               OutputStream outputStream) throws IOException;

}
