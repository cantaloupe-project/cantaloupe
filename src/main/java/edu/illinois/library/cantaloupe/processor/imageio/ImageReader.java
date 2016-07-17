package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.processor.ReductionFactor;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.StreamSource;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
        ALREADY_CROPPED
    }

    private Metadata cachedMetadata;
    private AbstractImageReader reader;

    static {
        // The application will handle caching itself, if so configured, so
        // so the ImageIO cache would only slow things down.
        ImageIO.setUseCache(false);
    }

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by ImageIO.
     */
    public static Set<Format> supportedFormats() {
        return new HashSet<>(Arrays.asList(Format.BMP, Format.GIF, Format.JPG,
                Format.PNG, Format.TIF));
    }

    /**
     * Constructor for reading from files.
     *
     * @param sourceFile File to read from.
     * @param format Format of the source image.
     * @throws IOException
     */
    public ImageReader(File sourceFile, Format format)
            throws IOException {
        switch (format) {
            case BMP:
                reader = new BmpImageReader(sourceFile);
                break;
            case GIF:
                reader = new GifImageReader(sourceFile);
                break;
            case JPG:
                reader = new JpegImageReader(sourceFile);
                break;
            case PNG:
                reader = new PngImageReader(sourceFile);
                break;
            case TIF:
                reader = new TiffImageReader(sourceFile);
                break;
        }
    }

    /**
     * Constructor for reading from streams.
     *
     * @param streamSource Source of stream to read from.
     * @param format Format of the source image.
     * @throws IOException
     */
    public ImageReader(StreamSource streamSource, Format format)
            throws IOException {
        switch (format) {
            case BMP:
                reader = new BmpImageReader(streamSource);
                break;
            case GIF:
                reader = new GifImageReader(streamSource);
                break;
            case JPG:
                reader = new JpegImageReader(streamSource);
                break;
            case PNG:
                reader = new PngImageReader(streamSource);
                break;
            case TIF:
                reader = new TiffImageReader(streamSource);
                break;
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
     * @return Metadata of the image at the given index.
     * @throws IOException
     */
    public Metadata getMetadata(int imageIndex) throws IOException {
        if (cachedMetadata == null) {
            cachedMetadata = reader.getMetadata(imageIndex);
        }
        return cachedMetadata;
    }

    /**
     * @return
     * @throws IOException
     */
    public int getNumResolutions() throws IOException {
        return reader.getNumResolutions();
    }

    /**
     * @return Actual dimensions of the image at the zero index, not taking
     *         into account embedded orientation metadata.
     * @throws IOException
     */
    public Dimension getSize() throws IOException {
        return reader.getSize();
    }

    /**
     * @param imageIndex Zero-based index.
     * @return Actual dimensions of the image at the given index, not taking
     *         into account embedded orientation metadata.
     * @throws IOException
     */
    public Dimension getSize(int imageIndex) throws IOException {
        return reader.getSize(imageIndex);
    }

    /**
     * @param imageIndex Zero-based index.
     * @return Size of the tiles of the image at the given index, not taking
     *         into account embedded orientation metadata. If the image is not
     *         tiled, the full image dimensions are returned.
     * @throws IOException
     */
    public Dimension getTileSize(int imageIndex) throws IOException {
        return reader.getTileSize(imageIndex);
    }

    /**
     * Expedient but not necessarily efficient method that reads a whole
     * image (excluding subimages) in one shot.
     *
     * @return BufferedImage guaranteed to not be of type
     *         {@link BufferedImage#TYPE_CUSTOM}.
     * @throws IOException
     */
    public BufferedImage read() throws IOException {
        return reader.read();
    }

    /**
     * <p>Attempts to read an image as efficiently as possible, utilizing its
     * tile layout and/or subimages, if possible.</p>
     *
     * <p>After reading, clients should check the reader hints to see whether
     * the returned image will require cropping.</p>
     *
     * @param opList          Note that if a
     *                        {@link edu.illinois.library.cantaloupe.image.Crop}
     *                        operation is present, it will be modified
     *                        according to the <code>orientation</code>
     *                        argument.
     * @param orientation     Orientation of the source image data, e.g. as
     *                        reported by embedded metadata.
     * @param reductionFactor {@link ReductionFactor#factor} property will be
     *                        modified to reflect the reduction factor of the
     *                        returned image.
     * @param hints           Will be populated by information returned from
     *                        the reader.
     * @return BufferedImage best matching the given parameters, guaranteed to
     *         not be of {@link BufferedImage#TYPE_CUSTOM}. Clients should
     *         check the hints set to see whether they need to perform
     *         additional cropping.
     * @throws IOException
     * @throws ProcessorException
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
     * @throws IOException
     * @throws UnsupportedSourceFormatException
     */
    public RenderedImage readRendered() throws IOException,
            UnsupportedSourceFormatException {
        return reader.readRendered();
    }

    /**
     * <p>Attempts to reads an image as efficiently as possible, utilizing its
     * tile layout and/or subimages, if possible.</p>
     *
     * @param opList          Note that if a
     *                        {@link edu.illinois.library.cantaloupe.image.Crop}
     *                        operation is present, it will be modified
     *                        according to the <code>orientation</code>
     *                        argument.
     * @param orientation     Orientation of the source image data, e.g. as
     *                        reported by embedded metadata.
     * @param reductionFactor {@link ReductionFactor#factor} property will be
     *                        modified to reflect the reduction factor of the
     *                        returned image.
     * @return RenderedImage best matching the given parameters.
     * @throws IOException
     * @throws ProcessorException
     */
    public RenderedImage readRendered(final OperationList opList,
                                      final Orientation orientation,
                                      final ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return reader.readRendered(opList, orientation, reductionFactor);
    }

}
