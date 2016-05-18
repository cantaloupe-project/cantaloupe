package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Image writer using ImageIO, capable of writing both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s in several formats.
 */
class ImageIoImageWriter {

    /**
     * @return Set of supported output formats.
     */
    public static Set<Format> supportedFormats() {
        return new HashSet<>(Arrays.asList(Format.GIF, Format.JPG,
                Format.PNG, Format.TIF));
    }

    /**
     * Writes a Java 2D {@link BufferedImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(final BufferedImage image,
               final Format outputFormat,
               final OutputStream outputStream) throws IOException {
        switch (outputFormat) {
            case GIF:
                new ImageIoGifImageWriter().write(image, outputStream);
                break;
            case JPG:
                new ImageIoJpegImageWriter().write(image, outputStream);
                break;
            case PNG:
                new ImageIoPngImageWriter().write(image, outputStream);
                break;
            case TIF:
                new ImageIoTiffImageWriter().write(image, outputStream);
                break;
            default:
                final Iterator<ImageWriter> writers =
                        ImageIO.getImageWritersByMIMEType(
                                outputFormat.getPreferredMediaType().toString());
                if (writers.hasNext()) {
                    final ImageWriter writer = writers.next();
                    final IIOImage iioImage = new IIOImage(image, null, null);
                    final ImageOutputStream ios =
                            ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(ios);
                    try {
                        writer.write(iioImage);
                        ios.flush();
                    } finally {
                        writer.dispose();
                    }
                }
                break;
        }
    }

    /**
     * Writes a JAI {@link PlanarImage} to the given output stream.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    void write(final PlanarImage image,
               final Format outputFormat,
               final OutputStream outputStream) throws IOException {
        switch (outputFormat) {
            case GIF:
                new ImageIoGifImageWriter().write(image, outputStream);
                break;
            case JPG:
                new ImageIoJpegImageWriter().write(image, outputStream);
                break;
            case PNG:
                new ImageIoPngImageWriter().write(image, outputStream);
                break;
            case TIF:
                new ImageIoTiffImageWriter().write(image, outputStream);
                break;
            default:
                final Iterator<ImageWriter> writers =
                        ImageIO.getImageWritersByMIMEType(
                                outputFormat.getPreferredMediaType().toString());
                if (writers.hasNext()) {
                    final ImageWriter writer = writers.next();
                    final IIOImage iioImage = new IIOImage(image, null, null);
                    final ImageOutputStream ios =
                            ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(ios);
                    try {
                        writer.write(iioImage);
                        ios.flush();
                    } finally {
                        writer.dispose();
                    }
                }
                break;
        }
    }

}
