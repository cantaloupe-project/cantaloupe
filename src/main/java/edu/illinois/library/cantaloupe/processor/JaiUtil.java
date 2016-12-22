package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Color;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Sharpen;
import edu.illinois.library.cantaloupe.image.watermark.Position;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Transpose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.TransposeDescriptor;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;

abstract class JaiUtil {

    private static Logger logger = LoggerFactory.getLogger(JaiUtil.class);

    /**
     * Applies the watermark to the given image.
     *
     * TODO: this doesn't work properly with rotations. JaiProcessor is
     * currently using {@link Java2dUtil#applyWatermark(BufferedImage)}
     * instead.
     *
     * @param baseImage Image to apply the watermark on top of.
     * @return Watermarked image, or the input image if there is no watermark
     *         set in the application configuration.
     * @throws IOException

    static RenderedOp applyWatermark(final RenderedOp baseImage)
    throws ConfigurationException, IOException {
    RenderedOp markedImage = baseImage;
    final Dimension imageSize = new Dimension(baseImage.getWidth(),
    baseImage.getHeight());
    if (WatermarkService.shouldApplyToImage(imageSize)) {
    final Watermark watermark = WatermarkService.newWatermark();
    markedImage = overlayImage(baseImage,
    Java2dUtil.getWatermarkImage(watermark),
    watermark.getPosition(),
    watermark.getInset());
    }
    return markedImage;
    } */

    /**
     * Reduces an image's component size to 8 bits if greater.
     *
     * @param inImage Image to reduce
     * @return Reduced image, or the input image if it already is 8 bits or
     *         less.
     */
    static RenderedOp convertTo8Bits(RenderedOp inImage) {
        if (inImage.getColorModel().getComponentSize(0) != 8) {
            // This seems to clip the color depth to 8-bit. Not sure why it
            // works.
            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            inImage = JAI.create("format", pb, inImage.getRenderingHints());
        }
        return inImage;
    }

    /**
     * @param inImage Image to crop
     * @param crop    Crop operation
     * @return Cropped image, or the input image if the given operation is a
     *         no-op.
     */
    static RenderedOp cropImage(RenderedOp inImage, Crop crop) {
        return cropImage(inImage, crop, new ReductionFactor(0));
    }

    /**
     * Crops the given image taking into account a reduction factor
     * (<code>reductionFactor</code>). In other words, the dimensions of the
     * input image have already been halved <code>reductionFactor</code> times
     * but the given region is relative to the full-sized image.
     *
     * @param inImage Image to crop
     * @param crop    Crop operation
     * @param rf      Number of times the dimensions of
     *                <code>inImage</code> have already been halved
     *                relative to the full-sized version
     * @return Cropped image, or the input image if the given operation is a
     *         no-op.
     */
    static RenderedOp cropImage(RenderedOp inImage,
                                Crop crop,
                                ReductionFactor rf) {
        RenderedOp croppedImage = inImage;
        if (!crop.isNoOp()) {
            // calculate the region x, y, and actual width/height
            final double scale = rf.getScale();
            final double regionX = crop.getX() * scale;
            final double regionY = crop.getY() * scale;
            final double regionWidth = crop.getWidth() * scale;
            final double regionHeight = crop.getHeight() * scale;

            float x, y, requestedWidth, requestedHeight, croppedWidth,
                    croppedHeight;
            if (crop.getShape().equals(Crop.Shape.SQUARE)) {
                final int shortestSide =
                        Math.min(inImage.getWidth(), inImage.getHeight());
                x = (inImage.getWidth() - shortestSide) / 2;
                y = (inImage.getHeight() - shortestSide) / 2;
                requestedWidth = requestedHeight = shortestSide;
            } else if (crop.getUnit().equals(Crop.Unit.PERCENT)) {
                x = (int) Math.round(regionX * inImage.getWidth());
                y = (int) Math.round(regionY * inImage.getHeight());
                requestedWidth = (int) Math.round(regionWidth *
                        inImage.getWidth());
                requestedHeight = (int) Math.round(regionHeight *
                        inImage.getHeight());
            } else {
                x = (int) Math.round(regionX);
                y = (int) Math.round(regionY);
                requestedWidth = (int) Math.round(regionWidth);
                requestedHeight = (int) Math.round(regionHeight);
            }
            // prevent width/height from exceeding the image bounds
            croppedWidth = (x + requestedWidth > inImage.getWidth()) ?
                    inImage.getWidth() - x : requestedWidth;
            croppedHeight = (y + requestedHeight > inImage.getHeight()) ?
                    inImage.getHeight() - y : requestedHeight;

            logger.debug("cropImage(): x: {}; y: {}; width: {}; height: {}",
                    x, y, croppedWidth, croppedHeight);

            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add(x);
            pb.add(y);
            pb.add(croppedWidth);
            pb.add(croppedHeight);
            croppedImage = JAI.create("crop", pb);
        }
        return croppedImage;
    }

