package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.ConfigurationException;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.redaction.Redaction;
import edu.illinois.library.cantaloupe.image.watermark.Position;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.watermark.Watermark;
import edu.illinois.library.cantaloupe.image.watermark.WatermarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * A collection of methods for operating on {@link BufferedImage}s.
 */
abstract class Java2dUtil {

    private static Logger logger = LoggerFactory.getLogger(Java2dUtil.class);

    /**
     * Redacts regions from the given image.
     *
     * @param baseImage Image to apply the watermark on top of.
     * @param appliedCrop Crop already applied to <code>baseImage</code>.
     * @param reductionFactor Reduction factor already applied to
     *                        <code>baseImage</code>.
     * @param redactions Regions of the image to redact.
     * @return Input image with redactions applied.
     */
    public static BufferedImage applyRedactions(final BufferedImage baseImage,
                                                final Crop appliedCrop,
                                                final ReductionFactor reductionFactor,
                                                final List<Redaction> redactions) {
        if (baseImage != null && redactions.size() > 0) {
            final long msec = System.currentTimeMillis();
            final Dimension imageSize = new Dimension(
                    baseImage.getWidth(), baseImage.getHeight());

            final Graphics2D g2d = baseImage.createGraphics();
            g2d.setColor(Color.black);

            for (final Redaction redaction : redactions) {
                final Rectangle redactionRegion =
                        redaction.getResultingRegion(imageSize, appliedCrop);
                redactionRegion.x *= reductionFactor.getScale();
                redactionRegion.y *= reductionFactor.getScale();
                redactionRegion.width *= reductionFactor.getScale();
                redactionRegion.height *= reductionFactor.getScale();

                if (!redactionRegion.isEmpty()) {
                    logger.debug("applyRedactions(): applying {} at {},{}/{}x{}",
                            redaction, redactionRegion.x, redactionRegion.y,
                            redactionRegion.width, redactionRegion.height);
                    g2d.fill(redactionRegion);
                } else {
                    logger.debug("applyRedactions(): {} is outside crop area; skipping",
                            redaction);
                }
            }
            g2d.dispose();
            logger.info("applyRedactions() executed in {} msec",
                    System.currentTimeMillis() - msec);
        }
        return baseImage;
    }

    /**
     * Applies the watermark to the given image.
     *
     * @param baseImage Image to apply the watermark on top of.
     * @param watermark Watermark to apply to the base image.
     * @return Watermarked image, or the input image if there is no watermark
     *         set in the application configuration.
     * @throws ConfigurationException
     * @throws IOException
     */
    public static BufferedImage applyWatermark(final BufferedImage baseImage,
                                               final Watermark watermark)
            throws ConfigurationException, IOException {
        BufferedImage markedImage = baseImage;
        final Dimension imageSize = new Dimension(baseImage.getWidth(),
                baseImage.getHeight());
        if (WatermarkService.shouldApplyToImage(imageSize)) {
            markedImage = overlayImage(baseImage,
                    getWatermarkImage(watermark),
                    watermark.getPosition(),
                    watermark.getInset());
        }
        return markedImage;
    }

