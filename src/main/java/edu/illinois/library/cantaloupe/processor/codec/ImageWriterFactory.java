package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.processor.codec.gif.GIFImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.jpeg.JPEGImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.png.PNGImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.tiff.TIFFImageWriter;

import java.util.Set;

public final class ImageWriterFactory {

    private static final Set<Format> SUPPORTED_FORMATS = Set.of(
            Format.get("gif"), Format.get("jpg"), Format.get("png"),
            Format.get("tif"));

    /**
     * @return Set of supported output formats.
     */
    public static Set<Format> supportedFormats() {
        return SUPPORTED_FORMATS;
    }

    public ImageWriter newImageWriter(Encode encode) {
        ImageWriter writer;
        if (Format.get("gif").equals(encode.getFormat())) {
            writer = new GIFImageWriter();
        } else if (Format.get("jpg").equals(encode.getFormat())) {
            writer = new JPEGImageWriter();
        } else if (Format.get("png").equals(encode.getFormat())) {
            writer = new PNGImageWriter();
        } else if (Format.get("tif").equals(encode.getFormat())) {
            writer = new TIFFImageWriter();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported output format: " + encode.getFormat());
        }
        writer.setEncode(encode);
        return writer;
    }

}
