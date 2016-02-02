package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Image reader using ImageIO to efficiently read source images.
 */
class ImageIoImageReader {

    // Note: methods that return BufferedImages (for Java 2D) are arranged
    // toward the beginning of the class; methods that return RenderedImages
    // (for JAI) are toward the end.

    private static Logger logger = LoggerFactory.
            getLogger(ImageIoImageReader.class);

    public enum ReaderHint {
        ALREADY_CROPPED
    }

    /**
     * @param inputSource  {@link File} or {@link StreamSource}
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
            if (inputSource instanceof StreamSource) {
                inputSource = ((StreamSource) inputSource).newImageInputStream();
            } else if (inputSource instanceof File) {
                inputSource = ImageIO.createImageInputStream(inputSource);
            }
            final ImageReader reader = it.next();
            reader.setInput(inputSource);
            return reader;
        } else {
            throw new UnsupportedSourceFormatException(sourceFormat);
        }
    }

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by the ImageIO library.
     */
    public static Set<SourceFormat> supportedFormats() {
        final HashSet<SourceFormat> formats = new HashSet<>();
        for (String mediaType : ImageIO.getReaderMIMETypes()) {
            final SourceFormat sourceFormat =
                    SourceFormat.getSourceFormat(new MediaType(mediaType));
            if (sourceFormat != null && !sourceFormat.equals(SourceFormat.UNKNOWN)) {
                formats.add(sourceFormat);
            }
        }
        return formats;
    }

    /////////////////////// BufferedImage methods //////////////////////////

    /**
     * Expedient but not necessarily efficient method wrapping
     * {@link ImageIO#read} that reads a whole image (excluding subimages) in
     * one shot.
     *
     * @param inputStream Input stream to read.
     * @return BufferedImage guaranteed to not be of type
     * {@link BufferedImage#TYPE_CUSTOM}.
     * @throws IOException
     */
    public BufferedImage read(InputStream inputStream) throws IOException {
        final BufferedImage image = ImageIO.read(
                ImageIO.createImageInputStream(inputStream));
        final BufferedImage rgbImage = Java2dUtil.convertCustomToRgb(image);
        if (rgbImage != image) {
            logger.warn("Converted image to RGB (this is very expensive)");
        }
        return rgbImage;
    }

    /**
     * <p>Attempts to reads an image as efficiently as possible, utilizing its
     * tile layout and/or subimages, if possible.</p>
     *
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
     * @param hints           Will be populated by information returned by the
     *                        reader.
     * @return BufferedImage best matching the given parameters, guaranteed to
     *         not be of {@link BufferedImage#TYPE_CUSTOM}. Clients should
     *         check the hints set to see whether they need to perform
     *         additional cropping.
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
     * @param streamSource Source of image streams to read
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
    public BufferedImage read(final StreamSource streamSource,
                              final SourceFormat sourceFormat,
                              final OperationList ops,
                              final Dimension fullSize,
                              final ReductionFactor reductionFactor,
                              final Set<ReaderHint> hints)
            throws IOException, ProcessorException {
        return multiLevelAwareRead(streamSource.newImageInputStream(),
                sourceFormat, ops, fullSize, reductionFactor, hints);
    }

    /**
     * @param inputSource  {@link InputStream} or {@link File}
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
                    scale = new Scale();
                    scale.setMode(Scale.Mode.FULL);
                    for (Operation op : ops) {
                        if (op instanceof Crop) {
                            crop = (Crop) op;
                        } else if (op instanceof Scale) {
                            scale = (Scale) op;
                        }
                    }
                    if (crop != null) {
                        image = tileAwareRead(reader, 0,
                                crop.getRectangle(fullSize), scale, rf, hints);
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
            bestImage = tileAwareRead(reader, 0, regionRect, scale, rf, hints);
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
                bestImage = tileAwareRead(reader, 0, regionRect, scale, rf,
                        hints);
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
                        bestImage = tileAwareRead(reader, i, reducedRect,
                                scale, rf, hints);
                        break;
                    }
                }
            }
        }
        return bestImage;
    }

    /**
     * <p>Returns an image for the requested source area by reading the tiles
     * (or strips) of the source image and joining them into a single image.
     * Subsampling will be used if possible.</p>
     *
     * <p>This method is intended to be compatible with all source images, no
     * matter the data layout (tiled or not). For some image types, including
     * GIF and PNG, a tiled reading strategy will not work, and so this method
     * will read the entire image.</p>
     *
     * <p>This method may populate <code>hints</code> with
     * {@link ReaderHint#ALREADY_CROPPED}, in which case cropping will have
     * already been performed according to the
     * <code>requestedSourceArea</code> parameter.</p>
     *
     * @param reader              ImageReader with input source already set
     * @param imageIndex          Index of the image to read from the
     *                            ImageReader.
     * @param requestedSourceArea Source image area to retrieve. The returned
     *                            image will be this size or smaller if it
     *                            would overlap the right or bottom edge of the
     *                            source image.
     * @param scale               Scale that is to be applied to the returned
     *                            image. Will be used to calculate a
     *                            subsampling rate.
     * @param rf                  Will be populated with the reduction factor
     *                            of the returned image.
     * @param hints               Will be populated by information returned from
     *                            the reader.
     * @return Cropped image
     * @throws IOException
     * @throws IllegalArgumentException If the source image is not tiled.
     */
    private BufferedImage tileAwareRead(final ImageReader reader,
                                        final int imageIndex,
                                        final Rectangle requestedSourceArea,
                                        final Scale scale,
                                        final ReductionFactor rf,
                                        final Set<ReaderHint> hints)
            throws IOException, IllegalArgumentException {
        final Dimension fullSize = new Dimension(reader.getWidth(imageIndex),
                reader.getHeight(imageIndex));
        final Dimension scaledSize = scale.getResultingSize(fullSize);
        final float xScale = (float) scaledSize.width / (float) fullSize.width;
        final float yScale = (float) scaledSize.height / (float) fullSize.height;
        rf.factor = ProcessorUtil.
                getReductionFactor(Math.max(xScale, yScale), 0).factor;

        ImageReadParam param = reader.getDefaultReadParam();
        param.setSourceRegion(requestedSourceArea);
        if (rf.factor > 0) {
            logger.debug("tileAwareRead(): using subsampling factor of {}",
                    rf.factor);
            final int subsample = (int) Math.pow(2, rf.factor);
            param.setSourceSubsampling(subsample, subsample, 0, 0);
        }
        hints.add(ReaderHint.ALREADY_CROPPED);
        return reader.read(imageIndex, param);
    }

