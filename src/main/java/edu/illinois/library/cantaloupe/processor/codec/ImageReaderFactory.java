package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.codec.bmp.BMPImageReader;
import edu.illinois.library.cantaloupe.processor.codec.gif.GIFImageReader;
import edu.illinois.library.cantaloupe.processor.codec.jpeg.JPEGImageReader;
import edu.illinois.library.cantaloupe.processor.codec.png.PNGImageReader;
import edu.illinois.library.cantaloupe.processor.codec.tiff.TIFFImageReader;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.source.stream.ClosingMemoryCacheImageInputStream;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;

public final class ImageReaderFactory {

    private static final Set<Format> SUPPORTED_FORMATS = Set.of(
            Format.BMP, Format.GIF, Format.JPG, Format.PNG, Format.TIF);

    /**
     * @return Map of available output formats for all known source formats,
     *         based on information reported by ImageIO.
     */
    public static Set<Format> supportedFormats() {
        return SUPPORTED_FORMATS;
    }

    public ImageReader newImageReader(Format format) {
        if (Format.BMP.equals(format)) {
            return new BMPImageReader();
        } else if (Format.GIF.equals(format)) {
            return new GIFImageReader();
        } else if (Format.JPG.equals(format)) {
            return new JPEGImageReader();
        } else if (Format.PNG.equals(format)) {
            return new PNGImageReader();
        } else if (Format.TIF.equals(format)) {
            return new TIFFImageReader();
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    /**
     * Creates a reusable instance for reading from files.
     *
     * @param format     Format of the source image.
     * @param sourceFile File to read from.
     * @throws IllegalArgumentException if the format is unsupported.
     */
    public ImageReader newImageReader(Format format,
                                      Path sourceFile) throws IOException {
        ImageReader reader = newImageReader(format);
        reader.setSource(sourceFile);
        return reader;
    }

    /**
     * <p>Creates a non-reusable instance.</p>
     *
     * <p>{@link #newImageReader(Format, ImageInputStream)} should be preferred
     * when a first-class {@link ImageInputStream} can be provided.</p>
     *
     * @param format      Format of the source image.
     * @param inputStream Stream to read from.
     * @throws IllegalArgumentException if the format is unsupported.
     */
    public ImageReader newImageReader(Format format,
                                      InputStream inputStream) throws IOException {
        return newImageReader(format, new ClosingMemoryCacheImageInputStream(inputStream));
    }

    /**
     * Creates a non-reusable instance.
     *
     * @param format      Format of the source image.
     * @param inputStream Stream to read from.
     * @throws IllegalArgumentException if the format is unsupported.
     */
    public ImageReader newImageReader(Format format,
                                      ImageInputStream inputStream) throws IOException {
        ImageReader reader = newImageReader(format);
        reader.setSource(inputStream);
        return reader;
    }

    /**
     * Creates a reusable instance for reading from streams.
     *
     * @param format        Format of the source image.
     * @param streamFactory Source of stream to read from.
     * @throws IllegalArgumentException if the format is unsupported.
     */
    public ImageReader newImageReader(Format format,
                                      StreamFactory streamFactory) throws IOException {
        ImageReader reader = newImageReader(format);
        reader.setSource(streamFactory);
        return reader;
    }

}
