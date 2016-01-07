package edu.illinois.library.cantaloupe.processor;

import com.sun.imageio.plugins.gif.GIFImageReader;
import com.sun.imageio.plugins.png.PNGImageReader;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resolver.ChannelSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Image reader using ImageIO to efficiently read source images.
 */
class ImageIoImageReader {

    private static Logger logger = LoggerFactory.
            getLogger(ImageIoImageReader.class);

    public enum ReaderHint {
        ALREADY_CROPPED
    }

    /**
     * @param inputSource  {@link File} or {@link ReadableByteChannel}
     * @param sourceFormat Format of the source image
     * @return New ImageReader instance with input already set. Should be
     * disposed after using.
     * @throws IOException
     * @throws UnsupportedSourceFormatException
     */
    private static ImageReader newImageReader(Object inputSource,
                                              final SourceFormat sourceFormat)
            throws IOException, UnsupportedSourceFormatException {
        Iterator<ImageReader> it = ImageIO.getImageReadersByMIMEType(
                sourceFormat.getPreferredMediaType().toString());
        if (it.hasNext()) {
            final ImageReader reader = it.next();
            reader.setInput(ImageIO.createImageInputStream(inputSource));
            return reader;
        } else {
            throw new UnsupportedSourceFormatException(sourceFormat);
        }
    }

    /**
     * Expedient but not necessarily efficient method wrapping
     * {@link ImageIO#read} that reads a whole image (excluding subimages) in
     * one shot.
     *
     * @param readableChannel Image channel to read.
     * @return BufferedImage guaranteed to not be of type
     * {@link BufferedImage#TYPE_CUSTOM}.
     * @throws IOException
     */
    public BufferedImage read(ReadableByteChannel readableChannel)
            throws IOException {
        final BufferedImage image = ImageIO.read(
                ImageIO.createImageInputStream(readableChannel));
        final BufferedImage rgbImage = Java2dUtil.convertCustomToRgb(image);
        if (rgbImage != image) {
            logger.warn("Converted image to RGB (this is very expensive)");
        }
        return rgbImage;
    }

    /**
     * <p>Attempts to reads an image as efficiently as possible, utilizing its
     * tile layout and/or subimages, if possible.</p>
     * <p/>
     * <p>After reading, clients should check the reader hints to see whether
     * the returned image will require cropping.</p>
     *
     * @param imageFile       Image file to read.
     * @param sourceFormat    Format of the source image.
     * @param ops
     * @param fullSize        Full size of the source image.
     * @param reductionFactor {@link ReductionFactor#factor} property will be
     *                        modified to reflect the reduction factor of the
     *                        returned image.
     * @param hints           Will be populated by information returned by the reader.
     * @return BufferedImage best matching the given parameters, guaranteed to
     * not be of {@link BufferedImage#TYPE_CUSTOM}. Clients should
     * check the hints set to see whether they need to perform
     * additional cropping.
     * @throws IOException
     * @throws ProcessorException
     */
    public BufferedImage read(final File imageFile,
                              final SourceFormat sourceFormat,
                              final OperationList ops,
                              final Dimension fullSize,
                              final ReductionFactor reductionFactor,
                              final Set<ReaderHint> hints)
            throws IOException, ProcessorException {
        return multiLevelAwareRead(imageFile, sourceFormat, ops, fullSize,
                reductionFactor, hints);
    }

    /**
     * @see #read(File, SourceFormat, OperationList, Dimension,
     * ReductionFactor, Set< ReaderHint >)
     *
     * @param channelSource Source of image channels to read
     * @param sourceFormat Format of the source image
     * @param ops
     * @param fullSize Full size of the source image.
     * @param reductionFactor {@link ReductionFactor#factor} property will be
     *                        modified to reflect the reduction factor of the
     *                        returned image.
     * @param hints Will be populated by information returned by the reader.
     * @return BufferedImage best matching the given parameters, guaranteed to
     * not be of {@link BufferedImage#TYPE_CUSTOM}. Clients should
     * check the hints set to see whether they need to perform
     * additional cropping.
     * @throws IOException
     * @throws ProcessorException
     * @see #read(File, SourceFormat, OperationList, Dimension,
     * ReductionFactor, Set< ReaderHint >)
     */
    public BufferedImage read(final ChannelSource channelSource,
                              final SourceFormat sourceFormat,
                              final OperationList ops,
                              final Dimension fullSize,
                              final ReductionFactor reductionFactor,
                              final Set<ReaderHint> hints)
            throws IOException, ProcessorException {
        return multiLevelAwareRead(channelSource.newChannel(), sourceFormat,
                ops, fullSize, reductionFactor, hints);
    }