    /**
     * @param baseImage
     * @param overlayImage
     * @param position
     * @param inset Inset in pixels.
     * @return
     */
    private static BufferedImage overlayImage(final BufferedImage baseImage,
                                              final BufferedImage overlayImage,
                                              final Position position,
                                              final int inset) {
        if (overlayImage != null && position != null) {
            final long msec = System.currentTimeMillis();
            int overlayX, overlayY;
            switch (position) {
                case TOP_LEFT:
                    overlayX = inset;
                    overlayY = inset;
                    break;
                case TOP_RIGHT:
                    overlayX = baseImage.getWidth() -
                            overlayImage.getWidth() - inset;
                    overlayY = inset;
                    break;
                case BOTTOM_LEFT:
                    overlayX = inset;
                    overlayY = baseImage.getHeight() -
                            overlayImage.getHeight() - inset;
                    break;
                // case BOTTOM_RIGHT: will be handled in default:
                case TOP_CENTER:
                    overlayX = (baseImage.getWidth() -
                            overlayImage.getWidth()) / 2;
                    overlayY = inset;
                    break;
                case BOTTOM_CENTER:
                    overlayX = (baseImage.getWidth() -
                            overlayImage.getWidth()) / 2;
                    overlayY = baseImage.getHeight() -
                            overlayImage.getHeight() - inset;
                    break;
                case LEFT_CENTER:
                    overlayX = inset;
                    overlayY = (baseImage.getHeight() -
                            overlayImage.getHeight()) / 2;
                    break;
                case RIGHT_CENTER:
                    overlayX = baseImage.getWidth() -
                            overlayImage.getWidth() - inset;
                    overlayY = (baseImage.getHeight() -
                            overlayImage.getHeight()) / 2;
                    break;
                case CENTER:
                    overlayX = (baseImage.getWidth() -
                            overlayImage.getWidth()) / 2;
                    overlayY = (baseImage.getHeight() -
                            overlayImage.getHeight()) / 2;
                    break;
                default: // bottom right
                    overlayX = baseImage.getWidth() -
                            overlayImage.getWidth() - inset;
                    overlayY = baseImage.getHeight() -
                            overlayImage.getHeight() - inset;
                    break;
            }

            final Graphics2D g2d = baseImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(baseImage, 0, 0, null);
            g2d.drawImage(overlayImage, overlayX, overlayY, null);
            g2d.dispose();
            logger.info("overlayImage() executed in {} msec",
                    System.currentTimeMillis() - msec);
        }
        return baseImage;
    }

    /**
     * <p>Copies the given BufferedImage of type
     * {@link BufferedImage#TYPE_CUSTOM} into a new image of type
     * {@link BufferedImage#TYPE_INT_RGB}, to make it work with the
     * rest of the image operation pipeline.</p>
     *
     * <p>This is extremely expensive and should be avoided if possible.</p>
     *
     * @param inImage Image to convert
     * @return A new BufferedImage of type RGB, or the input image if it
     * is not of custom type.
     */
    public static BufferedImage convertCustomToRgb(final BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage != null && inImage.getType() == BufferedImage.TYPE_CUSTOM) {
            final long msec = System.currentTimeMillis();

            outImage = new BufferedImage(inImage.getWidth(),
                    inImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = outImage.createGraphics();
            g.drawImage(inImage, 0, 0, null);
            g.dispose();

            logger.info("convertCustomToRgb() executed in {} msec",
                    System.currentTimeMillis() - msec);
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
        final Dimension croppedSize = crop.getResultingSize(
                new Dimension(inImage.getWidth(), inImage.getHeight()));
        BufferedImage croppedImage;
        if (crop.isNoOp() || (croppedSize.width == inImage.getWidth() &&
                croppedSize.height == inImage.getHeight())) {
            croppedImage = inImage;
        } else {
            final long msec = System.currentTimeMillis();
            final double scale = rf.getScale();
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

            logger.info("cropImage(): cropped {}x{} image to {} in {} msec",
                    inImage.getWidth(), inImage.getHeight(), crop,
                    System.currentTimeMillis() - msec);
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
        final long msec = System.currentTimeMillis();
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

            logger.info("filterImage(): filtered {}x{} image in {} msec",
                    inImage.getWidth(), inImage.getHeight(),
                    System.currentTimeMillis() - msec);
        }
        return filteredImage;
    }

    /**
     * @param watermark
     * @return Watermark image, or null if
     *         {@link WatermarkService#WATERMARK_FILE_CONFIG_KEY} is not set.
     * @throws IOException
     */
    public static BufferedImage getWatermarkImage(Watermark watermark)
            throws IOException {
        final ImageIoImageReader reader = new ImageIoImageReader();
        reader.setSource(watermark.getImage());
        reader.setFormat(Format.PNG);
        return reader.read();
    }

    public static BufferedImage removeAlpha(final BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage.getColorModel().hasAlpha()) {
            final long msec = System.currentTimeMillis();
            int newType;
            switch (inImage.getType()) {
                case BufferedImage.TYPE_4BYTE_ABGR:
                    newType = BufferedImage.TYPE_INT_BGR;
                    break;
                default:
                    newType = BufferedImage.TYPE_INT_RGB;
                    break;
            }
            outImage = new BufferedImage(inImage.getWidth(),
                    inImage.getHeight(), newType);
            Graphics2D g = outImage.createGraphics();
            g.drawImage(inImage, 0, 0, null);
            g.dispose();
            logger.info("removeAlpha(): converted BufferedImage type {} to " +
                    "RGB in {} msec", inImage.getType(),
                    System.currentTimeMillis() - msec);
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
            final long msec = System.currentTimeMillis();
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
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = rotatedImage.createGraphics();
            RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHints(hints);
            g2d.drawImage(inImage, tx, null);
            logger.info("rotateImage() executed in {} msec",
                    System.currentTimeMillis() - msec);
        }
        return rotatedImage;
    }