    /**
     * @param inImage Image to get a RenderedOp of.
     * @return RenderedOp
     */
    static RenderedOp getAsRenderedOp(PlanarImage inImage) {
        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(inImage);
        return JAI.create("null", pb);
    }

    /**
     * Normalizes the component size to 8 bits per pixel.
     *
     * @param inImage
     * @return
     */
    static RenderedOp rescalePixels(RenderedOp inImage) {
        final int componentSize = inImage.getColorModel().getComponentSize(0);
        if (componentSize != 8) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);

            final double multiplier = Math.pow(2, 8) / Math.pow(2, componentSize);
            // Per-band constants to multiply by.
            final double[] constants = {multiplier};
            pb.add(constants);

            // Per-band offsets to be added.
            final double[] offsets = {0};
            pb.add(offsets);

            logger.debug("rescalePixels(): multiplying by {}", multiplier);
            inImage = JAI.create("rescale", pb);
        }
        return inImage;
    }

    /**
     * @param baseImage    Base image over which to draw the overlay.
     * @param overlayImage Image to overlay on top of the base image.
     * @param position     Position in which to render the overlay image.
     * @param inset        Minimum distance between the edges of the overlay
     *                     image and the edge of the base image, in pixels.
     * @return The base image with the overlay image overlaid on top of it.
     */
    private static RenderedOp overlayImage(RenderedOp baseImage,
                                           RenderedImage overlayImage,
                                           final Position position,
                                           final int inset) {
        if (overlayImage != null && position != null) {
            float overlayX, overlayY;
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
                            overlayImage.getWidth()) / 2f;
                    overlayY = inset;
                    break;
                case BOTTOM_CENTER:
                    overlayX = (baseImage.getWidth() -
                            overlayImage.getWidth()) / 2f;
                    overlayY = baseImage.getHeight() -
                            overlayImage.getHeight() - inset;
                    break;
                case LEFT_CENTER:
                    overlayX = inset;
                    overlayY = (baseImage.getHeight() -
                            overlayImage.getHeight()) / 2f;
                    break;
                case RIGHT_CENTER:
                    overlayX = baseImage.getWidth() -
                            overlayImage.getWidth() - inset;
                    overlayY = (baseImage.getHeight() -
                            overlayImage.getHeight()) / 2f;
                    break;
                case CENTER:
                    overlayX = (baseImage.getWidth() -
                            overlayImage.getWidth()) / 2f;
                    overlayY = (baseImage.getHeight() -
                            overlayImage.getHeight()) / 2f;
                    break;
                default: // bottom right
                    overlayX = baseImage.getWidth() -
                            overlayImage.getWidth() - inset;
                    overlayY = baseImage.getHeight() -
                            overlayImage.getHeight() - inset;
                    break;
            }
            // Move the overlay into the correct position.
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(overlayImage);
            pb.add(overlayX);
            pb.add(overlayY);
            pb.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
            overlayImage = JAI.create("translate", pb);

            // The overlay image may have a different number of bands than the
            // base image. The JAI "overlay" operation requires both images to
            // have the same number of bands.
            if (overlayImage.getSampleModel().getNumBands() ==
                    baseImage.getSampleModel().getNumBands()) {
                pb = new ParameterBlock();
                pb.addSource(baseImage);
                pb.addSource(overlayImage);
                baseImage = JAI.create("overlay", pb);
            } else {
                // The base image and overlay image have a different number of
                // bands. We will use the mosaic operator to combine them.

                // First, get the RGB bands of the base image.
                pb = new ParameterBlock();
                pb.addSource(baseImage);
                pb.add(new int[] {0, 1, 2});
                final RenderedOp rgbBaseImage = JAI.create("bandselect", pb);

                // Create a constant 1-band byte image to represent the base
                // image's alpha channel. It has the same dimensions and is
                // filled with 255 to indicate that the entire source is
                // opaque.
                pb = new ParameterBlock();
                pb.add((float) rgbBaseImage.getWidth());
                pb.add((float) rgbBaseImage.getHeight());
                pb.add(new Byte[] { new Byte((byte) 0xFF) });
                final RenderedOp baseAlpha = JAI.create("constant", pb);

                // Merge the RGB and alpha images together into an RGBA image.
                pb = new ParameterBlock();
                pb.addSource(baseImage);
                pb.addSource(baseAlpha);
                final RenderedOp rgbaBaseImage = JAI.create("bandmerge", pb);

                // Use the mosaic operation to blend the overlay into the new
                // RGBA base image.
                pb = new ParameterBlock();
                pb.addSource(rgbaBaseImage);
                pb.addSource(overlayImage);
                pb.add(MosaicDescriptor.MOSAIC_TYPE_BLEND);
                pb.add(null);                   // sourceAlpha
                pb.add(null);                   // sourceROI
                pb.add(new double[][] {{1.0}}); // sourceThreshold
                pb.add(new double[] {0.0});     // backgroundValues
                baseImage = JAI.create("mosaic", pb);
            }
        }
        return baseImage;
    }

    /**
     * @param inImage Image to rotate
     * @param rotate  Rotate operation
     * @return Rotated image, or the input image if the given rotate operation
     *         is a no-op.
     */
    static RenderedOp rotateImage(RenderedOp inImage, Rotate rotate) {
        RenderedOp rotatedImage = inImage;
        if (!rotate.isNoOp()) {
            logger.debug("rotateImage(): rotating {} degrees",
                    rotate.getDegrees());

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(rotatedImage);
            pb.add(inImage.getWidth() / 2.0f);                   // x origin
            pb.add(inImage.getHeight() / 2.0f);                  // y origin
            pb.add((float) Math.toRadians(rotate.getDegrees())); // radians
            pb.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
            rotatedImage = JAI.create("rotate", pb);
        }
        return rotatedImage;
    }

    /**
     * Scales an image using JAI, taking an already-applied reduction factor
     * into account. (In other words, the dimensions of the input image have
     * already been halved <code>reductionFactor</code> times but the given
     * size is relative to the full-sized image.)
     *
     * @param inImage       Image to scale.
     * @param scale         Requested size ignoring any reduction factor.
     * @param interpolation Interpolation.
     * @param rf            Reduction factor that has already been applied to
     *                      <code>inImage</code>.
     * @return Scaled image, or the input image if the given scale is a no-op.
     */
    static RenderedOp scaleImage(RenderedOp inImage,
                                 Scale scale,
                                 Interpolation interpolation,
                                 ReductionFactor rf) {
        RenderedOp scaledImage = inImage;
        if (!scale.isNoOp()) {
            final int sourceWidth = inImage.getWidth();
            final int sourceHeight = inImage.getHeight();
            final Dimension scaledSize = scale.getResultingSize(
                    new Dimension(sourceWidth, sourceHeight));

            double xScale = scaledSize.width / (double) sourceWidth;
            double yScale = scaledSize.height / (double) sourceHeight;
            if (scale.getPercent() != null) {
                xScale = scale.getPercent() / rf.getScale();
                yScale = scale.getPercent() / rf.getScale();
            }

            logger.debug("scaleImage(): width: {}%; height: {}%",
                    xScale * 100, yScale * 100);
            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add((float) xScale);
            pb.add((float) yScale);
            pb.add(0.0f);
            pb.add(0.0f);
            pb.add(interpolation);
            scaledImage = JAI.create("scale", pb);
        }
        return scaledImage;
    }

    /**
     * <p>Better-quality alternative to {@link #scaleImage(RenderedOp, Scale,
     * Interpolation, ReductionFactor)} using JAI's
     * <code>SubsampleAverage</code> operator.</p>
     *
     * <p>N.B. The <code>SubsampleAverage</code> operator is not capable of
     * upscaling. If asked to upscale, this method will use the inferior-quality
     * <code>Scale</code> operator instead.</p>
     *
     * @param inImage       Image to scale. Must be at least 3 pixels on the
     *                      smallest side.
     * @param scale         Requested size ignoring any reduction factor.
     * @param rf            Reduction factor that has already been applied to
     *                      <code>inImage</code>.
     * @return Scaled image, or the input image if the given scale is a no-op.
     */
    static RenderedOp scaleImageUsingSubsampleAverage(RenderedOp inImage,
                                                      Scale scale,
                                                      ReductionFactor rf) {
        RenderedOp scaledImage = inImage;
        final int sourceWidth = inImage.getWidth();
        final int sourceHeight = inImage.getHeight();
        final Dimension fullSize = new Dimension(sourceWidth, sourceHeight);

        if (scale.isUp(fullSize)) {
            logger.debug("scaleImageUsingSubsampleAverage(): can't upscale; " +
                    "invoking scaleImage() instead");
            return scaleImage(inImage, scale,
                    Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                    rf);
        } else if (!scale.isNoOp()) {
            final Dimension scaledSize = scale.getResultingSize(
                    new Dimension(sourceWidth, sourceHeight));

            double xScale = scaledSize.width / (double) sourceWidth;
            double yScale = scaledSize.height / (double) sourceHeight;
            if (scale.getPercent() != null) {
                xScale = scale.getPercent() / rf.getScale();
                yScale = scale.getPercent() / rf.getScale();
            }

            logger.debug("scaleImageUsingSubsampleAverage(): " +
                            "width: {}%; height: {}%",
                    xScale * 100, yScale * 100);
            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add(xScale);
            pb.add(yScale);
            pb.add(0.0); // X translation
            pb.add(0.0); // Y translation

            final RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            scaledImage = JAI.create("SubsampleAverage", pb, hints);
        }
        return scaledImage;
    }

    /**
     * @param inImage Image to sharpen.
     * @param sharpen The sharpen operation.
     * @return Sharpened image.
     */
    static RenderedOp sharpenImage(final RenderedOp inImage,
                                   final Sharpen sharpen) {
        RenderedOp sharpenedImage = inImage;
        if (!sharpen.isNoOp()) {
            // http://docs.oracle.com/cd/E17802_01/products/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/UnsharpMaskDescriptor.html
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add(null);
            pb.add(sharpen.getAmount());
            sharpenedImage = JAI.create("UnsharpMask", pb);
        }
        return sharpenedImage;
    }

    /**
     * @param inImage Image to filter
     * @param color   Color transform operation
     * @return Transformed image, or the input image if the given operation
     *         is a no-op.
     */
    @SuppressWarnings({"deprecation"}) // really, JAI itself is basically deprecated
    static RenderedOp transformColor(RenderedOp inImage, Color color) {
        RenderedOp filteredImage = inImage;
        if (!color.isNoOp()) {
            // convert to grayscale
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            final int numBands = OpImage.getExpandedNumBands(
                    inImage.getSampleModel(), inImage.getColorModel());
            double[][] matrix = new double[1][numBands + 1];
            matrix[0][0] = 0.114;
            matrix[0][1] = 0.587;
            matrix[0][2] = 0.299;
            for (int i = 3; i <= numBands; i++) {
                matrix[0][i] = 0;
            }
            pb.add(matrix);
            filteredImage = JAI.create("bandcombine", pb, null);
            if (color == Color.BITONAL) {
                pb = new ParameterBlock();
                pb.addSource(filteredImage);
                pb.add(1.0 * 128);
                filteredImage = JAI.create("binarize", pb);
            }
        }
        return filteredImage;
    }

    /**
     * @param inImage   Image to transpose.
     * @param transpose The transpose operation.
     * @return Transposed image, or the input image if the given transpose
     *         operation is a no-op.
     */
    static RenderedOp transposeImage(RenderedOp inImage, Transpose transpose) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(inImage);
        switch (transpose) {
            case HORIZONTAL:
                logger.debug("transposeImage(): horizontal");
                pb.add(TransposeDescriptor.FLIP_HORIZONTAL);
                break;
            case VERTICAL:
                logger.debug("transposeImage(): vertical");
                pb.add(TransposeDescriptor.FLIP_VERTICAL);
                break;
        }
        return JAI.create("transpose", pb);
    }

}
