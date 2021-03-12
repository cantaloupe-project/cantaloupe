package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.MediaType;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.processor.SourceFormatException;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.source.stream.ClosingMemoryCacheImageInputStream;
import org.slf4j.Logger;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Supplies some base functionality and tries to be efficient with most
 * formats. Format-specific readers may override what they need to.
 */
public abstract class AbstractIIOImageReader {

    /**
     * Assigned by {@link #createReader()}.
     */
    protected javax.imageio.ImageReader iioReader;

    /**
     * Set by {@link #setSource}.
     */
    protected ImageInputStream inputStream;

    /**
     * Set by {@link #setSource}. May be {@literal null}.
     */
    protected Object source;

    protected static void handle(IIOException e) throws IOException {
        // Image I/O ImageReaders don't distinguish between errors resulting
        // from an incompatible format and other kinds of errors, so we have to
        // check message strings, which are plugin-specific.
        if (e.getMessage().startsWith("Unexpected block type") ||
                e.getMessage().startsWith("I/O error reading PNG header") ||
                e.getMessage().startsWith("Not a JPEG file") ||
                (e.getCause() != null &&
                        e.getCause().getMessage() != null &&
                        (e.getCause().getMessage().startsWith("Invalid magic value") ||
                                e.getCause().getMessage().equals("I/O error reading PNG header!")))) {
            throw new SourceFormatException();
        }
        throw e;
    }

    private List<javax.imageio.ImageReader> availableIIOReaders() {
        Iterator<javax.imageio.ImageReader> it = null;
        if (getFormat() != null) {
            // Search by media type.
            Iterator<MediaType> types = getFormat().getMediaTypes().iterator();
            while (types.hasNext() && (it == null || !it.hasNext())) {
                it = ImageIO.getImageReadersByMIMEType(types.next().toString());
            }
            // Search by extension.
            Iterator<String> extensions = getFormat().getExtensions().iterator();
            while (extensions.hasNext() && (it == null || !it.hasNext())) {
                it = ImageIO.getImageReadersBySuffix(extensions.next());
            }
        } else {
            it = ImageIO.getImageReaders(inputStream);
        }

        final List<javax.imageio.ImageReader> iioReaders = new ArrayList<>();
        while (it != null && it.hasNext()) {
            iioReaders.add(it.next());
        }
        return iioReaders;
    }

    abstract public boolean canSeek();

    private void createReader() throws IOException {
        if (inputStream == null) {
            throw new IOException("No source set.");
        }

        iioReader = negotiateIIOReader();

        if (iioReader != null) {
            getLogger().debug("Using {}", iioReader.getClass().getName());
            iioReader.setInput(inputStream, false, false);
        } else {
            throw new IOException("Unable to determine the format of the " +
                    "source image.");
        }
    }

