package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Transpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A collection of methods for reading, operating on, and writing
 * {@link BufferedImage}s.
 */
abstract class Java2dUtil {

    public enum ReaderHint {
        ALREADY_CROPPED
    }

    private static Logger logger = LoggerFactory.getLogger(Java2dUtil.class);

    /**
     * <p>Copies the given BufferedImage into a new image of type
     * {@link BufferedImage#TYPE_INT_RGB}, to make it compatible with the
     * rest of the application image operation pipeline.</p>
     *
     * <p>This is extremely expensive and should be avoided if possible.</p>
     *
     * @param inImage Image to convert
     * @return A new BufferedImage of type RGB, or the input image if it
     * already is RGB.
     */
    public static BufferedImage convertToRgb(final BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage != null && inImage.getType() != BufferedImage.TYPE_INT_RGB) {
            outImage = new BufferedImage(inImage.getWidth(),
                    inImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = outImage.createGraphics();
            g.drawImage(inImage, 0, 0, null);
            g.dispose();
        }
        return outImage;
    }

    /**
     * @param inImage Image to crop
     * @param crop Crop operation
     * @return Cropped image, or the input image if the given operation is a
     * no-op.
     */
    public static BufferedImage cropImage(final BufferedImage inImage,
                                          final Crop crop) {
        return cropImage(inImage, crop, new ReductionFactor());
    }

    /**
     * Crops the given image taking into account a reduction factor
     * (<code>reductionFactor</code>). In other words, the dimensions of the
     * input image have already been halved <code>reductionFactor</code> times
     * but the given region is relative to the full-sized image.
     *
     * @param inImage Image to crop
     * @param crop Crop operation
     * @param rf Number of times the dimensions of <code>inImage</code> have
     *           already been halved relative to the full-sized version
     * @return Cropped image, or the input image if the given operation is a
     * no-op.
     */
    public static BufferedImage cropImage(final BufferedImage inImage,
                                          final Crop crop,
                                          final ReductionFactor rf) {
        BufferedImage croppedImage = inImage;
        if (!crop.isNoOp()) {
            final double scale = ProcessorUtil.getScale(rf);
            final double regionX = crop.getX() * scale;
            final double regionY = crop.getY() * scale;
            final double regionWidth = crop.getWidth() * scale;
            final double regionHeight = crop.getHeight() * scale;

            int x, y, requestedWidth, requestedHeight, croppedWidth,
                    croppedHeight;
            if (crop.getUnit().equals(Crop.Unit.PERCENT)) {
                x = (int) Math.round(regionX * inImage.getWidth());
                y = (int) Math.round(regionY * inImage.getHeight());
                requestedWidth = (int) Math.round(regionWidth  *
                        inImage.getWidth());
                requestedHeight = (int) Math.round(regionHeight *
                        inImage.getHeight());
            } else {
                x = (int) Math.round(regionX);
                y = (int) Math.round(regionY);
                requestedWidth = (int) Math.round(regionWidth);
                requestedHeight = (int) Math.round(regionHeight);
            }
            // BufferedImage.getSubimage() will protest if asked for more
            // width/height than is available
            croppedWidth = (x + requestedWidth > inImage.getWidth()) ?
                    inImage.getWidth() - x : requestedWidth;
            croppedHeight = (y + requestedHeight > inImage.getHeight()) ?
                    inImage.getHeight() - y : requestedHeight;
            croppedImage = inImage.getSubimage(x, y, croppedWidth,
                    croppedHeight);
        }
        return croppedImage;
    }

    /**
     * @param inImage Image to filter
     * @param filter Filter operation
     * @return Filtered image, or the input image if the given filter operation
     * is a no-op.
     */
    public static BufferedImage filterImage(final BufferedImage inImage,
                                            final Filter filter) {
        BufferedImage filteredImage = inImage;
        switch (filter) {
            case GRAY:
                filteredImage = new BufferedImage(inImage.getWidth(),
                        inImage.getHeight(),
                        BufferedImage.TYPE_BYTE_GRAY);
                break;
            case BITONAL:
                filteredImage = new BufferedImage(inImage.getWidth(),
                        inImage.getHeight(),
                        BufferedImage.TYPE_BYTE_BINARY);
                break;
        }
        if (filteredImage != inImage) {
            Graphics2D g2d = filteredImage.createGraphics();
            g2d.drawImage(inImage, 0, 0, null);
        }
        return filteredImage;
    }

