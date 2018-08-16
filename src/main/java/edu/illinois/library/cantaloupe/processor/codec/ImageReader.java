package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.source.StreamFactory;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * <p>N.B.: Clients should remember to call {@link #dispose()} when done with
 * an instance.</p>
 */
public interface ImageReader {

    /**
     * Releases all resources.
     */
    void dispose();

    /**
     * @return Type of compression used in the image at the given index.
     * @throws IOException if there is an error reading the compression.
     */
    Compression getCompression(int imageIndex) throws IOException;

    /**
     * @return Metadata for the image at the given index.
     * @throws IOException if there is an error reading the metadata.
     */
    Metadata getMetadata(int imageIndex) throws IOException;

    /**
     * @return Number of subimages physically available in the image container.
     * @throws IOException if there is an error reading the number of images.
     */
    int getNumImages() throws IOException;

    /**
     * <p>Returns the number of resolutions available in the image.</p>
     *
     * <ul>
     *     <li>For conventional formats, this will be {@literal 1}.</li>
     *     <li>For {@link edu.illinois.library.cantaloupe.image.Format#TIF
     *     pyramidal TIFF}, this will be the number of embedded images, equal
     *     to {@link #getNumImages()}.</li>
     *     <li>For {@link edu.illinois.library.cantaloupe.image.Format#JP2}, it
     *     will be {@literal number of decomposition levels + 1}.</li>
     * </ul>
     *
     * @return Number of resolutions available in the image.
     * @throws IOException if there is an error reading the number of
     *         resolutions.
     */
    int getNumResolutions() throws IOException;

    /**
     * @return Dimensions of the image at the given index.
     * @throws IOException if there is an error reading the size.
     */
    Dimension getSize(int imageIndex) throws IOException;

    /**
     * @return Size of the tiles in the image at the given index, or the full
     *         image dimensions if the image is not tiled.
     * @throws IOException if there is an error reading the size.
     */
    Dimension getTileSize(int imageIndex) throws IOException;

    /**
     * Reads an entire image into memory.
     *
     * @throws IOException if there is an error reading the image.
     */
    BufferedImage read() throws IOException;

    /**
     * Reads the region of the image corresponding to the given arguments.
     *
     * @throws IOException if there is an error reading the image.
     */
    BufferedImage read(OperationList opList,
                       Orientation orientation,
                       ReductionFactor reductionFactor,
                       Set<ReaderHint> hints) throws IOException;

    /**
     * Reads the region of the image corresponding to the given arguments.
     *
     * @throws IOException if there is an error reading the image.
     * @deprecated Since version 4.0.
     */
    @Deprecated
    RenderedImage readRendered(OperationList opList,
                               Orientation orientation,
                               ReductionFactor reductionFactor,
                               Set<ReaderHint> hints) throws IOException;

    /**
     * Reads a sequence of images into memory, such as for e.g. animated GIFs.
     *
     * @throws IOException if there is an error reading the sequence.
     * @throws UnsupportedOperationException if the reader does not support
     *         reading sequences.
     */
    default BufferedImageSequence readSequence() throws IOException {
        throw new UnsupportedOperationException();
    }

    void setSource(Path imageFile) throws IOException;

    void setSource(ImageInputStream inputStream) throws IOException;

    void setSource(StreamFactory streamFactory) throws IOException ;

}