    /**
     * Should be called when the instance is no longer needed.
     */
    public void dispose() {
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (IOException e) {
                getLogger().debug("dispose(): failed to close the input " +
                                "stream: {}", e.getMessage(), e);
            } finally {
                if (iioReader != null) {
                    iioReader.dispose();
                    iioReader = null;
                }
            }
        }
    }

    /**
     * May be overridden by {@link #getUserPreferredIIOImplementation()}.
     *
     * @return ImageIO implementations preferred by the application, in order
     *         of most to least preferred, or an empty array if there is no
     *         preference.
     */
    abstract protected String[] getApplicationPreferredIIOImplementations();

    abstract protected Format getFormat();

    abstract protected Logger getLogger();

    /**
     * @return Number of images contained inside the source image.
     */
    public int getNumImages() throws IOException {
        // Throw any contract-required exceptions.
        getSize(0);
        // The boolean argument tells whether to scan for images, which seems
        // to be necessary for some, but is slower.
        int numImages = iioReader.getNumImages(false);
        if (numImages == -1) {
            numImages = iioReader.getNumImages(true);
        }
        return numImages;
    }

    /**
     * @return {@literal 1}.
     */
    public int getNumResolutions() throws IOException {
        // Throw any contract-required exceptions.
        getSize(0);
        return 1;
    }

    /**
     * N.B.: This method returns a list of strings rather than {@link Class
     * classes} because some readers reside under the {@link com.sun} package,
     * which is encapsulated in Java 9.
     *
     * @return Preferred reader implementation classes, in order of highest to
     *         lowest priority, or an empty array if there is no preference.
     */
    public String[] getPreferredIIOImplementations() {
        final List<String> impls = new ArrayList<>();

        // Prefer a user-specified implementation.
        final String userImpl = getUserPreferredIIOImplementation();
        if (userImpl != null && !userImpl.isEmpty()) {
            impls.add(userImpl);
        }
        // Fall back to an application-preferred implementation.
        final String[] appImpls = getApplicationPreferredIIOImplementations();
        impls.addAll(List.of(appImpls));

        return impls.toArray(new String[] {});
    }

    /**
     * @return Pixel dimensions of the image at the given index.
     */
    public Dimension getSize(int imageIndex) throws IOException {
        try {
            final int width  = iioReader.getWidth(imageIndex);
            final int height = iioReader.getHeight(imageIndex);
            return new Dimension(width, height);
        } catch (IndexOutOfBoundsException e) { // thrown by GeoSolutions TIFFImageReader
            throw new SourceFormatException();
        } catch (IIOException e) {
            handle(e);
            return null;
        }
    }

    /**
     * @return Tile size of the image at the given index, or the full image
     *         dimensions if the image is not tiled (or is mono-tiled).
     */
    public Dimension getTileSize(int imageIndex) throws IOException {
        try {
            final int width = iioReader.getTileWidth(imageIndex);
            int height;

            // If the tile width == the full image width, the image is almost
            // certainly not tiled, or at least not in a way that is useful,
            // and getTileHeight() may return 1 to indicate a strip height, or
            // some other wonky value. In that case, set the tile height to the
            // full image height.
            if (width == iioReader.getWidth(imageIndex)) {
                height = iioReader.getHeight(imageIndex);
            } else {
                height = iioReader.getTileHeight(imageIndex);
            }
            return new Dimension(width, height);
        } catch (IndexOutOfBoundsException e) { // thrown by GeoSolutions TIFFImageReader
            throw new SourceFormatException();
        } catch (IIOException e) {
            handle(e);
            return null;
        }
    }

    /**
     * If this returns a non-{@literal null} value, it will override {@link
     * #getApplicationPreferredIIOImplementations()}.
     *
     * @return Preferred ImageIO implementation as specified by the user. May
     *         be {@literal null}.
     */
    abstract protected String getUserPreferredIIOImplementation();

    /**
     * Chooses the most appropriate ImageIO reader to use based on the return
     * value of {@link #getPreferredIIOImplementations()}.
     */
    private javax.imageio.ImageReader negotiateIIOReader() {
        javax.imageio.ImageReader negotiatedReader = null;

        final List<javax.imageio.ImageReader> iioReaders =
                availableIIOReaders();

        if (!iioReaders.isEmpty()) {
            final String[] preferredImpls = getPreferredIIOImplementations();

            getLogger().trace("ImageIO plugin preferences: {}",
                    (preferredImpls.length > 0) ?
                            String.join(", ", preferredImpls) : "none");

            Found:
            for (String implClass : preferredImpls) {
                for (javax.imageio.ImageReader candidateReader : iioReaders) {
                    if (implClass.equals(candidateReader.getClass().getName())) {
                        negotiatedReader = candidateReader;
                        break Found;
                    }
                }
            }

            if (negotiatedReader == null) {
                negotiatedReader = iioReaders.get(0);
            }
        }
        return negotiatedReader;
    }

    /**
     * Resets the source, closing any existing source input stream and creating
     * a new one.
     *
     * @throws UnsupportedOperationException if the instance is not reusable.
     */
    protected void reset() throws IOException {
        if (source == null) {
            throw new UnsupportedOperationException("Instance is not reusable");
        } else if (source instanceof Path) {
            setSource((Path) source);
        } else {
            setSource((StreamFactory) source);
        }
    }

    public void setSource(Path inputFile) throws IOException {
        dispose();
        source = inputFile;
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            getLogger().debug("setSource(Path): failed to close the input " +
                            "stream: {}", e.getMessage(), e);
        } finally {
            inputStream = ImageIO.createImageInputStream(inputFile.toFile());
            createReader();
        }
    }

    public void setSource(ImageInputStream inputStream) throws IOException {
        dispose();
        source = null;
        this.inputStream = inputStream;
        createReader();
    }

    public void setSource(StreamFactory streamFactory) throws IOException {
        dispose();
        source = streamFactory;
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            getLogger().debug("setSource(StreamFactory): failed to close " +
                    "the input stream: {}", e.getMessage(), e);
        } finally {
            // Some Image I/O readers, like the ones for JPEG, GIF, and PNG,
            // must read whole images into memory.
            if (canSeek()) {
                inputStream = streamFactory.newSeekableStream();
                getLogger().trace("Using a {} for format {}",
                        inputStream.getClass().getSimpleName(),
                        getFormat());
            } else {
                inputStream = new ClosingMemoryCacheImageInputStream(
                        streamFactory.newInputStream());
                getLogger().trace("Using a {} for format {}",
                        inputStream.getClass().getSimpleName(),
                        getFormat());
            }
            createReader();
        }
    }

    ////////////////////////////////////////////////////////////////////////
    /////////////////////// BufferedImage methods //////////////////////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * Expedient but not necessarily efficient method that reads a whole image
     * (excluding subimages) in one shot.
     */
    public BufferedImage read(int imageIndex) throws IOException {
        try {
            return iioReader.read(imageIndex);
        } catch (IndexOutOfBoundsException e) { // thrown by GeoSolutions TIFFImageReader
            throw new SourceFormatException();
        } catch (IIOException e) {
            handle(e);
            throw e;
        }
    }

    /**
     * <p>Attempts to read an image as efficiently as possible, exploiting its
     * tile layout, if possible.</p>
     *
     * <p>This implementation is optimized for mono-resolution images.</p>
     *
     * <p>After reading, clients should check the reader hints to see whether
     * the returned image will require cropping.</p>
     *
     * @param imageIndex      Image index.
     * @param crop            May be {@code null}.
     * @param scale           May be {@code null}.
     * @param scaleConstraint Scale constraint.
     * @param reductionFactor The {@link ReductionFactor#factor} property will
     *                        be modified to reflect the reduction factor of the
     *                        returned image.
     * @param hints           Will be populated by information returned from
     *                        the reader.
     * @return                Image best matching the given arguments.
     */
    public BufferedImage read(final int imageIndex,
                              final Crop crop,
                              final Scale scale,
                              final ScaleConstraint scaleConstraint,
                              final ReductionFactor reductionFactor,
                              final Set<ReaderHint> hints) throws IOException {
        BufferedImage image;
        try {
            if (crop != null && !hints.contains(ReaderHint.IGNORE_CROP)) {
                final Dimension fullSize = new Dimension(
                        iioReader.getWidth(0), iioReader.getHeight(0));
                image = tileAwareRead(
                        imageIndex, crop.getRectangle(fullSize), hints);
            } else {
                image = iioReader.read(imageIndex);
            }
            if (image == null) {
                throw new SourceFormatException(iioReader.getFormatName());
            }
            return image;
        } catch (IndexOutOfBoundsException e) { // thrown by GeoSolutions TIFFImageReader
            throw new SourceFormatException();
        } catch (IIOException e) {
            handle(e);
            return null;
        }
    }

    /**
     * Reads a particular image from a multi-image file.
     *
     * @param imageIndex      Image index.
     * @param crop            Requested crop.
     * @param scaleConstraint Virtual scale constraint applied to the image.
     * @return                Smallest image fitting the requested operations.
     * @see                   #readSmallestUsableSubimage
     */
    protected BufferedImage readMonoResolution(
            final int imageIndex,
            final Crop crop,
            final ScaleConstraint scaleConstraint,
            final Set<ReaderHint> hints) throws IOException {
        final Dimension fullSize = new Dimension(
                iioReader.getWidth(0), iioReader.getHeight(0));
        final Rectangle regionRect = crop.getRectangle(
                fullSize, new ReductionFactor(), scaleConstraint);
        return tileAwareRead(imageIndex, regionRect, hints);
    }

    /**
     * Reads the smallest image that can fulfill the given crop and scale from
     * a multi-image file.
     *
     * @param crop            Requested crop.
     * @param scale           Requested scale.
     * @param scaleConstraint Virtual scale constraint applied to the image.
     * @param reductionFactor Will be set to the reduction factor of the
     *                        returned image.
     * @param hints           Will be populated by information returned by the
     *                        reader.
     * @return                Smallest image fitting the requested operations.
     * @see                   #readMonoResolution
     */
    protected BufferedImage readSmallestUsableSubimage(
            final Crop crop,
            final Scale scale,
            final ScaleConstraint scaleConstraint,
            final ReductionFactor reductionFactor,
            final Set<ReaderHint> hints) throws IOException {
        final Dimension fullSize = new Dimension(
                iioReader.getWidth(0), iioReader.getHeight(0));
        final Rectangle regionRect = crop.getRectangle(
                fullSize, new ReductionFactor(), scaleConstraint);
        BufferedImage bestImage = null;

        if (!scale.hasEffect() && !scaleConstraint.hasEffect()) {
            bestImage = tileAwareRead(0, regionRect, hints);
            getLogger().debug("readSmallestUsableSubimage(): using a {}x{} source " +
                            "image (0x reduction factor)",
                    bestImage.getWidth(), bestImage.getHeight());
        } else {
            // Pyramidal TIFFs will have > 1 image, each with half the
            // dimensions of the previous one. The boolean parameter tells
            // getNumImages() whether to scan for images, which seems to be
            // necessary for at least some files, but is slower. If it is
            // false, and getNumImages() can't find anything, it will return -1.
            int numImages = iioReader.getNumImages(false);
            if (numImages > 1) {
                getLogger().trace("Detected {} subimage(s)", numImages);
            } else if (numImages == -1) {
                numImages = iioReader.getNumImages(true);
                if (numImages > 1) {
                    getLogger().trace("Scan revealed {} subimage(s)", numImages);
                }
            }
            // At this point, we know how many images are available.
            if (numImages == 1) {
                bestImage = tileAwareRead(0, regionRect, hints);
                getLogger().debug("readSmallestUsableSubimage(): using a " +
                                "{}x{} source image (0x reduction factor)",
                        bestImage.getWidth(), bestImage.getHeight());
            } else if (numImages > 1) {
                // Loop through the reduced images from smallest to largest to
                // find the first one that can supply the requested scale.
                for (int i = numImages - 1; i >= 0; i--) {
                    final int subimageWidth = iioReader.getWidth(i);
                    final int subimageHeight = iioReader.getHeight(i);

                    final double reducedScale =
                            (double) subimageWidth / fullSize.width();
                    if (fits(regionRect.size(), scale, scaleConstraint,
                            reducedScale)) {
                        reductionFactor.factor =
                                ReductionFactor.forScale(reducedScale).factor;
                        getLogger().trace("Subimage {}: {}x{} - fits! " +
                                        "({}x reduction factor)",
                                i + 1, subimageWidth, subimageHeight,
                                reductionFactor.factor);
                        final Rectangle reducedRect = new Rectangle(
                                regionRect.x() * reducedScale,
                                regionRect.y() * reducedScale,
                                regionRect.width() * reducedScale,
                                regionRect.height() * reducedScale);
                        bestImage = tileAwareRead(i, reducedRect, hints);
                        break;
                    } else {
                        getLogger().trace("Subimage {}: {}x{} - too small",
                                i + 1, subimageWidth, subimageHeight);
                    }
                }
            }
        }
        return bestImage;
    }

    /**
     * <p>Returns an image for the requested source area by reading the tiles
     * (or strips) of the source image and joining them into a single image.</p>
     *
     * <p>This method is intended to be compatible with all source images, no
     * matter the data layout (tiled, striped, etc.).</p>
     *
     * <p>This method may populate {@literal hints} with
     * {@link ReaderHint#ALREADY_CROPPED}, in which case cropping will
     * have already been performed according to the {@literal region}
     * argument.</p>
     *
     * @param imageIndex Index of the image to read from the ImageReader.
     * @param region     Image region to retrieve. The returned image will be
     *                   this size or smaller if it would overlap the right or
     *                   bottom edge of the source image.
     * @param hints      Will be populated with information returned from the
     *                   reader.
     */
    private BufferedImage tileAwareRead(final int imageIndex,
                                        final Rectangle region,
                                        final Set<ReaderHint> hints) throws IOException {
        final Dimension imageSize = getSize(imageIndex);
        final Dimension tileSize = getTileSize(imageIndex);

        if (tileSize.equals(imageSize)) {
            getLogger().debug("Acquiring region {},{}/{}x{} from {}x{} mono-tiled/mono-striped image",
                    region.intX(), region.intY(),
                    region.intWidth(), region.intHeight(),
                    imageSize.intWidth(), imageSize.intHeight());
        } else if (tileSize.intWidth() == imageSize.intWidth()) {
            getLogger().debug("Acquiring region {},{}/{}x{} from {}x{} image ({}x{} strip size)",
                    region.intX(), region.intY(),
                    region.intWidth(), region.intHeight(),
                    imageSize.intWidth(), imageSize.intHeight(),
                    tileSize.intWidth(), tileSize.intHeight());
        } else {
            getLogger().debug("Acquiring region {},{}/{}x{} from {}x{} image ({}x{} tile size)",
                    region.intX(), region.intY(),
                    region.intWidth(), region.intHeight(),
                    imageSize.intWidth(), imageSize.intHeight(),
                    tileSize.intWidth(), tileSize.intHeight());
        }

        hints.add(ReaderHint.ALREADY_CROPPED);
        final ImageReadParam param = iioReader.getDefaultReadParam();
        param.setSourceRegion(region.toAWTRectangle());

        return iioReader.read(imageIndex, param);
    }

    public BufferedImageSequence readSequence() throws IOException {
        BufferedImageSequence seq = new BufferedImageSequence();
        for (int i = 0, count = getNumImages(); i < count; i++) {
            seq.add(iioReader.read(i));
        }
        return seq;
    }

    ////////////////////////////////////////////////////////////////////////
    /////////////////////// RenderedImage methods //////////////////////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * Reads an image (excluding subimages).
     *
     * @deprecated Since version 4.0.
     */
    @Deprecated
    public RenderedImage readRendered() throws IOException {
        return iioReader.readAsRenderedImage(0,
                iioReader.getDefaultReadParam());
    }

    /**
     * <p>Attempts to read an image as efficiently as possible, utilizing its
     * tile layout and/or subimages, if possible.</p>
     *
     * @param imageIndex
     * @param crop            May be {@code null}.
     * @param scale           May be {@code null}.
     * @param scaleConstraint Scale constraint.
     * @param reductionFactor The {@link ReductionFactor#factor} property will
     *                        be modified to reflect the reduction factor of the
     *                        returned image.
     * @param hints           Will be populated by information returned from
     *                        the reader. May also contain hints for the
     *                        reader. May be {@literal null}.
     * @return                Image best matching the given arguments.
     * @deprecated            Since version 4.0.
     */
    @Deprecated
    public RenderedImage readRendered(int imageIndex,
                                      Crop crop,
                                      Scale scale,
                                      final ScaleConstraint scaleConstraint,
                                      final ReductionFactor reductionFactor,
                                      final Set<ReaderHint> hints) throws IOException {
        if (crop == null) {
            crop = new CropByPercent();
        }
        if (scale == null) {
            scale = new ScaleByPercent();
        }

        RenderedImage image;
        try {
            if (hints != null && hints.contains(ReaderHint.IGNORE_CROP)) {
                image = readRendered();
            } else {
                final Dimension fullSize = new Dimension(
                        iioReader.getWidth(imageIndex),
                        iioReader.getHeight(imageIndex));
                image = readSmallestUsableSubimage(
                        crop.getRectangle(fullSize), scale, fullSize,
                        scaleConstraint, reductionFactor);
            }
            if (image == null) {
                throw new SourceFormatException(iioReader.getFormatName());
            }
            return image;
        } catch (IndexOutOfBoundsException e) { // thrown by GeoSolutions TIFFImageReader
            throw new SourceFormatException();
        } catch (IIOException e) {
            handle(e);
            return null;
        }
    }

    /**
     * Reads the smallest image that can fulfill the given crop and scale from
     * a multi-resolution image.
     *
     * @param region Requested region.
     * @param scale  Requested scale.
     * @param rf     The {@link ReductionFactor#factor} will be set to the
     *               reduction factor of the returned image.
     * @return       The smallest image fitting the requested crop and scale
     *               operations from the given reader.
     * @deprecated Since version 4.0.
     */
    @Deprecated
    private RenderedImage readSmallestUsableSubimage(
            final Rectangle region,
            final Scale scale,
            final Dimension fullSize,
            final ScaleConstraint scaleConstraint,
            final ReductionFactor rf) throws IOException {
        final ImageReadParam param = iioReader.getDefaultReadParam();

        RenderedImage bestImage = null;
        if (!scale.hasEffect() && !scaleConstraint.hasEffect()) {
            bestImage = iioReader.readAsRenderedImage(0, param);
            getLogger().debug("Using a {}x{} source image (0x reduction factor)",
                    bestImage.getWidth(), bestImage.getHeight());
        } else {
            // Pyramidal TIFFs will have > 1 image, each half the dimensions of
            // the next larger. The "true" parameter tells getNumImages() to
            // scan for images, which seems to be necessary for at least some
            // files, but is slower.
            int numImages = iioReader.getNumImages(false);
            if (numImages > 1) {
                getLogger().trace("Detected {} subimage(s)", numImages - 1);
            } else if (numImages == -1) {
                numImages = iioReader.getNumImages(true);
                if (numImages > 1) {
                    getLogger().trace("Scan revealed {} subimage(s)",
                            numImages - 1);
                }
            }
            if (numImages == 1) {
                bestImage = iioReader.read(0, param);
                getLogger().debug("Using a {}x{} source image (0x reduction " +
                                "factor)",
                        bestImage.getWidth(), bestImage.getHeight());
            } else if (numImages > 1) {
                // Loop through the reduced images from smallest to largest to
                // find the first one that can supply the requested scale.
                for (int i = numImages - 1; i >= 0; i--) {
                    final int subimageWidth = iioReader.getWidth(i);
                    final int subimageHeight = iioReader.getHeight(i);

                    final double reducedScale =
                            (double) subimageWidth / fullSize.width();
                    if (fits(region.size(), scale, scaleConstraint,
                            reducedScale)) {
                        rf.factor = ReductionFactor.forScale(reducedScale).factor;
                        getLogger().trace("Subimage {}: {}x{} - fits! " +
                                        "({}x reduction factor)",
                                i + 1, subimageWidth, subimageHeight,
                                rf.factor);
                        bestImage = iioReader.readAsRenderedImage(i, param);
                        break;
                    } else {
                        getLogger().trace("Subimage {}: {}x{} - too small",
                                i + 1, subimageWidth, subimageHeight);
                    }
                }
            }
        }
        return bestImage;
    }

    /**
     * @param regionSize      Size of a cropped source image region.
     * @param scale           Requested scale.
     * @param scaleConstraint Scale constraint to be applied to the requested
     *                        scale.
     * @param reducedScale    Reduced scale of a pyramid level.
     * @return                Whether the given source image region can be
     *                        satisfied by the given reduced scale at the
     *                        requested scale.
     */
    private boolean fits(final Dimension regionSize,
                         final Scale scale,
                         final ScaleConstraint scaleConstraint,
                         final double reducedScale) {
        if (scale instanceof ScaleByPercent) {
            return fits((ScaleByPercent) scale, scaleConstraint, reducedScale);
        }
        return fits(regionSize, (ScaleByPixels) scale, reducedScale);
    }

    private boolean fits(final ScaleByPercent scale,
                         final ScaleConstraint scaleConstraint,
                         final double reducedScale) {
        final double scScale = scaleConstraint.getRational().doubleValue();
        double cappedScale   = (scale.getPercent() > 1) ? 1 : scale.getPercent();
        return (cappedScale * scScale <= reducedScale);
    }

    private boolean fits(final Dimension regionSize,
                         final ScaleByPixels scale,
                         final double reducedScale) {
        switch (scale.getMode()) {
            case ASPECT_FIT_WIDTH:
                double cappedWidth = (scale.getWidth() > regionSize.width()) ?
                        regionSize.width() : scale.getWidth();
                return (cappedWidth / regionSize.width() <= reducedScale);
            case ASPECT_FIT_HEIGHT:
                double cappedHeight = (scale.getHeight() > regionSize.height()) ?
                        regionSize.height() : scale.getHeight();
                return (cappedHeight / regionSize.height() <= reducedScale);
            default:
                cappedWidth = (scale.getWidth() > regionSize.width()) ?
                        regionSize.width() : scale.getWidth();
                cappedHeight = (scale.getHeight() > regionSize.height()) ?
                        regionSize.height() : scale.getHeight();
                return (cappedWidth / regionSize.width() <= reducedScale &&
                        cappedHeight / regionSize.height() <= reducedScale);
        }
    }

}
