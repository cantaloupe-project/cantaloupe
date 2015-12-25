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
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

abstract class Java2dUtil {

    private static Logger logger = LoggerFactory.getLogger(Java2dUtil.class);

    /**
     * <p>BufferedImages with a type of <code>TYPE_CUSTOM</code> won't work
     * with various operations, so this method copies them into a new image of
     * type RGB.</p>
     *
     * <p>This is extremely expensive and should be avoided if possible.</p>
     *
     * @param inImage Image to convert
     * @return A new BufferedImage of type RGB, or the input image if it
     * already is RGB
     */
    public static BufferedImage convertToRgb(BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage != null && inImage.getType() == BufferedImage.TYPE_CUSTOM) {
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
    public static BufferedImage cropImage(BufferedImage inImage,
                                          Crop crop) {
        return cropImage(inImage, crop, 0);
    }

    /**
     * Crops the given image taking into account a reduction factor
     * (<code>reductionFactor</code>). In other words, the dimensions of the
     * input image have already been halved <code>reductionFactor</code> times
     * but the given region is relative to the full-sized image.
     *
     * @param inImage Image to crop
     * @param crop Crop operation
     * @param reductionFactor Number of times the dimensions of
     *                        <code>inImage</code> have already been halved
     *                        relative to the full-sized version
     * @return Cropped image, or the input image if the given operation is a
     * no-op.
     */
    public static BufferedImage cropImage(BufferedImage inImage,
                                          Crop crop,
                                          int reductionFactor) {
        BufferedImage croppedImage = inImage;
        if (!crop.isNoOp()) {
            final double scale = ProcessorUtil.getScale(reductionFactor);
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
    public static BufferedImage filterImage(BufferedImage inImage,
                                            Filter filter) {
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

    public static BufferedImage readImage(ReadableByteChannel readableChannel)
            throws IOException {
        return Java2dUtil.convertToRgb(ImageIO.read(
                ImageIO.createImageInputStream(readableChannel)));
    }

    public static BufferedImage readImage(File inputFile,
                                          SourceFormat sourceFormat,
                                          OperationList ops,
                                          Dimension fullSize,
                                          ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return doReadImageWithImageIo(inputFile, sourceFormat, ops, fullSize,
                reductionFactor);
    }

    public static BufferedImage readImage(ReadableByteChannel readableChannel,
                                          SourceFormat sourceFormat,
                                          OperationList ops,
                                          Dimension fullSize,
                                          ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return doReadImageWithImageIo(readableChannel, sourceFormat, ops,
                fullSize, reductionFactor);
    }

    /**
     * @param inputSource {@link ReadableByteChannel} or {@link File}
     * @param sourceFormat
     * @param ops
     * @param fullSize
     * @param reductionFactor
     * @return
     * @throws IOException
     * @throws ProcessorException
     */
    private static BufferedImage doReadImageWithImageIo(
            Object inputSource,
            SourceFormat sourceFormat,
            OperationList ops,
            Dimension fullSize,
            ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        BufferedImage image = null;
        switch (sourceFormat) {
            case BMP:
                ImageInputStream iis = ImageIO.createImageInputStream(inputSource);
                image = ImageIO.read(iis);
                break;
            case TIF:
                String tiffReader = Application.getConfiguration().
                        getString(Java2dProcessor.TIF_READER_CONFIG_KEY,
                                "TIFFImageReader");
                if (tiffReader.equals("TIFFImageReader")) {
                    image = readWithTiffImageReader(inputSource, ops,
                            fullSize, reductionFactor);
                } else {
                    RenderedImage ri = JaiUtil.readImageWithTiffImageDecoder(
                            inputSource, ops, fullSize, reductionFactor);
                    image = new BufferedImage(ri.getWidth(),
                            ri.getHeight(), BufferedImage.TYPE_INT_RGB);
                    image.setData(ri.getData());
                }
                break;
            default:
                Iterator<ImageReader> it = ImageIO.getImageReadersByMIMEType(
                        sourceFormat.getPreferredMediaType().toString());
                while (it.hasNext()) {
                    ImageReader reader = it.next();
                    try {
                        iis = ImageIO.createImageInputStream(inputSource);
                        reader.setInput(iis);
                        image = reader.read(0);
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
            logger.warn("Converting {} to RGB (this is very expensive)",
                    ops.getIdentifier());
        }
        return rgbImage;
    }

    /**
     * <p>Uses TIFFImageReader to load a TIFF.</p>
     *
     * <p>The TIFFImageReader class currently being used,
     * it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader, has several
     * issues:</p>
     *
     * <ul>
     *     <li>It sometimes sets BufferedImages to <code>TYPE_CUSTOM</code>,
     *     which necessitates an expensive redraw into a new BufferedImage of
     *     <code>TYPE_RGB</code>. Though, we seem to be able to work around
     *     this (see inline commentary in
     *     {@link #getSmallestUsableImage}).</li>
     *     <li>It throws an ArrayIndexOutOfBoundsException when a TIFF file
     *     contains a tag value greater than 6. (To inspect tag values, run
     *     <code>$ tiffdump &lt;file&gt;</code>.) (The Sun TIFFImageReader
     *     suffers from the same issue except it throws an
     *     IllegalArgumentException instead.)</li>
     *     <li>It renders some TIFFs with improper colors.</li>
     * </ul>
     *
     * <p><code>ImageIO.read()</code> would be an alternative, but it is not
     * usable because it also suffers from the <code>TYPE_CUSTOM</code> issue.
     * Also, it doesn't allow access to pyramidal TIFF sub-images, and just
     * generally doesn't provide any control over the reading process.</p>
     *
     * @param inputSource {@link ReadableByteChannel} or {@link File}
     * @param ops
     * @param fullSize
     * @param reductionFactor
     * @return
     * @throws IOException
     * @throws ProcessorException
     * @throws IllegalArgumentException
     * @see <a href="https://github.com/geosolutions-it/imageio-ext/blob/master/plugin/tiff/src/main/java/it/geosolutions/imageioimpl/plugins/tiff/TIFFImageReader.java">
     *     TIFFImageReader source</a>
     */
    private static BufferedImage readWithTiffImageReader(
            Object inputSource, OperationList ops, Dimension fullSize,
            ReductionFactor reductionFactor) throws IOException,
            ProcessorException {
        BufferedImage image = null;
        try {
            Iterator<ImageReader> it = ImageIO.
                    getImageReadersByMIMEType("image/tiff");
            ImageReader reader = it.next();
            // TODO: figure out why it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader is not found in the packaged jar
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
                ImageInputStream iis = ImageIO.createImageInputStream(inputSource);
                reader.setInput(iis);
                image = getSmallestUsableImage(reader, fullSize, crop, scale,
                        reductionFactor);
            } finally {
                reader.dispose();
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("TIFFImageReader failed to read {}",
                    ops.getIdentifier());
            throw e;
        }
        return image;
    }

    /**
     * Returns the smallest image fitting the requested size from the given
     * reader. Useful for e.g. pyramidal TIFF.
     *
     * @param reader ImageReader with input source already set
     * @param fullSize
     * @param crop Requested crop
     * @param scale Requested scale
     * @param rf Set by reference
     * @return
     * @throws IOException
     */
    private static BufferedImage getSmallestUsableImage(ImageReader reader,
                                                        Dimension fullSize,
                                                        Crop crop,
                                                        Scale scale,
                                                        ReductionFactor rf)
            throws IOException {
        BufferedImage bestImage = null;
        final ImageReadParam param = reader.getDefaultReadParam();
        if (scale.isNoOp()) {
            // ImageIO loves to read TIFFs into BufferedImages of type
            // TYPE_CUSTOM, which need to be redrawn into a new image of type
            // TYPE_INT_RGB at huge expense. The goal here is to directly get
            // a BufferedImage of TYPE_INT_RGB instead For an explanation of
            // this strategy, which may not even work anyway:
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
            reader.read(0, param);
        } else {
            // Pyramidal TIFFs will have > 1 image, each half the dimensions of
            // the next larger. The "true" parameter tells getNumImages() to
            // scan for images, which seems to be necessary for at least some
            // files, but is O^n with source image size.
            int numImages = reader.getNumImages(false);
            if (numImages > 1) {
                logger.debug("Detected pyramidal TIFF with {} levels",
                        numImages);
            } else if (numImages == -1) {
                numImages = reader.getNumImages(true);
                if (numImages > 1) {
                    logger.debug("Scan revealed pyramidal TIFF with {} levels",
                            numImages);
                }
            }
            if (numImages > 1) {
                final Rectangle regionRect = crop.getRectangle(fullSize);

                // Loop through the tiles from smallest to largest to find the
                // first one that fits the requested scale
                for (int i = numImages - 1; i >= 0; i--) {
                    final BufferedImage tile = reader.read(i);
                    final double tileScale = (double) tile.getWidth() /
                            (double) fullSize.width;
                    boolean fits = false;
                    if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        fits = (scale.getWidth() / (float) regionRect.width <= tileScale);
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        fits = (scale.getHeight() / (float) regionRect.height <= tileScale);
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        fits = (scale.getWidth() / (float) regionRect.width <= tileScale &&
                                scale.getHeight() / (float) regionRect.height <= tileScale);
                    } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                        fits = (scale.getWidth() / (float) regionRect.width <= tileScale &&
                                scale.getHeight() / (float) regionRect.height <= tileScale);
                    } else if (scale.getPercent() != null) {
                        float pct = scale.getPercent();
                        fits = ((pct * fullSize.width) / (float) regionRect.width <= tileScale &&
                                (pct * fullSize.height) / (float) regionRect.height <= tileScale);
                    }
                    if (fits) {
                        rf.factor = ProcessorUtil.getReductionFactor(tileScale, 0);
                        logger.debug("Using a {}x{} source tile ({}x reduction factor)",
                                tile.getWidth(), tile.getHeight(), rf.factor);
                        bestImage = tile;
                        break;
                    }
                }
            }
        }
        return bestImage;
    }

    /**
     * @param inImage Image to rotate
     * @param rotate Rotate operation
     * @return Rotated image, or the input image if the given rotation is a
     * no-op.
     */
    public static BufferedImage rotateImage(BufferedImage inImage,
                                            Rotate rotate) {
        BufferedImage rotatedImage = inImage;
        if (!rotate.isNoOp()) {
            double radians = Math.toRadians(rotate.getDegrees());
            int sourceWidth = inImage.getWidth();
            int sourceHeight = inImage.getHeight();
            int canvasWidth = (int) Math.round(Math.abs(sourceWidth *
                    Math.cos(radians)) + Math.abs(sourceHeight *
                    Math.sin(radians)));
            int canvasHeight = (int) Math.round(Math.abs(sourceHeight *
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
     * @param inImage
     * @param scale
     * @return
     */
    public static BufferedImage scaleImageWithAffineTransform(
            BufferedImage inImage, Scale scale) {
        BufferedImage scaledImage = inImage;
        if (!scale.isNoOp()) {
            double xScale = 0.0f, yScale = 0.0f;
            if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                xScale = scale.getWidth() / (double) inImage.getWidth();
                yScale = xScale;
            } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                yScale = scale.getHeight() / (double) inImage.getHeight();
                xScale = yScale;
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
                xScale = scale.getPercent();
                yScale = xScale;
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
     * @param inImage
     * @param scale
     * @return
     */
    public static BufferedImage scaleImageWithG2d(BufferedImage inImage,
                                                  Scale scale) {
        return scaleImageWithG2d(inImage, scale, 0, false);
    }

    /**
     * Scales an image using Graphics2D.
     *
     * @param inImage
     * @param scale
     * @param highQuality
     * @return
     */
    public static BufferedImage scaleImageWithG2d(BufferedImage inImage,
                                                  Scale scale,
                                                  boolean highQuality) {
        return scaleImageWithG2d(inImage, scale, 0, highQuality);
    }

    /**
     * Scales an image using Graphics2D, taking an already-applied reduction
     * factor into account. In other words, the dimensions of the input image
     * have already been halved rf times but the given size is relative to the
     * full-sized image.
     *
     * @param inImage The input image
     * @param scale Requested size ignoring any reduction factor
     * @param reductionFactor Reduction factor that has already been applied to
     *                        <code>inImage</code>
     * @param highQuality
     * @return Scaled image
     */
    public static BufferedImage scaleImageWithG2d(BufferedImage inImage,
                                                  Scale scale,
                                                  int reductionFactor,
                                                  boolean highQuality) {
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
                final double appliedScale = ProcessorUtil.
                        getScale(reductionFactor);
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
    public static BufferedImage transposeImage(BufferedImage inImage,
                                               Transpose transpose) {
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
          /*  case TIF: TODO: this doesn't write anything
                Iterator<ImageWriter> it = ImageIO.
                        getImageWritersByMIMEType("image/tiff");
                while (it.hasNext()) {
                    writer = it.next();
                    if (writer instanceof it.geosolutions.imageioimpl.
                            plugins.tiff.TIFFImageWriter) {
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
                        break;
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