    /**
     * @param inputSource  {@link ReadableByteChannel} or {@link File}
     * @param sourceFormat Format of the source image.
     * @param ops
     * @param fullSize     Full size of the source image.
     * @param rf           {@link ReductionFactor#factor} property will be modified to
     *                     reflect the reduction factor of the returned image.
     * @param hints        Will be populated by information returned by the reader.
     * @return BufferedImage best matching the given parameters. Clients
     * should check the hints set to see whether they need to perform
     * additional cropping.
     * @throws IOException
     * @throws ProcessorException
     * @see #read(File, SourceFormat, OperationList, Dimension,
     * ReductionFactor, Set< ReaderHint >)
     */
    private BufferedImage multiLevelAwareRead(final Object inputSource,
                                              final SourceFormat sourceFormat,
                                              final OperationList ops,
                                              final Dimension fullSize,
                                              final ReductionFactor rf,
                                              final Set<ReaderHint> hints)
            throws IOException, ProcessorException {
        final ImageReader reader = newImageReader(inputSource, sourceFormat);
        BufferedImage image = null;
        try {
            switch (sourceFormat) {
                case TIF:
                    Crop crop = new Crop();
                    crop.setFull(true);
                    Scale scale = new Scale();
                    scale.setMode(Scale.Mode.FULL);
                    for (Operation op : ops) {
                        if (op instanceof Crop) {
                            crop = (Crop) op;
                        } else if (op instanceof Scale) {
                            scale = (Scale) op;
                        }
                    }
                    image = readSmallestUsableSubimage(reader, crop, scale,
                            rf, hints);
                    break;
                // This is similar to the TIF case, except it doesn't scan for
                // subimages, which is costly to do.
                default:
                    crop = null;
                    for (Operation op : ops) {
                        if (op instanceof Crop) {
                            crop = (Crop) op;
                            break;
                        }
                    }
                    if (crop != null) {
                        image = tileAwareRead(reader, 0,
                                crop.getRectangle(fullSize), hints);
                    } else {
                        image = reader.read(0);
                    }
                    break;
            }
        } finally {
            reader.dispose();
        }
        if (image == null) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        }
        BufferedImage rgbImage = Java2dUtil.convertCustomToRgb(image);
        if (rgbImage != image) {
            logger.warn("Converted {} to RGB (this is very expensive)",
                    ops.getIdentifier());
        }
        return rgbImage;
    }

    /**
     * Reads the smallest image that can fulfill the given crop and scale from
     * a multi-resolution image.
     *
     * @param reader ImageReader with input source already set
     * @param crop   Requested crop
     * @param scale  Requested scale
     * @param rf     {@link ReductionFactor#factor} will be set to the reduction
     *               factor of the returned image.
     * @param hints  Will be populated by information returned by the reader.
     * @return The smallest image fitting the requested crop and scale
     * operations from the given reader.
     * @throws IOException
     */
    private BufferedImage readSmallestUsableSubimage(final ImageReader reader,
                                                     final Crop crop,
                                                     final Scale scale,
                                                     final ReductionFactor rf,
                                                     final Set<ReaderHint> hints)
            throws IOException {
        final Dimension fullSize = new Dimension(
                reader.getWidth(0), reader.getHeight(0));
        final Rectangle regionRect = crop.getRectangle(fullSize);
        final ImageReadParam param = reader.getDefaultReadParam();
        BufferedImage bestImage = null;
        if (scale.isNoOp()) {
            // ImageReader loves to read TIFFs into BufferedImages of type
            // TYPE_CUSTOM, which need to be redrawn into a new image of type
            // TYPE_INT_RGB at huge expense. The goal here is to directly get
            // a BufferedImage of TYPE_INT_RGB instead. An explanation of this
            // strategy, which may not even work anyway:
            // https://lists.apple.com/archives/java-dev/2005/Apr/msg00456.html
            bestImage = new BufferedImage(fullSize.width,
                    fullSize.height, BufferedImage.TYPE_INT_RGB);
            param.setDestination(bestImage);
            // An alternative would apparently be to use setDestinationType()
            // and then allow ImageReader.read() to create the BufferedImage
            // itself. But, that results in, "Destination type from
            // ImageReadParam does not match!" during writing.
            // param.setDestinationType(ImageTypeSpecifier.
            //        createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
            bestImage = tileAwareRead(reader, 0, regionRect, hints);
            logger.debug("Using a {}x{} source image (0x reduction factor)",
                    bestImage.getWidth(), bestImage.getHeight());
        } else {
            // Pyramidal TIFFs will have > 1 image, each half the dimensions of
            // the next larger. The "true" parameter tells getNumImages() to
            // scan for images, which seems to be necessary for at least some
            // files, but is slower.
            int numImages = reader.getNumImages(false);
            if (numImages > 1) {
                logger.debug("Detected {} subimage(s)", numImages - 1);
            } else if (numImages == -1) {
                numImages = reader.getNumImages(true);
                if (numImages > 1) {
                    logger.debug("Scan revealed {} subimage(s)", numImages - 1);
                }
            }
            if (numImages == 1) {
                bestImage = tileAwareRead(reader, 0, regionRect, hints);
                logger.debug("Using a {}x{} source image (0x reduction factor)",
                        bestImage.getWidth(), bestImage.getHeight());
            } else if (numImages > 1) {
                // Loop through the reduced images from smallest to largest to
                // find the first one that can supply the requested scale
                for (int i = numImages - 1; i >= 0; i--) {
                    final int reducedWidth = reader.getWidth(i);
                    final int reducedHeight = reader.getHeight(i);

                    final double reducedScale = (double) reducedWidth /
                            (double) fullSize.width;
                    boolean fits = false;
                    if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        fits = (scale.getWidth() / (float) regionRect.width <= reducedScale);
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        fits = (scale.getHeight() / (float) regionRect.height <= reducedScale);
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        fits = (scale.getWidth() / (float) regionRect.width <= reducedScale &&
                                scale.getHeight() / (float) regionRect.height <= reducedScale);
                    } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                        fits = (scale.getWidth() / (float) regionRect.width <= reducedScale &&
                                scale.getHeight() / (float) regionRect.height <= reducedScale);
                    } else if (scale.getPercent() != null) {
                        float pct = scale.getPercent();
                        fits = ((pct * fullSize.width) / (float) regionRect.width <= reducedScale &&
                                (pct * fullSize.height) / (float) regionRect.height <= reducedScale);
                    }
                    if (fits) {
                        rf.factor = ProcessorUtil.
                                getReductionFactor(reducedScale, 0).factor;
                        logger.debug("Using a {}x{} source image ({}x reduction factor)",
                                reducedWidth, reducedHeight, rf.factor);
                        final Rectangle reducedRect = new Rectangle(
                                (int) Math.round(regionRect.x * reducedScale),
                                (int) Math.round(regionRect.y * reducedScale),
                                (int) Math.round(regionRect.width * reducedScale),
                                (int) Math.round(regionRect.height * reducedScale));
                        bestImage = tileAwareRead(reader, i, reducedRect, hints);
                        break;
                    }
                }
            }
        }
        return bestImage;
    }

    /**
     * <p>Returns an image for the requested source area by reading the tiles
     * (or strips) of the source image and joining them into a single image.</p>
     * <p/>
     * <p>This method is intended to be compatible with all source images, no
     * matter the data layout (tiled or not). For some image types, including
     * GIF and PNG, a tiled reading strategy will not work, and so this method
     * will read the entire image.</p>
     * <p/>
     * <p>This method may populate <code>hints</code> with
     * {@link ReaderHint#ALREADY_CROPPED}, in which case cropping will have
     * already been performed according to the
     * <code>requestedSourceArea</code> parameter.</p>
     *
     * @param reader              ImageReader with input source already set
     * @param imageIndex          Index of the image to read from the ImageReader.
     * @param requestedSourceArea Source image area to retrieve. The returned
     *                            image will be this size or smaller if it
     *                            would overlap the right or bottom edge of the
     *                            source image.
     * @param hints               Will be populated by information returned by the reader.
     * @return Cropped image
     * @throws IOException
     * @throws IllegalArgumentException If the source image is not tiled.
     */
    private BufferedImage tileAwareRead(final ImageReader reader,
                                        final int imageIndex,
                                        final Rectangle requestedSourceArea,
                                        final Set<ReaderHint> hints)
            throws IOException, IllegalArgumentException {

        // These formats are uncooperative with the tile-aware loading
        // strategy.
        if (reader instanceof GIFImageReader ||
                reader instanceof PNGImageReader) {
            logger.debug("tileAwareRead(): reading full image {}", imageIndex);
            return reader.read(imageIndex);
        }

        final int imageWidth = reader.getWidth(imageIndex);
        final int imageHeight = reader.getHeight(imageIndex);
        final int tileWidth = reader.getTileWidth(imageIndex);
        final int tileHeight = reader.getTileHeight(imageIndex);
        final int numXTiles = (int) Math.ceil((double) imageWidth /
                (double) tileWidth);
        final int numYTiles = (int) Math.ceil((double) imageHeight /
                (double) tileHeight);
        final int tileX1 = (int) Math.floor((double) requestedSourceArea.x /
                (double) tileWidth);
        final int tileX2 = Math.min((int) Math.ceil((double) (requestedSourceArea.x +
                requestedSourceArea.width) / (double) tileWidth) - 1, numXTiles - 1);
        final int tileY1 = (int) Math.floor((double) requestedSourceArea.y /
                (double) tileHeight);
        final int tileY2 = Math.min((int) Math.ceil((double) (requestedSourceArea.y +
                requestedSourceArea.height) / (double) tileHeight) - 1, numYTiles - 1);
        final int offsetX = requestedSourceArea.x - tileX1 * tileWidth;
        final int offsetY = requestedSourceArea.y - tileY1 * tileHeight;

        if (tileHeight == 1) {
            logger.debug("tileAwareRead(): reading {}-column strip", numYTiles);
        } else {
            logger.debug("tileAwareRead(): reading tile rows {}-{} of {} and " +
                            "columns {}-{} of {} ({}x{} tiles; {}x{} offset)",
                    tileX1 + 1, tileX2 + 1, numXTiles,
                    tileY1 + 1, tileY2 + 1, numYTiles,
                    tileWidth, tileHeight, offsetX, offsetY);
        }

        BufferedImage outImage = null;
        // Copy the tile rasters into outImage
        for (int tx = tileX1, ix = 0; tx <= tileX2; tx++, ix++) {
            for (int ty = tileY1, iy = 0; ty <= tileY2; ty++, iy++) {
                final BufferedImage tile = reader.readTile(imageIndex, tx, ty);
                // ImageReader.readTileRaster() doesn't always work, so get a
                // Raster from the tile BufferedImage and translate it.
                final Raster raster = tile.getData().createTranslatedChild(
                        ix * tileWidth - offsetX,
                        iy * tileHeight - offsetY);
                if (ix == 0 && iy == 0) {
                    final int outImageType = tile.getColorModel().hasAlpha() ?
                            BufferedImage.TYPE_INT_ARGB :
                            BufferedImage.TYPE_INT_RGB;
                    outImage = new BufferedImage(
                            Math.min(requestedSourceArea.width, imageWidth - requestedSourceArea.x),
                            Math.min(requestedSourceArea.height, imageHeight - requestedSourceArea.y),
                            outImageType);
                }
                outImage.setData(raster);
            }
        }
        hints.add(ReaderHint.ALREADY_CROPPED);
        return outImage;
    }

}
