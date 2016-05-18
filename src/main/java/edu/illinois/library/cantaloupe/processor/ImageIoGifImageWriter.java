package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.RequestAttributes;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * GIF image writer using ImageIO, capable of taking both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s and writing them as
 * GIFs.
 */
class ImageIoGifImageWriter {

    private RequestAttributes requestAttributes;

    ImageIoGifImageWriter(RequestAttributes attrs) {
        requestAttributes = attrs;
    }

    /**
     * Writes a Java 2D {@link BufferedImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(BufferedImage image, final OutputStream outputStream)
            throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.GIF.getPreferredMediaType().toString());
        if (writers.hasNext()) {
            ImageWriter writer = writers.next();
            IIOImage iioImage = new IIOImage(image, null, null);
            ImageOutputStream ios =
                    ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(ios);
            try {
                writer.write(iioImage);
                ios.flush();
            } finally {
                writer.dispose();
            }
        }
    }

    /**
     * Writes a JAI {@link PlanarImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(PlanarImage image, OutputStream outputStream)
            throws IOException {
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                Format.GIF.getPreferredMediaType().toString());
        if (writers.hasNext()) {
            // GIFWriter can't deal with a non-0,0 origin ("coordinate
            // out of bounds!")
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            pb.add((float) -image.getMinX());
            pb.add((float) -image.getMinY());
            image = JAI.create("translate", pb);

            ImageWriter writer = writers.next();
            ImageOutputStream os = ImageIO.
                    createImageOutputStream(outputStream);
            writer.setOutput(os);
            try {
                writer.write(image);
                os.flush(); // http://stackoverflow.com/a/14489406
            } finally {
                writer.dispose();
            }
        }
    }

}
