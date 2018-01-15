package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Image reader wrapping an ImageIO {@link javax.imageio.ImageReader}
 * instance, with enhancements to support efficient reading of multi-resolution
 * and/or tiled source images.</p>
 *
 * <p>Various image property accessors are available on the instance, and
 * additional metadata is available via {@link #getMetadata(int)}.</p>
 *
 * <p>Clients should remember to call {@link #dispose()} when done with an
 * instance.</p>
 */
public class ImageReader {

    public enum Hint {
        /**
         * Returned from a reader. The reader has read only the requested region
         * of the image and there will be no need to crop it any further.
         */
        ALREADY_CROPPED,

        /**
         * Provided to a reader, telling it to read the entire image ignoring
         * {@link edu.illinois.library.cantaloupe.operation.Crop} operations.
         */
        IGNORE_CROP
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageReader.class);

    private static final Set<Format> SUPPORTED_FORMATS =
            Collections.unmodifiableSet(EnumSet.of(Format.BMP, Format.DCM,
                    Format.GIF, Format.JPG, Format.PNG, Format.TIF));

    private Metadata cachedMetadata;
    private AbstractImageReader reader;

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
     * Constructor for reading from files.
     *
     * @param sourceFile File to read from.
     * @param format     Format of the source image.
     * @throws IllegalArgumentException if the format is unsupported.
     */
    public ImageReader(Path sourceFile, Format format)
            throws IOException {
        switch (format) {
            case BMP:
                reader = new BMPImageReader(sourceFile);
                break;
            case DCM:
                reader = new DICOMImageReader(sourceFile);
                break;
            case GIF:
                reader = new GIFImageReader(sourceFile);
                break;
            case JPG:
                reader = new JPEGImageReader(sourceFile);
                break;
            case PNG:
                reader = new PNGImageReader(sourceFile);
                break;
            case TIF:
                reader = new TIFFImageReader(sourceFile);
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    /**
     * Constructor for reading from streams.
     *
     * @param streamSource Source of stream to read from.
     * @param format       Format of the source image.
     * @throws IllegalArgumentException if the format is unsupported.
     */
    public ImageReader(StreamSource streamSource, Format format)
            throws IOException {
        switch (format) {
            case BMP:
                reader = new BMPImageReader(streamSource);
                break;
            case DCM:
                reader = new DICOMImageReader(streamSource);
                break;
            case GIF:
                reader = new GIFImageReader(streamSource);
                break;
            case JPG:
                reader = new JPEGImageReader(streamSource);
                break;
            case PNG:
                reader = new PNGImageReader(streamSource);
                break;
            case TIF:
                reader = new TIFFImageReader(streamSource);
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    /**
     * Should be called when the reader is no longer needed.
     */
    public void dispose() {
        reader.dispose();
    }

    /**
     * @param imageIndex Zero-based index.
     * @return Compression type of the image at the given index.
     */
    public Compression getCompression(int imageIndex) throws IOException {
        return reader.getCompression(imageIndex);
    }

    /**
     * @return Wrapped ImageIO reader.
     */
    javax.imageio.ImageReader getIIOReader() {
        return reader.getIIOReader();
    }

    /**
     * @param imageIndex Zero-based index.
     * @return Metadata of the image at the given index.
     */
    public Metadata getMetadata(int imageIndex) throws IOException {
        if (cachedMetadata == null) {
            cachedMetadata = reader.getMetadata(imageIndex);
        }
        return cachedMetadata;
    }

    /**
     * @return Number of subimages in the source image container. These may be
     *         different resolutions, or frames of an animation.
     */
    public int getNumImages() throws IOException {
        return reader.getNumImages();
    }

    /**
     * @return Actual dimensions of the image at the zero index, not taking
     *         into account embedded orientation metadata.
     */
    public Dimension getSize() throws IOException {
        return reader.getSize();
    }

    /**
     * @param imageIndex Zero-based index.
     * @return Actual dimensions of the image at the given index, not taking
     *         into account embedded orientation metadata.
     */
    public Dimension getSize(int imageIndex) throws IOException {
        return reader.getSize(imageIndex);
    }

    /**
     * @param imageIndex Zero-based index.
     * @return Size of the tiles of the image at the given index, not taking
     *         into account embedded orientation metadata. If the image is not
     *         tiled, the full image dimensions are returned.
     */
    public Dimension getTileSize(int imageIndex) throws IOException {
        return reader.getTileSize(imageIndex);
    }

    /**
     * Expedient but not necessarily efficient method that reads a whole image
     * (excluding subimages) in one shot.
     *
     * @return Image guaranteed to not be of type
     *         {@link BufferedImage#TYPE_CUSTOM}.
     */
    public BufferedImage read() throws IOException {
        return reader.read();
    }

    /**
     * <p>Attempts to read an image as efficiently as possible, exploiting its
     * tile layout and/or subimages, if possible.</p>
     *
     * <p>After reading, clients should check the reader hints to see whether
     * the returned image will require cropping.</p>
     *
     * @param opList          Note that if a
     *                        {@link edu.illinois.library.cantaloupe.operation.Crop}
     *                        operation is present, it will be modified
     *                        according to the {@literal orientation} argument.
     * @param orientation     Orientation of the source image data, e.g. as
     *                        reported by embedded metadata.
     * @param reductionFactor The {@link ReductionFactor#factor} property will
     *                        be modified to reflect the reduction factor of
     *                        the returned image.
     * @param hints           Will be populated by information returned from
     *                        the reader.
     * @return BufferedImage best matching the given parameters. Clients should
     *         check the hints set to see whether they need to perform
     *         additional cropping.
     */
    public BufferedImage read(final OperationList opList,
                              final Orientation orientation,
                              final ReductionFactor reductionFactor,
                              final Set<Hint> hints)
            throws IOException, ProcessorException {
        return reader.read(opList, orientation, reductionFactor, hints);
    }

    /**
     * Reads an image (excluding subimages).
     *
     * @return RenderedImage
     */
    public RenderedImage readRendered() throws IOException {
        return reader.readRendered();
    }

    /**
     * <p>Attempts to reads an image as efficiently as possible, exploiting its
     * tile layout and/or subimages, if possible.</p>
     *
     * @param opList          Note that if a
     *                        {@link edu.illinois.library.cantaloupe.operation.Crop}
     *                        operation is present, it will be modified
     *                        according to the {@literal orientation} argument.
     * @param orientation     Orientation of the source image data, e.g. as
     *                        reported by embedded metadata.
     * @param reductionFactor The {@link ReductionFactor#factor} property will
     *                        be modified to reflect the reduction factor of
     *                        the returned image.
     * @param hints           Will be populated by information returned from
     *                        the reader.
     * @return Image best matching the given parameters.
     */
    public RenderedImage readRendered(final OperationList opList,
                                      final Orientation orientation,
                                      final ReductionFactor reductionFactor,
                                      final Set<ImageReader.Hint> hints)
            throws IOException, ProcessorException {
        return reader.readRendered(opList, orientation, reductionFactor, hints);
    }

    /**
     * Expedient but not necessarily efficient method that reads all images of
     * a sequence in one shot.
     *
     * @return List of images in sequence order.
     */
    public BufferedImageSequence readSequence() throws IOException {
        return reader.readSequence();
    }

}
