package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.operation.Encode;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Wraps a {@link javax.imageio.ImageWriter}.</p>
 *
 * <p>N.B.: Clients should remember to call {@link #dispose()} when done with
 * an instance.</p>
 */
public interface ImageWriter {

    /**
     * Releases all resources.
     */
    void dispose();

    /**
     * @param encode Operation defining encoding parameters.
     */
    void setEncode(Encode encode);

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
