package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.iiif.v2.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Arrays;
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
        final List<Format> iiifOutputFormats = Arrays
                .stream(OutputFormat.values())
                .map(OutputFormat::toFormat)
                .collect(Collectors.toList());

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
        ImageWriter writer = newImageWriter(opList.getOutputFormat());
        writer.setOperationList(opList);
        return writer;
    }

    public ImageWriter newImageWriter(OperationList opList,
                                      Metadata sourceMetadata) {
        ImageWriter writer = newImageWriter(opList.getOutputFormat());
        writer.setOperationList(opList);
        writer.setMetadata(sourceMetadata);
        return writer;
    }

    private ImageWriter newImageWriter(Format format) {
        switch (format) {
            case GIF:
                return new GIFImageWriter();
            case JPG:
                return new JPEGImageWriter();
            case PNG:
                return new PNGImageWriter();
            case TIF:
                return new TIFFImageWriter();
            default:
                throw new IllegalArgumentException(
                        "Unsupported output format: " + format);
        }
    }

}
