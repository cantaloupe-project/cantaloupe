package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class ImageWriterFactory {

    private static final Set<Format> SUPPORTED_FORMATS =
            Collections.unmodifiableSet(EnumSet.of(Format.GIF, Format.JPG,
                    Format.PNG, Format.TIF));

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
