package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ImageReaderFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageReaderFactory.class);

    private static final Set<Format> SUPPORTED_FORMATS =
            Collections.unmodifiableSet(EnumSet.of(Format.BMP,
                    Format.GIF, Format.JPG, Format.PNG, Format.TIF));

    static {
        try {
            // The application will handle caching itself, if so configured. The
            // ImageIO cache would be redundant.
            ImageIO.setUseCache(false);

            // ImageIO will automatically scan for plugins once, the first time
            // it's used. If our app is initialized after another ImageIO-using
            // app in the same JVM, any additional plugins bundled within our
            // app won't be picked up unless we scan again.
            LOGGER.info("Scanning for ImageIO plugins...");
            ImageIO.scanForPlugins();
        } catch (NumberFormatException e) {
            // This is an ImageIO bug in JDK 9.
            if (!e.getMessage().equals("For input string: \"\"")) {
                throw e;
            }
        }

        logImageIOReaders();
    }

    private static void logImageIOReaders() {
        final List<Format> imageFormats = Arrays.stream(Format.values()).
                filter(f -> Format.Type.IMAGE.equals(f.getType())).
                collect(Collectors.toList());

        for (Format format : imageFormats) {
            Iterator<javax.imageio.ImageReader> it =
                    ImageIO.getImageReadersByMIMEType(format.getPreferredMediaType().toString());
            List<String> readerClasses = new ArrayList<>();

            while (it.hasNext()) {
                javax.imageio.ImageReader reader = it.next();
                readerClasses.add(reader.getClass().getName());
            }

            LOGGER.info("ImageIO readers for {}.{}: {}",
                    Format.class.getSimpleName(),
                    format.getName(),
                    readerClasses.stream().collect(Collectors.joining(", ")));
        }
    }

    /**
     * @return Map of available output formats for all known source formats,
     *         based on information reported by ImageIO.
     */
    public static Set<Format> supportedFormats() {
        return SUPPORTED_FORMATS;
    }

    /**
     * Creates a reusable instance for reading from files.
     *
     * @param sourceFile File to read from.
     * @param format     Format of the source image.
     * @throws IllegalArgumentException if the format is unsupported.
     */
    public ImageReader newImageReader(Path sourceFile,
                                      Format format) throws IOException {
        ImageReader reader = newImageReader(format);
        reader.setSource(sourceFile);
        return reader;
    }

    /**
     * <p>Creates a non-reusable instance.</p>
     *
     * <p>{@link #newImageReader(ImageInputStream, Format)} should be preferred
     * when a first-class {@link ImageInputStream} can be provided.</p>
     *
     * @param inputStream Stream to read from.
     * @param format      Format of the source image.
     * @throws IllegalArgumentException if the format is unsupported.
     */
    public ImageReader newImageReader(InputStream inputStream,
                                      Format format) throws IOException {
        return newImageReader(ImageIO.createImageInputStream(inputStream), format);
    }

    /**
     * Creates a non-reusable instance.
     *
     * @param inputStream Stream to read from.
     * @param format      Format of the source image.
     * @throws IllegalArgumentException if the format is unsupported.
     */
    public ImageReader newImageReader(ImageInputStream inputStream,
                                      Format format) throws IOException {
        ImageReader reader = newImageReader(format);
        reader.setSource(inputStream);
        return reader;
    }

    /**
     * Creates a reusable instance for reading from streams.
     *
     * @param streamSource Source of stream to read from.
     * @param format       Format of the source image.
     * @throws IllegalArgumentException if the format is unsupported.
     */
    public ImageReader newImageReader(StreamSource streamSource,
                                      Format format) throws IOException {
        ImageReader reader = newImageReader(format);
        reader.setSource(streamSource);
        return reader;
    }

    private ImageReader newImageReader(Format format) {
        switch (format) {
            case BMP:
                return new BMPImageReader();
            case GIF:
                return new GIFImageReader();
            case JP2:
                return new JPEG2000ImageReader();
            case JPG:
                return new JPEGImageReader();
            case PNG:
                return new PNGImageReader();
            case TIF:
                return new TIFFImageReader();
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

}
