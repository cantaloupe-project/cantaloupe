package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Transpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

/**
 * A collection of methods for operating on {@link BufferedImage}s.
 */
abstract class Java2dUtil {

    private static Logger logger = LoggerFactory.getLogger(Java2dUtil.class);

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
        final Dimension croppedSize = crop.getResultingSize(
                new Dimension(inImage.getWidth(), inImage.getHeight()));
        BufferedImage croppedImage;
        if (crop.isNoOp() || (croppedSize.width == inImage.getWidth() &&
                croppedSize.height == inImage.getHeight())) {
            logger.debug("No need to crop; skipping.");
            croppedImage = inImage;
        } else {
            logger.debug("Cropping {}x{} image: {}",
                    inImage.getWidth(), inImage.getHeight(), crop);
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

    public static BufferedImage removeAlpha(final BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage.getColorModel().hasAlpha()) {
            int newType;
            switch (inImage.getType()) {
                case BufferedImage.TYPE_4BYTE_ABGR:
                    newType = BufferedImage.TYPE_INT_BGR;
                    break;
                default:
                    newType = BufferedImage.TYPE_INT_RGB;
                    break;
            }
            logger.warn("Converting BufferedImage type {} to RGB (this is " +
                    "very expensive)", inImage.getType());
            outImage = new BufferedImage(inImage.getWidth(),
                    inImage.getHeight(), newType);
            Graphics2D g = outImage.createGraphics();
            g.drawImage(inImage, 0, 0, null);
            g.dispose();
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
                    BufferedImage.TYPE_INT_ARGB);
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
        final Dimension scaledSize = scale.getResultingSize(
                new Dimension(inImage.getWidth(), inImage.getHeight()));
        BufferedImage scaledImage;
        if (scale.isNoOp() || (scaledSize.width == inImage.getWidth() &&
                scaledSize.height == inImage.getHeight())) {
            logger.debug("No need to scale; skipping.");
            scaledImage = inImage;
        } else {
            logger.debug("Scaling {}x{} image to {}x{}",
                    inImage.getWidth(), inImage.getHeight(),
                    scaledSize.width, scaledSize.height);
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
     * no-op.
     */
    public static BufferedImage scaleImageWithG2d(final BufferedImage inImage,
                                                  final Scale scale,
                                                  final ReductionFactor rf,
                                                  final boolean highQuality) {
        final Dimension scaledSize = scale.getResultingSize(
                new Dimension(inImage.getWidth(), inImage.getHeight()));
        BufferedImage scaledImage;
        if (scale.isNoOp() || (scaledSize.width == inImage.getWidth() &&
                scaledSize.height == inImage.getHeight())) {
            logger.debug("No need to scale; skipping.");
            scaledImage = inImage;
        } else {
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
            logger.debug("Scaling {}x{} image to {}x{}",
                    inImage.getWidth(), inImage.getHeight(), width, height);
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
     * @return Transposed image.
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

}
