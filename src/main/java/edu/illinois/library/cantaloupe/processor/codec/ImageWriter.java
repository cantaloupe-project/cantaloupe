package edu.illinois.library.cantaloupe.processor.codec;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

public interface ImageWriter {

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
