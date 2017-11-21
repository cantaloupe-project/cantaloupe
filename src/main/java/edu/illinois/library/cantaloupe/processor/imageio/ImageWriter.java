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

    private static final Logger LOGGER = LoggerFactory.
            getLogger(ImageWriter.class);

    private static final Set<Format> SUPPORTED_FORMATS =
            Collections.unmodifiableSet(EnumSet.of(Format.GIF, Format.JPG,
                    Format.PNG, Format.TIF));

    private OperationList opList;
    private Metadata sourceMetadata;

    public static void logImageIOWriters() {
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
        this.opList = opList;
    }

    public ImageWriter(final OperationList opList,
                       final Metadata sourceMetadata) {
        this.opList = opList;
        this.sourceMetadata = sourceMetadata;
    }

    /**
     * Writes the given image to the given output stream.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param outputStream Stream to write the image to
     * @throws IOException
     */
    public void write(final RenderedImage image,
                      final Format outputFormat,
                      final OutputStream outputStream) throws IOException {
        switch (outputFormat) {
            case GIF:
                new GIFImageWriter(opList, sourceMetadata).
                        write(image, outputStream);
                break;
            case JPG:
                new JPEGImageWriter(opList, sourceMetadata).
                        write(image, outputStream);
                break;
            case PNG:
                new PNGImageWriter(opList, sourceMetadata).
                        write(image, outputStream);
                break;
            case TIF:
                new TIFFImageWriter(opList, sourceMetadata).
                        write(image, outputStream);
                break;
        }
    }

}
