package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.RequestAttributes;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.color.ICC_Profile;
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
class ImageIoGifImageWriter extends AbstractImageIoImageWriter {

    ImageIoGifImageWriter(RequestAttributes attrs) {
        super(attrs);
    }

    /**
     * @param metadata Metadata to embed the profile into.
     * @param profile Profile to embed.
     * @throws IOException
     */
    protected IIOMetadata embedIccProfile(final IIOMetadata metadata,
                                          final IccProfile profile)
            throws IOException {
        // TODO: write this
        return metadata;
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
            final ImageWriter writer = writers.next();
            try {
                final ImageWriteParam writeParam =
                        writer.getDefaultWriteParam();
                final IIOMetadata metadata = getMetadata(writer, writeParam,
                        image);
                final IIOImage iioImage = new IIOImage(image, null, metadata);
                final ImageOutputStream ios =
                        ImageIO.createImageOutputStream(outputStream);
                writer.setOutput(ios);
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
            final ImageWriter writer = writers.next();
            try {
                // GIFWriter can't deal with a non-0,0 origin ("coordinate
                // out of bounds!")
                final ParameterBlock pb = new ParameterBlock();
                pb.addSource(image);
                pb.add((float) -image.getMinX());
                pb.add((float) -image.getMinY());
                image = JAI.create("translate", pb);

                final ImageWriteParam writeParam =
                        writer.getDefaultWriteParam();
                final IIOMetadata metadata = getMetadata(writer, writeParam,
                        image);
                final IIOImage iioImage = new IIOImage(image, null, metadata);
                final ImageOutputStream os = ImageIO.
                        createImageOutputStream(outputStream);
                writer.setOutput(os);
                writer.write(iioImage);
                os.flush(); // http://stackoverflow.com/a/14489406
            } finally {
                writer.dispose();
            }
        }
    }

}