    /**
     * @return Set of all output formats supported by ImageIO.
     */
    public static Set<OutputFormat> imageIoOutputFormats() {
        final String[] writerMimeTypes = ImageIO.getWriterMIMETypes();
        final Set<OutputFormat> outputFormats = new HashSet<>();
        for (OutputFormat outputFormat : OutputFormat.values()) {
            for (String mimeType : writerMimeTypes) {
                if (outputFormat.getMediaType().equals(mimeType.toLowerCase())) {
                    outputFormats.add(outputFormat);
                }
            }
        }
        return outputFormats;
    }

    /**
     * Simple and not necessarily efficient method wrapping
     * {@link ImageIO#read}.
     *
     * @param readableChannel Image channel to read.
     * @return RGB BufferedImage
     * @throws IOException
     */
    public static BufferedImage readImage(ReadableByteChannel readableChannel)
            throws IOException {
        final BufferedImage image = ImageIO.read(
                ImageIO.createImageInputStream(readableChannel));
        final BufferedImage rgbImage = Java2dUtil.convertToRgb(image);
        if (rgbImage != image) {
            logger.warn("Converted image to RGB (this is very expensive)");
        }
        return rgbImage;
    }

    /**
     * <p>Attempts to reads an image as efficiently as possible, utilizing its
     * tile layout and/or sub-images, if possible.</p>
     *
     * <p>After reading, clients should check the reader hints to see whether
     * the returned image will require cropping.</p>
     *
     * @param imageFile Image file to read.
     * @param sourceFormat
     * @param ops
     * @param fullSize Full size of the source image.
     * @param reductionFactor {@link ReductionFactor#factor} property will be
     *                        modified to reflect the reduction factor of the
     *                        returned image.
     * @param hints Will be populated by information returned by the reader.
     * @return RGB BufferedImage best matching the given parameters. Clients
     * should check the hints set to see whether they need to perform
     * additional cropping.
     * @throws IOException
     * @throws ProcessorException
     */
    public static BufferedImage readImage(final File imageFile,
                                          final SourceFormat sourceFormat,
                                          final OperationList ops,
                                          final Dimension fullSize,
                                          final ReductionFactor reductionFactor,
                                          final Set<ReaderHint> hints)
            throws IOException, ProcessorException {
        return doReadImage(imageFile, sourceFormat, ops, fullSize,
                reductionFactor, hints);
    }

    /**
     * @see #readImage(File, SourceFormat, OperationList, Dimension,
     * ReductionFactor, Set< ReaderHint >)
     *
     * @param readableChannel Image channel to read.
     * @param sourceFormat
     * @param ops
     * @param fullSize Full size of the source image.
     * @param reductionFactor {@link ReductionFactor#factor} property will be
     *                        modified to reflect the reduction factor of the
     *                        returned image.
     * @param hints Will be populated by information returned by the reader.
     * @return RGB BufferedImage best matching the given parameters. Clients
     * should check the hints set to see whether they need to perform
     * additional cropping.
     * @throws IOException
     * @throws ProcessorException
     */
    public static BufferedImage readImage(final ReadableByteChannel readableChannel,
                                          final SourceFormat sourceFormat,
                                          final OperationList ops,
                                          final Dimension fullSize,
                                          final ReductionFactor reductionFactor,
                                          final Set<ReaderHint> hints)
            throws IOException, ProcessorException {
        return doReadImage(readableChannel, sourceFormat, ops,
                fullSize, reductionFactor, hints);
    }