    /**
     * Scales an image using Graphics2D.
     *
     * @param inImage Image to scale
     * @param scale   Scale operation
     * @return Downscaled image, or the input image if the given scale is a
     *         no-op.
     */
    public static BufferedImage scaleImage(final BufferedImage inImage,
                                           final Scale scale) {
        return scaleImage(inImage, scale, new ReductionFactor(0), false);
    }

    /**
     * Scales an image using Graphics2D, taking an already-applied reduction
     * factor into account. In other words, the dimensions of the input image
     * have already been halved <code>rf</code> times but the given size is
     * relative to the full-sized image.
     *
     * @param inImage Image to scale
     * @param scale Requested size ignoring any reduction factor
     * @param rf Reduction factor that has already been applied to
     *           <code>inImage</code>
     * @param highQuality Whether to use a high-quality but more expensive
     *                    scaling method.
     * @return Downscaled image, or the input image if the given scale is a
     *         no-op.
     */
    public static BufferedImage scaleImage(final BufferedImage inImage,
                                           final Scale scale,
                                           final ReductionFactor rf,
                                           final boolean highQuality) {
        final Dimension sourceSize = new Dimension(
                inImage.getWidth(), inImage.getHeight());

        // Calculate the size that the image will need to be scaled to based
        // on the source image size, scale, and already-applied reduction
        // factor.
        Dimension resultingSize;
        if (scale.getPercent() != null) {
            resultingSize = new Dimension();
            resultingSize.width = (int) Math.round(sourceSize.width *
                    (scale.getPercent() / rf.getScale()));
            resultingSize.height = (int) Math.round(sourceSize.height *
                    (scale.getPercent() / rf.getScale()));
        } else {
            resultingSize = scale.getResultingSize(sourceSize);
        }

        BufferedImage scaledImage = inImage;
        if (!scale.isNoOp() && (resultingSize.width != sourceSize.width &&
                resultingSize.height != sourceSize.height)) {
            final long startMsec = System.currentTimeMillis();

            scaledImage = new BufferedImage(
                    resultingSize.width, resultingSize.height,
                    inImage.getType());

            final Graphics2D g2d = scaledImage.createGraphics();
            try {
                // The "non-high-quality" technique results in images with
                // noticeable aliasing at small scales.
                // See: https://community.oracle.com/docs/DOC-983611
                // http://stackoverflow.com/a/34266703/177529
                if (highQuality) {
                    g2d.drawImage(
                            inImage.getScaledInstance(
                                    resultingSize.width, resultingSize.height,
                                    Image.SCALE_SMOOTH),
                            0, 0, resultingSize.width, resultingSize.height, null);
                } else {
                    final RenderingHints hints = new RenderingHints(
                            RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.setRenderingHints(hints);
                    g2d.drawImage(inImage, 0, 0,
                            resultingSize.width, resultingSize.height, null);
                }
                logger.info("scaleImage(): scaled {}x{} image to {}x{} in {} msec",
                        sourceSize.width, sourceSize.height,
                        resultingSize.width, resultingSize.height,
                        System.currentTimeMillis() - startMsec);
            } finally {
                g2d.dispose();
            }
        }
        return scaledImage;
    }

    /**
     * @param inImage Image to transpose.
     * @param transpose The transpose operation.
     * @return Transposed image.
     */
    public static BufferedImage transposeImage(final BufferedImage inImage,
                                               final Transpose transpose) {
        final long msec = System.currentTimeMillis();
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
        BufferedImage outImage = op.filter(inImage, null);

        logger.info("transposeImage(): transposed image in {} msec",
                System.currentTimeMillis() - msec);
        return outImage;
    }

}
