package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.OperationList;
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
 * <p>Image reader wrapping an ImageIO {@link javax.imageio.ImageReader} instance, with
 * enhancements to support efficient reading of multi-resolution and/or tiled
 * source images with scale-appropriate subsampling.</p>
 *
 * <p>Clients should remember to call {@link #dispose()} when done with an
 * instance.</p>
 */
public class ImageReader {

    public enum Hint {
        ALREADY_CROPPED
    }

    private Format format;
    private AbstractImageReader reader;

    static {
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
     * @param sourceFile
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
     * @param streamSource
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
     *
     * @throws IOException
     */
    public void dispose() throws IOException {
        reader.dispose();
    }

    /**
     * @param imageIndex
     * @return
     * @throws IOException
     */
    public Metadata getMetadata(int imageIndex) throws IOException {
        return reader.getMetadata(imageIndex);
    }

    /**
     * @return
     * @throws IOException
     */
    public int getNumResolutions() throws IOException {
        return reader.getNumResolutions();
    }

    /**
     * @return
     * @throws IOException
     */
    public Dimension getSize() throws IOException {
        return reader.getSize();
    }

    /**
     * @param imageIndex
     * @return
     * @throws IOException
     */
    public Dimension getSize(int imageIndex) throws IOException {
        return reader.getSize(imageIndex);
    }

    /**
     * @param imageIndex
     * @return Tile size of the image at the given index. If the image is not
     *         tiled, the full image dimensions are returned.
     * @throws IOException
     */
    public Dimension getTileSize(int imageIndex) throws IOException {
        return reader.getTileSize(imageIndex);
    }

    /**
     * @return
     * @throws IOException
     */
    public BufferedImage read() throws IOException {
        return reader.read();
    }

    /**
     * @param opList
     * @param reductionFactor
     * @param hints
     * @return
     * @throws IOException
     * @throws ProcessorException
     */
    public BufferedImage read(final OperationList opList,
                              final ReductionFactor reductionFactor,
                              final Set<Hint> hints)
            throws IOException, ProcessorException {
        return reader.read(opList, reductionFactor, hints);
    }

    /**
     * @return
     * @throws IOException
     * @throws UnsupportedSourceFormatException
     */
    public RenderedImage readRendered() throws IOException,
            UnsupportedSourceFormatException {
        return reader.readRendered();
    }

    /**
     * @param opList
     * @param reductionFactor
     * @return
     * @throws IOException
     * @throws ProcessorException
     */
    public RenderedImage readRendered(final OperationList opList,
                                      final ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return reader.readRendered(opList, reductionFactor);
    }

}