    ////////////////////////////////////////////////////////////////////////
    /////////////////////// RenderedImage methods //////////////////////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * Reads an image (excluding subimages).
     *
     * @param inputStream Input stream to read.
     * @param sourceFormat
     * @return RenderedImage
     * @throws IOException
     * @throws UnsupportedSourceFormatException
     */
    public RenderedImage readRendered(final InputStream inputStream,
                                      final SourceFormat sourceFormat)
            throws IOException, UnsupportedSourceFormatException {
        ImageReader reader = newImageReader(inputStream, sourceFormat);
        return reader.readAsRenderedImage(0, reader.getDefaultReadParam());
    }

    /**
     * <p>Attempts to reads an image as efficiently as possible, utilizing its
     * tile layout and/or subimages, if possible.</p>
     *
     * @param imageFile       Image file to read.
     * @param sourceFormat    Format of the source image.
     * @param ops
     * @param reductionFactor {@link ReductionFactor#factor} property will be
     *                        modified to reflect the reduction factor of the
     *                        returned image.
     * @return RenderedImage best matching the given parameters.
     * @throws IOException
     * @throws ProcessorException
     */
    public RenderedImage read(final File imageFile,
                              final SourceFormat sourceFormat,
                              final OperationList ops,
                              final ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return multiLevelAwareRead(imageFile, sourceFormat, ops,
                reductionFactor);
    }

    /**
     * @see #read(File, SourceFormat, OperationList, ReductionFactor)
     *
     * @param streamSource Source of image streams to read
     * @param sourceFormat Format of the source image
     * @param ops
     * @param reductionFactor {@link ReductionFactor#factor} property will be
     *                        modified to reflect the reduction factor of the
     *                        returned image.
     * @return BufferedImage best matching the given parameters.
     * @throws IOException
     * @throws ProcessorException
     * @see #read(File, SourceFormat, OperationList, ReductionFactor)
     */
    public RenderedImage read(final StreamSource streamSource,
                              final SourceFormat sourceFormat,
                              final OperationList ops,
                              final ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return multiLevelAwareRead(streamSource, sourceFormat,
                ops, reductionFactor);
    }

    /**
     * @param inputSource {@link StreamSource} or {@link File}
     * @param sourceFormat Format of the source image.
     * @param ops
     * @param rf {@link ReductionFactor#factor} property will be modified to
     *           reflect the reduction factor of the returned image.
     * @return BufferedImage best matching the given parameters.
     * @throws IOException
     * @throws ProcessorException
     * @see #read(File, SourceFormat, OperationList, ReductionFactor)
     */
    private RenderedImage multiLevelAwareRead(final Object inputSource,
                                              final SourceFormat sourceFormat,
                                              final OperationList ops,
                                              final ReductionFactor rf)
            throws IOException, ProcessorException {
        final ImageReader reader = newImageReader(inputSource, sourceFormat);
        RenderedImage image = null;
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
                    image = readSmallestUsableSubimage(reader, crop, scale, rf);
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
                        image = reader.readAsRenderedImage(0,
                                reader.getDefaultReadParam());
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
        return image;
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
     * @return The smallest image fitting the requested crop and scale
     * operations from the given reader.
     * @throws IOException
     */
    private RenderedImage readSmallestUsableSubimage(final ImageReader reader,
                                                     final Crop crop,
                                                     final Scale scale,
                                                     final ReductionFactor rf)
            throws IOException {
        final Dimension fullSize = new Dimension(
                reader.getWidth(0), reader.getHeight(0));
        final Rectangle regionRect = crop.getRectangle(fullSize);
        final ImageReadParam param = reader.getDefaultReadParam();
        RenderedImage bestImage = null;
        if (scale.isNoOp()) {
            bestImage = reader.readAsRenderedImage(0, param);
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
                bestImage = reader.read(0, param);
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
                        bestImage = reader.readAsRenderedImage(i, param);
                        break;
                    }
                }
            }
        }
        return bestImage;
    }

}
