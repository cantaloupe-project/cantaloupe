package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ImageWriterFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageWriter.class);

    private static final Set<Format> SUPPORTED_FORMATS =
            Collections.unmodifiableSet(EnumSet.of(Format.GIF, Format.JPG,
                    Format.PNG, Format.TIF));

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

    public ImageWriter newImageWriter(OperationList opList) {
        switch (opList.getOutputFormat()) {
            case GIF:
                return new GIFImageWriter(opList);
            case JPG:
                return new JPEGImageWriter(opList);
            case PNG:
                return new PNGImageWriter(opList);
            case TIF:
                return new TIFFImageWriter(opList);
            default:
                throw new IllegalArgumentException(
                        "Unsupported output format: " + opList.getOutputFormat());
        }
    }

    public ImageWriter newImageWriter(OperationList opList,
                                      Metadata sourceMetadata) {
        switch (opList.getOutputFormat()) {
            case GIF:
                return new GIFImageWriter(opList, sourceMetadata);
            case JPG:
                return new JPEGImageWriter(opList, sourceMetadata);
            case PNG:
                return new PNGImageWriter(opList, sourceMetadata);
            case TIF:
                return new TIFFImageWriter(opList, sourceMetadata);
            default:
                throw new IllegalArgumentException(
                        "Unsupported output format: " + opList.getOutputFormat());
        }
    }

}