    /**
     * @see #readImage(File, SourceFormat, OperationList, Dimension,
     * ReductionFactor, Set< ReaderHint >)
     *
     * @param inputSource {@link ReadableByteChannel} or {@link File}
     * @param sourceFormat
     * @param ops
     * @param fullSize Full size of the source image.
     * @param rf {@link ReductionFactor#factor} property will be modified to
     *           reflect the reduction factor of the returned image.
     * @param hints Will be populated by information returned by the reader.
     * @return RGB BufferedImage best matching the given parameters. Clients
     * should check the hints set to see whether they need to perform
     * additional cropping.
     * @throws IOException
     * @throws ProcessorException
     */
    private static BufferedImage doReadImage(final Object inputSource,
                                             final SourceFormat sourceFormat,
                                             final OperationList ops,
                                             final Dimension fullSize,
                                             final ReductionFactor rf,
                                             final Set<ReaderHint> hints)
            throws IOException, ProcessorException {
        BufferedImage image = null;
        switch (sourceFormat) {
            case TIF:
                Iterator<ImageReader> it = ImageIO.
                        getImageReadersByMIMEType("image/tiff");
                if (it.hasNext()) {
                    ImageReader reader = it.next();
                    try {
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
                        ImageInputStream iis =
                                ImageIO.createImageInputStream(inputSource);
                        reader.setInput(iis);
                        image = readSmallestUsableImage(reader, crop, scale,
                                rf, hints);
                    } finally {
                        reader.dispose();
                    }
                }
                break;
            // This is similar to the TIF case; the main difference is that it
            // doesn't scan for sub-images, which is costly to do.
            default:
                it = ImageIO.getImageReadersByMIMEType(
                        sourceFormat.getPreferredMediaType().toString());
                if (it.hasNext()) {
                    ImageReader reader = it.next();
                    try {
                        ImageInputStream iis =
                                ImageIO.createImageInputStream(inputSource);
                        reader.setInput(iis);

                        Crop crop = null;
                        for (Operation op : ops) {
                            if (op instanceof Crop) {
                                crop = (Crop) op;
                                break;
                            }
                        }
                        if (reader.isImageTiled(0) && crop != null) {
                            image = readImageFromTiles(reader, 0,
                                    crop.getRectangle(fullSize));
                            hints.add(ReaderHint.ALREADY_CROPPED);
                        } else {
                            image = reader.read(0);
                        }
                    } finally {
                        reader.dispose();
                    }
                }
                break;
        }
        if (image == null) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        }
        BufferedImage rgbImage = Java2dUtil.convertToRgb(image);
        if (rgbImage != image) {
            logger.warn("Converted {} to RGB (this is very expensive)",
                    ops.getIdentifier());
        }
        return rgbImage;
    }

    /**
     * Reads the smallest usable image from a multi-resolution image.
     *
     * @param reader ImageReader with input source already set
     * @param crop Requested crop
     * @param scale Requested scale
     * @param rf {@link ReductionFactor#factor} will be set to the reduction
     *           factor of the returned image.
     * @param hints Will be populated by information returned by the reader.
     * @return The smallest image fitting the requested crop and scale
     * operations from the given reader.
     * @throws IOException
     */
    private static BufferedImage readSmallestUsableImage(
            final ImageReader reader,
            final Crop crop,
            final Scale scale,
            final ReductionFactor rf,
            final Set<ReaderHint> hints) throws IOException {
        BufferedImage bestImage = null;
        final Dimension fullSize = new Dimension(reader.getWidth(0),
                reader.getHeight(0));
        final Rectangle regionRect = crop.getRectangle(fullSize);
        final ImageReadParam param = reader.getDefaultReadParam();
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
            bestImage = readImageFromTiles(reader, 0, regionRect);
            hints.add(ReaderHint.ALREADY_CROPPED);
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
                bestImage = readImageFromTiles(reader, 0, regionRect);
                hints.add(ReaderHint.ALREADY_CROPPED);
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
                        bestImage = readImageFromTiles(reader, i, reducedRect);
                        hints.add(ReaderHint.ALREADY_CROPPED);
                        break;
                    }
                }
            }
        }
        return bestImage;
    }

    /**
     * <p>Returns an image for the requested source area by reading the tiles
     * of the source image and joining them into a single image.</p>
     *
     * <p>Clients should check that {@link ImageReader#isImageTiled(int)}
     * returns <code>true</code> before calling this method.</p>
     *
     * <p>This method performs cropping according to the
     * <code>requestedSourceArea</code> parameter and thus obviates the need
     * for an immediately-post-read cropping step.</p>
     *
     * @param reader ImageReader with input source already set
     * @param imageIndex Index of the image to read from the ImageReader.
     * @param requestedSourceArea Source image area to retrieve. The returned
     *                            image will be this size or smaller if it
     *                            would overlap the right or bottom edge of the
     *                            source image.
     * @return Cropped image
     * @throws IOException
     * @throws IllegalArgumentException If the source image is not tiled.
     */
    private static BufferedImage readImageFromTiles(
            final ImageReader reader,
            final int imageIndex,
            final Rectangle requestedSourceArea)
            throws IOException, IllegalArgumentException {
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

        logger.debug("Reading tile rows {}-{} of {} and columns {}-{} of {} " +
                "({}x{} tiles; {}x{} offset)",
                tileX1 + 1, tileX2 + 1, numXTiles,
                tileY1 + 1, tileY2 + 1, numYTiles,
                tileWidth, tileHeight, offsetX, offsetY);

        final BufferedImage outImage = new BufferedImage(
                Math.min(requestedSourceArea.width, imageWidth - requestedSourceArea.x),
                Math.min(requestedSourceArea.height, imageHeight - requestedSourceArea.y),
                BufferedImage.TYPE_INT_RGB);

        // Copy the tile rasters into outImage
        for (int tx = tileX1, ix = 0; tx <= tileX2; tx++, ix++) {
            for (int ty = tileY1, iy = 0; ty <= tileY2; ty++, iy++) {
                // ImageReader.readTileRaster() doesn't always work, so get a
                // Raster from the tile's BufferedImage and translate it.
                final Raster raster = reader.readTile(imageIndex, tx, ty).
                        getData().createTranslatedChild(
                        ix * tileWidth - offsetX,
                        iy * tileHeight - offsetY);
                outImage.setData(raster);
            }
        }
        return outImage;
    }

    /**
     * @param inImage Image to rotate
     * @param rotate Rotate operation
     * @return Rotated image, or the input image if the given rotation is a
     * no-op.
     */
    public static BufferedImage rotateImage(final BufferedImage inImage,
                                            final Rotate rotate) {
        BufferedImage rotatedImage = inImage;
        if (!rotate.isNoOp()) {
            final double radians = Math.toRadians(rotate.getDegrees());
            final int sourceWidth = inImage.getWidth();
            final int sourceHeight = inImage.getHeight();
            final int canvasWidth = (int) Math.round(Math.abs(sourceWidth *
                    Math.cos(radians)) + Math.abs(sourceHeight *
                    Math.sin(radians)));
            final int canvasHeight = (int) Math.round(Math.abs(sourceHeight *
                    Math.cos(radians)) + Math.abs(sourceWidth *
                    Math.sin(radians)));

            // note: operations happen in reverse order of declaration
            AffineTransform tx = new AffineTransform();
            // 3. translate the image to the center of the "canvas"
            tx.translate(canvasWidth / 2, canvasHeight / 2);
            // 2. rotate it
            tx.rotate(radians);
            // 1. translate the image so that it is rotated about the center
            tx.translate(-sourceWidth / 2, -sourceHeight / 2);

            rotatedImage = new BufferedImage(canvasWidth, canvasHeight,
                    inImage.getType());
            Graphics2D g2d = rotatedImage.createGraphics();
            RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHints(hints);
            g2d.drawImage(inImage, tx, null);
        }
        return rotatedImage;
    }

    /**
     * Scales an image using an AffineTransform.
     *
     * @param inImage Image to scale
     * @param scale Scale operation
     * @return Downscaled image, or the input image if the given scale is a
     * no-op.
     */
    public static BufferedImage scaleImageWithAffineTransform(
            BufferedImage inImage, Scale scale) {
        BufferedImage scaledImage = inImage;
        if (!scale.isNoOp()) {
            double xScale = 0.0f, yScale = 0.0f;
            if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                xScale = yScale = scale.getWidth() / (double) inImage.getWidth();
            } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                xScale = yScale = scale.getHeight() / (double) inImage.getHeight();
            } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                xScale = scale.getWidth() / (double) inImage.getWidth();
                yScale = scale.getHeight() / (double) inImage.getHeight();
            } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                double hScale = (double) scale.getWidth() /
                        (double) inImage.getWidth();
                double vScale = (double) scale.getHeight() /
                        (double) inImage.getHeight();
                xScale = inImage.getWidth() * Math.min(hScale, vScale) / 100f;
                yScale = inImage.getHeight() * Math.min(hScale, vScale) / 100f;
            } else if (scale.getPercent() != null) {
                xScale = yScale = scale.getPercent();
            }
            int width = (int) Math.round(inImage.getWidth() * xScale);
            int height = (int) Math.round(inImage.getHeight() * yScale);
            scaledImage = new BufferedImage(width, height, inImage.getType());
            AffineTransform at = new AffineTransform();
            at.scale(xScale, yScale);
            AffineTransformOp scaleOp = new AffineTransformOp(at,
                    AffineTransformOp.TYPE_BILINEAR);
            scaledImage = scaleOp.filter(inImage, scaledImage);
        }
        return scaledImage;
    }

    /**
     * Scales an image using Graphics2D.
     *
     * @param inImage Image to scale
     * @param scale Scale operation
     * @return Downscaled image, or the input image if the given scale is a
     * no-op.
     */
    public static BufferedImage scaleImageWithG2d(final BufferedImage inImage,
                                                  final Scale scale) {
        return scaleImageWithG2d(inImage, scale, new ReductionFactor(0), false);
    }

    /**
     * Scales an image using Graphics2D.
     *
     * @param inImage Image to scale.
     * @param scale Scale operation.
     * @param highQuality Whether to use a high-quality but more expensive
     *                    scaling method.
     * @return Downscaled image, or the input image if the given scale is a
     * no-op.
     */
    public static BufferedImage scaleImageWithG2d(final BufferedImage inImage,
                                                  final Scale scale,
                                                  final boolean highQuality) {
        return scaleImageWithG2d(inImage, scale, new ReductionFactor(0),
                highQuality);
    }

    /**
     * Scales an image using Graphics2D, taking an already-applied reduction
     * factor into account. In other words, the dimensions of the input image
     * have already been halved rf times but the given size is relative to the
     * full-sized image.
     *
     * @param inImage Image to scale
     * @param scale Requested size ignoring any reduction factor
     * @param rf Reduction factor that has already been applied to
     *           <code>inImage</code>
     * @param highQuality Whether to use a high-quality but more expensive
     *                    scaling method.
     * @return Downscaled image, or the input image if the given scale is a
     * no-op.
     */
    public static BufferedImage scaleImageWithG2d(final BufferedImage inImage,
                                                  final Scale scale,
                                                  final ReductionFactor rf,
                                                  final boolean highQuality) {
        BufferedImage scaledImage = inImage;
        if (!scale.isNoOp()) {
            final int sourceWidth = inImage.getWidth();
            final int sourceHeight = inImage.getHeight();
            int width = 0, height = 0;
            if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                width = scale.getWidth();
                height = sourceHeight * width / sourceWidth;
            } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                height = scale.getHeight();
                width = sourceWidth * height / sourceHeight;
            } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                width = scale.getWidth();
                height = scale.getHeight();
            } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                final double hScale = (double) scale.getWidth() /
                        (double) sourceWidth;
                final double vScale = (double) scale.getHeight() /
                        (double) sourceHeight;
                width = (int) Math.round(sourceWidth *
                        Math.min(hScale, vScale));
                height = (int) Math.round(sourceHeight *
                        Math.min(hScale, vScale));
            } else if (scale.getPercent() != null) {
                final double reqScale = scale.getPercent();
                final double appliedScale = ProcessorUtil.getScale(rf);
                final double pct = reqScale / appliedScale;
                width = (int) Math.round(sourceWidth * pct);
                height = (int) Math.round(sourceHeight * pct);
            }
            scaledImage = new BufferedImage(width, height,
                    inImage.getType());

            final Graphics2D g2d = scaledImage.createGraphics();
            // The "non-high-quality" technique results in images with
            // noticeable aliasing at small scales.
            // See: https://community.oracle.com/docs/DOC-983611
            // http://stackoverflow.com/a/34266703/177529
            if (highQuality) {
                g2d.drawImage(
                        inImage.getScaledInstance(width, height, Image.SCALE_SMOOTH),
                        0, 0, width, height, null);
            } else {
                final RenderingHints hints = new RenderingHints(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHints(hints);
                g2d.drawImage(inImage, 0, 0, width, height, null);
            }
            g2d.dispose();
        }
        return scaledImage;
    }

    /**
     * @param inImage Image to transpose.
     * @param transpose The transpose operation.
     * @return Transposed image, or the input image if the given transpose
     * operation is a no-op.
     */
    public static BufferedImage transposeImage(final BufferedImage inImage,
                                               final Transpose transpose) {
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        switch (transpose) {
            case HORIZONTAL:
                tx.translate(-inImage.getWidth(null), 0);
                break;
            case VERTICAL:
                tx.translate(0, -inImage.getHeight(null));
                break;
        }
        AffineTransformOp op = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_BILINEAR);
        return op.filter(inImage, null);
    }

    /**
     * Writes an image to the given output stream.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param writableChannel Channel to write the image to
     * @throws IOException
     */
    public static void writeImage(BufferedImage image,
                                  OutputFormat outputFormat,
                                  WritableByteChannel writableChannel)
            throws IOException {
        switch (outputFormat) {
            case JPG:
                // JPEG doesn't support alpha, so convert to RGB or else the
                // client will interpret as CMYK
                if (image.getColorModel().hasAlpha()) {
                    logger.warn("Converting RGBA BufferedImage to RGB (this is very expensive)");
                    BufferedImage rgbImage = new BufferedImage(
                            image.getWidth(), image.getHeight(),
                            BufferedImage.TYPE_INT_RGB);
                    rgbImage.createGraphics().drawImage(image, null, 0, 0);
                    image = rgbImage;
                }
                // TurboJpegImageWriter is used automatically if libjpeg-turbo
                // is available in java.library.path:
                // https://github.com/geosolutions-it/imageio-ext/wiki/TurboJPEG-plugin
                Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
                ImageWriter writer = (ImageWriter) iter.next();
                try {
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(Application.getConfiguration().
                            getFloat(Java2dProcessor.JPG_QUALITY_CONFIG_KEY, 0.7f));
                    param.setCompressionType("JPEG");
                    ImageOutputStream os = ImageIO.createImageOutputStream(writableChannel);
                    writer.setOutput(os);
                    IIOImage iioImage = new IIOImage(image, null, null);
                    writer.write(null, iioImage, param);
                } finally {
                    writer.dispose();
                }
                break;
            /*case PNG: // an alternative in case ImageIO.write() ever causes problems
                writer = ImageIO.getImageWritersByFormatName("png").next();
                ImageOutputStream os = ImageIO.createImageOutputStream(writableChannel);
                writer.setOutput(os);
                writer.write(image);
                break;*/
          /*  case TIF: TODO: try this
                Iterator<ImageWriter> it = ImageIO.
                        getImageWritersByMIMEType("image/tiff");
                if (it.hasNext()) {
                    writer = it.next();
                    try {
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        param.setDestinationType(ImageTypeSpecifier.
                                createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
                        ImageOutputStream os = ImageIO.createImageOutputStream(writableChannel);
                        writer.setOutput(os);
                        IIOImage iioImage = new IIOImage(image, null, null);
                        writer.write(null, iioImage, param);
                    } finally {
                        writer.dispose();
                    }
                }
                break; */
            default:
                // TODO: jp2 doesn't seem to work
                ImageIO.write(image, outputFormat.getExtension(),
                        ImageIO.createImageOutputStream(writableChannel));
                break;
        }
    }

}
