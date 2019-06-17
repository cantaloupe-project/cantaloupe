package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.source.StreamFactory;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * <p>Wraps an {@link javax.imageio.ImageReader}.</p>
 *
 * <p>N.B.: Clients should remember to call {@link #dispose()} when done with
 * an instance.</p>
 */
public interface ImageReader {

    /**
     * N.B.: For implementation reasons, this method must not perform any I/O.
     *
     * @return Whether the instance can employ seeking to read portions of the
     *         source image data selectively.
     */
    boolean canSeek();

    /**
     * Releases all resources. Must be called when an instance is no longer
     * needed.
     */
    void dispose();

    /**
     * @return Type of compression used in the image at the given index.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is an error reading the compression.
     */
    Compression getCompression(int imageIndex) throws IOException;

    /**
     * @return Metadata for the image at the given index.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is an error reading the metadata.
     */
    Metadata getMetadata(int imageIndex) throws IOException;

    /**
     * @return Number of subimages physically available in the image container.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is an error reading the number of images.
     */
    int getNumImages() throws IOException;

    /**
     * <p>Returns the number of resolutions available in the image.</p>
     *
     * <ul>
     *     <li>For conventional formats, this is {@literal 1}.</li>
     *     <li>For {@link edu.illinois.library.cantaloupe.image.Format#TIF
     *     pyramidal TIFF}, this is the number of embedded images, equal to
     *     {@link #getNumImages()}.</li>
     *     <li>For {@link edu.illinois.library.cantaloupe.image.Format#JP2}, it
     *     is {@literal number of decomposition levels + 1}.</li>
     * </ul>
     *
     * @return Number of resolutions available in the image.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is an error reading the number of
     *         resolutions.
     */
    int getNumResolutions() throws IOException;

    /**
     * @return Dimensions of the image at the given index.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is an error reading the size.
     */
    Dimension getSize(int imageIndex) throws IOException;

    /**
     * @return Size of the tiles in the image at the given index, or the full
     *         image dimensions if the image is not tiled.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is an error reading the size.
     */
    Dimension getTileSize(int imageIndex) throws IOException;

    /**
     * Reads an entire image into memory.
     *
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is some other error reading the image.
     */
    BufferedImage read() throws IOException;

    /**
     * Reads the region of the image corresponding to the given arguments.
     *
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is some other error reading the image.
     */
    BufferedImage read(Crop crop,
                       Scale scale,
                       ScaleConstraint scaleConstraint,
                       ReductionFactor reductionFactor,
                       Set<ReaderHint> hints) throws IOException;

    /**
     * Reads the region of the image corresponding to the given arguments.
     *
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there some other error reading the image.
     * @deprecated Since version 4.0.
     */
    @Deprecated
    RenderedImage readRendered(Crop crop,
                               Scale scale,
                               ScaleConstraint scaleConstraint,
                               ReductionFactor reductionFactor,
                               Set<ReaderHint> hints) throws IOException;

    /**
     * Reads a sequence of images into memory, such as for e.g. animated GIFs.
     * The {@link BufferedImageSequence#length() sequence length} will be
     * {@literal 1} for conventional images.
     *
     * @throws UnsupportedOperationException if the reader does not support
     *         reading sequences.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there some other error reading the sequence.
     */
    BufferedImageSequence readSequence() throws IOException;

    /**
     * Sets the source to a file.
     */
    void setSource(Path imageFile) throws IOException;

    /**
     * Sets the source to a stream, which is not reusable. {@link
     * #setSource(StreamFactory)} should be used instead, if possible.
     */
    void setSource(ImageInputStream inputStream) throws IOException;

    /**
     * Sets the source to a {@link StreamFactory}.
     */
    void setSource(StreamFactory streamFactory) throws IOException ;

}
