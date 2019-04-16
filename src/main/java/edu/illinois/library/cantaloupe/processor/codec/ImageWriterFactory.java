package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.processor.codec.gif.GIFImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.jpeg.JPEGImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.png.PNGImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.tiff.TIFFImageWriter;

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

    public ImageWriter newImageWriter(Encode encode) {
        ImageWriter writer;

        switch (encode.getFormat()) {
            case GIF:
                writer = new GIFImageWriter();
                break;
            case JPG:
                writer = new JPEGImageWriter();
                break;
            case PNG:
                writer = new PNGImageWriter();
                break;
            case TIF:
                writer = new TIFFImageWriter();
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported output format: " + encode.getFormat());
        }

        writer.setEncode(encode);
        return writer;
    }

}
