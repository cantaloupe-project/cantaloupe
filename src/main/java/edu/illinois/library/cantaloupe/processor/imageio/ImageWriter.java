package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Image writer using ImageIO, capable of writing both Java 2D
 * {@link BufferedImage}s and JAI {@link PlanarImage}s in several formats.
 */
public class ImageWriter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageWriter.class);

    private static final Set<Format> SUPPORTED_FORMATS =
            Collections.unmodifiableSet(EnumSet.of(Format.GIF, Format.JPG,
                    Format.PNG, Format.TIF));

    private AbstractImageWriter wrappedWriter;

    static {
        logImageIOWriters();
    }

    private static void logImageIOWriters() {
        // TODO: get this info from somewhere else
        final Format[] iiifOutputFormats = new Format[] {
                Format.JPG, Format.PNG, Format.TIF, Format.GIF, Format.PDF,
                Format.JP2, Format.WEBP };

        for (Format format : iiifOutputFormats) {
            Iterator<javax.imageio.ImageWriter> it =
                    ImageIO.getImageWritersByMIMEType(format.getPreferredMediaType().toString());
            List<String> writerClasses = new ArrayList<>();

            while (it.hasNext()) {
                javax.imageio.ImageWriter writer = it.next();
                writerClasses.add(writer.getClass().getName());
            }

            LOGGER.info("ImageIO writers for {}.{}: {}",
                    Format.class.getSimpleName(),
                    format.getName(),
                    writerClasses.stream().collect(Collectors.joining(", ")));
        }
    }

    /**
     * @return Set of supported output formats.
     */
    public static Set<Format> supportedFormats() {
        return SUPPORTED_FORMATS;
    }

    public ImageWriter(final OperationList opList) {
        switch (opList.getOutputFormat()) {
            case GIF:
                wrappedWriter = new GIFImageWriter(opList);
                break;
            case JPG:
                wrappedWriter = new JPEGImageWriter(opList);
                break;
            case PNG:
                wrappedWriter = new PNGImageWriter(opList);
                break;
            case TIF:
                wrappedWriter = new TIFFImageWriter(opList);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported output format: " + opList.getOutputFormat());
        }
    }

    public ImageWriter(final OperationList opList,
                       final Metadata sourceMetadata) {
        switch (opList.getOutputFormat()) {
            case GIF:
                wrappedWriter = new GIFImageWriter(opList, sourceMetadata);
                break;
            case JPG:
                wrappedWriter = new JPEGImageWriter(opList, sourceMetadata);
                break;
            case PNG:
                wrappedWriter = new PNGImageWriter(opList, sourceMetadata);
                break;
            case TIF:
                wrappedWriter = new TIFFImageWriter(opList, sourceMetadata);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported output format: " + opList.getOutputFormat());
        }
    }

    /**
     * @return Wrapped ImageIO writer.
     */
    javax.imageio.ImageWriter getIIOWriter() {
        return wrappedWriter.getIIOWriter();
    }

    /**
     * Writes the given image to the given output stream.
     *
     * @param image        Image to write.
     * @param outputStream Stream to write the image to.
     */
    public void write(final RenderedImage image,
                      final OutputStream outputStream) throws IOException {
        wrappedWriter.write(image, outputStream);
    }

    /**
     * Writes the given image sequence to the given output stream.
     *
     * @param sequence     Image sequence to write.
     * @param outputStream Stream to write the image to.
     */
    public void write(final BufferedImageSequence sequence,
                      final OutputStream outputStream) throws IOException {
        wrappedWriter.write(sequence, outputStream);
    }

}
