package edu.illinois.library.cantaloupe.processor;

import com.mortennobel.imagescaling.ResampleFilter;
import com.mortennobel.imagescaling.ResampleOp;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.overlay.ImageOverlay;
import edu.illinois.library.cantaloupe.operation.overlay.Position;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.overlay.StringOverlay;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.overlay.OverlayService;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.resolver.InputStreamStreamSource;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A collection of methods for operating on {@link BufferedImage}s.
 */
public abstract class Java2dUtil {

    private static Logger logger = LoggerFactory.getLogger(Java2dUtil.class);

    /**
     * See the inline documentation in scaleImage() for a rationale for
     * choosing this.
     */
    private static final Scale.Filter DEFAULT_DOWNSCALE_FILTER =
            Scale.Filter.BOX;

    /**
     * See the inline documentation in scaleImage() for a rationale for
     * choosing this.
     */
    private static final Scale.Filter DEFAULT_UPSCALE_FILTER =
            Scale.Filter.BICUBIC;

    /**
     * Redacts regions from the given image.
     *
     * @param baseImage Image to apply the overlay on top of.
     * @param appliedCrop Crop already applied to <code>baseImage</code>.
     * @param reductionFactor Reduction factor already applied to
     *                        <code>baseImage</code>.
     * @param redactions Regions of the image to redact.
     * @return Input image with redactions applied.
     */
    static BufferedImage applyRedactions(final BufferedImage baseImage,
                                         final Crop appliedCrop,
                                         final ReductionFactor reductionFactor,
                                         final List<Redaction> redactions) {
        if (baseImage != null && redactions.size() > 0) {
            final Stopwatch watch = new Stopwatch();
            final Dimension imageSize = new Dimension(
                    baseImage.getWidth(), baseImage.getHeight());

            final Graphics2D g2d = baseImage.createGraphics();
            g2d.setColor(java.awt.Color.BLACK);

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
            logger.debug("applyRedactions() executed in {} msec",
                    watch.timeElapsed());
        }
        return baseImage;
    }

    /**
     * Applies the given overlay to the given image. The overlay may be a
     * string ({@link StringOverlay}) or an image ({@link ImageOverlay}).
     *
     * @param baseImage Image to apply the overlay on top of.
     * @param overlay Overlay to apply to the base image.
     * @return Overlaid image, or the input image if the overlay should not be
     *         applied according to the application configuration.
     * @throws ConfigurationException
     * @throws IOException
     */
    static BufferedImage applyOverlay(final BufferedImage baseImage,
                                      final Overlay overlay)
            throws ConfigurationException, IOException {
        BufferedImage markedImage = baseImage;
        final Dimension imageSize = new Dimension(baseImage.getWidth(),
                baseImage.getHeight());
        if (new OverlayService().shouldApplyToImage(imageSize)) {
            if (overlay instanceof ImageOverlay) {
                markedImage = overlayImage(baseImage,
                        getOverlayImage((ImageOverlay) overlay),
                        overlay.getPosition(),
                        overlay.getInset());
            } else if (overlay instanceof StringOverlay) {
                final StringOverlay sw = (StringOverlay) overlay;
                markedImage = overlayString(baseImage,
                        sw.getString(),
                        sw.getFont(),
                        sw.getColor(),
                        sw.getStrokeColor(),
                        sw.getStrokeWidth(),
                        overlay.getPosition(),
                        overlay.getInset());
            }
        }
        return markedImage;
    }

    /**
     * @param inImage Image to crop.
     * @param crop Crop operation. Clients should call
     *             {@link Operation#hasEffect(Dimension, OperationList)}
     *             before invoking.
     * @return Cropped image, or the input image if the given operation is a
     *         no-op.
     */
    static BufferedImage cropImage(final BufferedImage inImage,
                                   final Crop crop) {
        return cropImage(inImage, crop, new ReductionFactor());
    }

    /**
     * Crops the given image taking into account a reduction factor
     * (<code>reductionFactor</code>). In other words, the dimensions of the
     * input image have already been halved <code>reductionFactor</code> times
     * but the given region is relative to the full-sized image.
     *
     * @param inImage Image to crop.
     * @param crop Crop operation. Clients should call
     *             {@link Operation#hasEffect(Dimension, OperationList)} before
     *             invoking.
     * @param rf Number of times the dimensions of <code>inImage</code> have
     *           already been halved relative to the full-sized version.
     * @return Cropped image, or the input image if the given operation is a
     *         no-op.
     */
    static BufferedImage cropImage(final BufferedImage inImage,
                                   final Crop crop,
                                   final ReductionFactor rf) {
        final Dimension croppedSize = crop.getResultingSize(
                new Dimension(inImage.getWidth(), inImage.getHeight()));
        BufferedImage croppedImage;
        if (!crop.hasEffect() || (croppedSize.width == inImage.getWidth() &&
                croppedSize.height == inImage.getHeight())) {
            croppedImage = inImage;
        } else {
            final Stopwatch watch = new Stopwatch();
            final double scale = rf.getScale();
            final double regionX = crop.getX() * scale;
            final double regionY = crop.getY() * scale;
            final double regionWidth = crop.getWidth() * scale;
            final double regionHeight = crop.getHeight() * scale;

            int x, y, requestedWidth, requestedHeight, croppedWidth,
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

            logger.debug("cropImage(): cropped {}x{} image to {} in {} msec",
                    inImage.getWidth(), inImage.getHeight(), crop,
                    watch.timeElapsed());
        }
        return croppedImage;
    }

    /**
     * @param overlay
     * @return Overlay image.
     * @throws IOException
     */
    static BufferedImage getOverlayImage(ImageOverlay overlay)
            throws IOException {
        try (InputStream is = overlay.openStream()) {
            InputStreamStreamSource isss = new InputStreamStreamSource(is);
            final ImageReader reader = new ImageReader(isss, Format.PNG);
            return reader.read();
        }
    }

    /**
     * @param baseImage Image to overlay the image onto.
     * @param overlayImage Image to overlay.
     * @param position Position of the overlaid image.
     * @param inset Inset in pixels.
     * @return
     */
    private static BufferedImage overlayImage(final BufferedImage baseImage,
                                              final BufferedImage overlayImage,
                                              final Position position,
                                              final int inset) {
        if (overlayImage != null) {
            final Stopwatch watch = new Stopwatch();
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
            logger.debug("overlayImage() executed in {} msec",
                    watch.timeElapsed());
        }
        return baseImage;
    }

    /**
     * <p>Overlays a string on top of an image, for e.g. a text overlay.</p>
     *
     * <p>The overlay string will only be drawn if it will fit entirely within
     * <var>baseImage</var>.</p>
     *
     * @param baseImage Image to overlay the string onto.
     * @param overlayString String to overlay onto the image.
     * @param fillColor Color of the text.
     * @param strokeColor Color of the text outline.
     * @param strokeWidth Width in pixels of the stroke.
     * @param position Position of the overlaid string.
     * @param inset Inset of the overlaid string in pixels.
     * @return
     */
    private static BufferedImage overlayString(final BufferedImage baseImage,
                                               final String overlayString,
                                               final Font font,
                                               final java.awt.Color fillColor,
                                               final java.awt.Color strokeColor,
                                               final float strokeWidth,
                                               final Position position,
                                               final int inset) {
        if (overlayString != null && overlayString.length() > 0) {
            final Stopwatch watch = new Stopwatch();

            final Graphics2D g2d = baseImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(baseImage, 0, 0, null);
            g2d.setFont(font);

            // Graphics2D.drawString() does not understand newlines, so each
            // line has to be drawn separately.
            final String[] lines = StringUtils.split(overlayString, "\n");
            final FontMetrics fm = g2d.getFontMetrics();
            // The total height is the sum of the height of all lines.
            final int lineHeight = fm.getHeight();
            final int totalHeight = lineHeight * lines.length;
            int maxWidth = 0;
            final int[] lineWidths = new int[lines.length];
            for (int i = 0; i < lines.length; i++) {
                final int lineWidth = fm.stringWidth(lines[i]);
                lineWidths[i] = lineWidth;
                if (lineWidth > maxWidth) {
                    maxWidth = lineWidth;
                }
            }

            // Only draw the text if it will fit completely within the image in
            // both dimensions.
            if (maxWidth + inset <= baseImage.getWidth() &&
                    totalHeight + inset <= baseImage.getHeight()) {
                for (int i = 0; i < lines.length; i++) {
                    int x, y;
                    switch (position) {
                        case TOP_LEFT:
                            x = inset;
                            y = inset + lineHeight * i;
                            break;
                        case TOP_RIGHT:
                            x = baseImage.getWidth() - lineWidths[i] - inset;
                            y = inset + lineHeight * i;
                            break;
                        case BOTTOM_LEFT:
                            x = inset;
                            y = baseImage.getHeight() - totalHeight - inset +
                                    lineHeight * i;
                            break;
                        // case BOTTOM_RIGHT: will be handled in default:
                        case TOP_CENTER:
                            x = (baseImage.getWidth() - lineWidths[i]) / 2;
                            y = inset + lineHeight * i;
                            break;
                        case BOTTOM_CENTER:
                            x = (baseImage.getWidth() - lineWidths[i]) / 2;
                            y = baseImage.getHeight() - totalHeight - inset +
                                    lineHeight * i;
                            break;
                        case LEFT_CENTER:
                            x = inset;
                            y = (baseImage.getHeight() - totalHeight) / 2 +
                                    lineHeight * i;
                            break;
                        case RIGHT_CENTER:
                            x = baseImage.getWidth() - lineWidths[i] - inset;
                            y = (baseImage.getHeight() - totalHeight) / 2 +
                                    lineHeight * i;
                            break;
                        case CENTER:
                            x = (baseImage.getWidth() - lineWidths[i]) / 2;
                            y = (baseImage.getHeight() - totalHeight) / 2;
                            break;
                        default: // bottom right
                            x = baseImage.getWidth() - lineWidths[i] - inset;
                            y = baseImage.getHeight() - totalHeight - inset +
                                    lineHeight * i;
                            break;
                    }
                    y += lineHeight * 0.8; // TODO: this is arbitrary fudge

                    // Draw the text outline.
                    if (strokeWidth > 0.001f) {
                        final FontRenderContext frc = g2d.getFontRenderContext();
                        final GlyphVector gv = font.createGlyphVector(frc, lines[i]);
                        final Shape shape = gv.getOutline(x, y);
                        g2d.setStroke(new BasicStroke(strokeWidth));
                        g2d.setPaint(strokeColor);
                        g2d.draw(shape);
                    }

                    // Draw the text.
                    g2d.setPaint(fillColor);
                    g2d.drawString(lines[i], x, y);
                }
                logger.debug("overlayString() executed in {} msec",
                        watch.timeElapsed());
            } else {
                logger.debug("overlayString(): {}x{} text won't fit in {}x{} image",
                        maxWidth + inset, totalHeight + inset,
                        baseImage.getWidth(), baseImage.getHeight());
            }
            g2d.dispose();
        }
        return baseImage;
    }

    /**
     * Reduces an image's component size to 8 bits if greater.
     *
     * @param inImage Image to reduce
     * @return Reduced image, or the input image if it already is 8 bits or
     *         less.
     */
    static BufferedImage reduceTo8Bits(final BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage.getColorModel().getComponentSize(0) > 8) {
            final Stopwatch watch = new Stopwatch();
            final int type = inImage.getColorModel().hasAlpha() ?
                    BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            outImage = new BufferedImage(inImage.getWidth(),
                    inImage.getHeight(), type);
            final ColorConvertOp op = new ColorConvertOp(
                    inImage.getColorModel().getColorSpace(),
                    outImage.getColorModel().getColorSpace(), null);
            outImage.createGraphics().drawImage(inImage, op, 0, 0);
            logger.debug("reduceTo8Bits(): converted in {} msec",
                    watch.timeElapsed());
        }
        return outImage;
    }

    public static BufferedImage removeAlpha(final BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage.getColorModel().hasAlpha()) {
            final Stopwatch watch = new Stopwatch();
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
            logger.debug("removeAlpha(): converted BufferedImage type {} to " +
                    "RGB in {} msec", inImage.getType(), watch.timeElapsed());
        }
        return outImage;
    }

    /**
     * @param inImage Image to rotate
     * @param rotate Rotate operation
     * @return Rotated image, or the input image if the given rotation is a
     * no-op.
     */
    static BufferedImage rotateImage(final BufferedImage inImage,
                                     final Rotate rotate) {
        BufferedImage rotatedImage = inImage;
        if (rotate.hasEffect()) {
            final Stopwatch watch = new Stopwatch();
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
            logger.debug("rotateImage() executed in {} msec",
                    watch.timeElapsed());
        }
        return rotatedImage;
    }

    /**
     * Scales an image using Graphics2D.
     *
     * @param inImage Image to scale.
     * @param scale   Scale operation. Clients should call
     *                {@link Operation#hasEffect(Dimension, OperationList)}
     *                before invoking.
     * @return Downscaled image, or the input image if the given scale is a
     *         no-op.
     */
    static BufferedImage scaleImage(final BufferedImage inImage,
                                    final Scale scale) {
        return scaleImage(inImage, scale, new ReductionFactor(0));
    }

    /**
     * Scales an image, taking an already-applied reduction factor into
     * account. In other words, the dimensions of the input image have already
     * been halved <code>rf</code> times but the given size is relative to the
     * full-sized image.
     *
     * @param inImage Image to scale.
     * @param scale Requested size ignoring any reduction factor. If no
     *              resample filter is set, a reasonable default will be used.
     *              Clients should call
     *              {@link Operation#hasEffect(Dimension, OperationList)}
     *              before invoking.
     * @param rf Reduction factor that has already been applied to
     *           <code>inImage</code>.
     * @return Downscaled image, or the input image if the given scale is a
     *         no-op.
     */
    static BufferedImage scaleImage(final BufferedImage inImage,
                                    final Scale scale,
                                    final ReductionFactor rf) {
        /*
        This method uses the image scaling code in
        com.mortennobel.imagescaling (see
        https://blog.nobel-joergensen.com/2008/12/20/downscaling-images-in-java/)
        as an alternative to the scaling available in Graphics2D.
        Problem is, while the performance of Graphics2D.drawImage() is OK, the
        quality, even using RenderingHints.VALUE_INTERPOLATION_BILINEAR, is
        horrible for downscaling. BufferedImage.getScaledInstance() is
        somewhat the opposite: great quality but very slow. There may be ways
        to mitigate the former (like multi-step downscaling) but not without
        cost.

        Subjective quality of downscale from 2288x1520 to 200x200:

        Lanczos3 > Box > Bicubic > Mitchell > Triangle > Bell > Hermite >
            BSpline > Graphics2D

        Approximate time-to-complete of same (milliseconds, 2.3GHz i7):
        Triangle: 19
        Box: 20
        Hermite: 24
        Bicubic: 25
        Mitchell: 30
        BSpline: 53
        Graphics2D: 70
        Bell: 145
        Lanczos3: 238

        Subjective quality of upscale from 2288x1520 to 3000x3000:
        Lanczos3 > Bicubic > Mitchell > Graphics2D = Triangle = Hermite >
            Bell > BSpline > Box

        Approximate time-to-complete of same (milliseconds, 2.3GHz i7):
        Triangle: 123
        Hermite: 142
        Box: 162
        Bicubic: 206
        Mitchell: 224
        BSpline: 230
        Graphics2D: 268
        Lanczos3: 355
        Bell: 468
        */

        final Dimension sourceSize = new Dimension(
                inImage.getWidth(), inImage.getHeight());

        // Calculate the size that the image will need to be scaled to based
        // on the source image size, scale, and already-applied reduction
        // factor.
        Dimension targetSize;
        if (scale.getPercent() != null) {
            targetSize = new Dimension();
            targetSize.width = (int) Math.round(sourceSize.width *
                    (scale.getPercent() / rf.getScale()));
            targetSize.height = (int) Math.round(sourceSize.height *
                    (scale.getPercent() / rf.getScale()));
        } else {
            targetSize = scale.getResultingSize(sourceSize);
        }

        // com.mortennobel.imagescaling.ResampleFilter requires a target size
        // of at least 3 pixels on a side.
        // OpenSeadragon has been known to request smaller.
        targetSize.width = (targetSize.width < 3) ? 3 : targetSize.width;
        targetSize.height = (targetSize.height < 3) ? 3 : targetSize.height;

        BufferedImage scaledImage = inImage;
        if (scale.hasEffect() && (targetSize.width != sourceSize.width &&
                targetSize.height != sourceSize.height)) {
            final Stopwatch watch = new Stopwatch();

            final ResampleOp resampleOp = new ResampleOp(
                    targetSize.width, targetSize.height);

            // Try to use the requested resample filter.
            ResampleFilter filter = null;
            if (scale.getFilter() != null) {
                filter = scale.getFilter().getResampleFilter();
            }
            // No particular filter requested, so select a default.
            if (filter == null) {
                if (targetSize.width < sourceSize.width ||
                        targetSize.height < sourceSize.height) {
                    filter = DEFAULT_DOWNSCALE_FILTER.getResampleFilter();
                } else {
                    filter = DEFAULT_UPSCALE_FILTER.getResampleFilter();
                }
            }
            resampleOp.setFilter(filter);

            scaledImage = resampleOp.filter(inImage, null);

            logger.debug("scaleImage(): scaled {}x{} image to {}x{} using " +
                    "the {} filter in {} msec",
                    sourceSize.width, sourceSize.height,
                    targetSize.width, targetSize.height,
                    filter.getName(), watch.timeElapsed());
        }
        return scaledImage;
    }

    /**
     * @param inImage Image to sharpen.
     * @param sharpen The sharpen operation.
     * @return Sharpened image.
     */
    static BufferedImage sharpenImage(final BufferedImage inImage,
                                      final Sharpen sharpen) {
        BufferedImage sharpenedImage = inImage;
        if (sharpen.hasEffect()) {
            if (inImage.getWidth() > 2 && inImage.getHeight() > 2) {
                final Stopwatch watch = new Stopwatch();

                final ResampleOp resampleOp = new ResampleOp(
                        inImage.getWidth(), inImage.getHeight());
                resampleOp.setUnsharpenMask(sharpen.getAmount());
                sharpenedImage = resampleOp.filter(inImage, null);

                logger.debug("sharpenImage(): sharpened by {} in {} msec",
                        sharpen.getAmount(), watch.timeElapsed());
            } else {
                logger.debug("sharpenImage(): image must be at least 3 " +
                        "pixels on a side; skipping");
            }
        }
        return sharpenedImage;
    }

    /**
     * <p>Linearly stretches the contrast of an image to occupy the full range
     * of intensities. Histogram gaps will result.</p>
     *
     * <p>Does not work with indexed images.</p>
     *
     * @param inImage Image to stretch.
     * @return Stretched image.
     */
    static BufferedImage stretchContrast(BufferedImage inImage) {
        if (inImage.getType() != BufferedImage.TYPE_BYTE_INDEXED) {
            // Stretch only if there is at least this difference between
            // minimum and maximum luminance.
            final float threshold = 0.01f;
            final float maxColor =
                    (float) Math.pow(2, inImage.getColorModel().getComponentSize(0));

            final Stopwatch watch = new Stopwatch();
            final int minX = inImage.getMinX();
            final int minY = inImage.getMinY();
            final int width = inImage.getWidth();
            final int height = inImage.getHeight();
            float lowRgb = maxColor, highRgb = 0;

            // Scan every pixel to find the darkest and brightest.
            for (int x = minX; x < minX + width; x++) {
                for (int y = minY; y < minY + height; y++) {
                    final int color = inImage.getRGB(x, y);
                    final int red = (color >>> 16) & 0xFF;
                    final int green = (color >>> 8) & 0xFF;
                    final int blue = color & 0xFF;
                    if (red < lowRgb) {
                        lowRgb = red;
                    }
                    if (green < lowRgb) {
                        lowRgb = green;
                    }
                    if (blue < lowRgb) {
                        lowRgb = blue;
                    }
                    if (red > highRgb) {
                        highRgb = red;
                    }
                    if (green > highRgb) {
                        highRgb = green;
                    }
                    if (blue > highRgb) {
                        highRgb = blue;
                    }
                }
            }

            if (Math.abs(highRgb - lowRgb) > threshold) {
                for (int x = minX; x < minX + width; x++) {
                    for (int y = minY; y < minY + height; y++) {
                        final int color = inImage.getRGB(x, y);
                        final int red = (color >>> 16) & 0xFF;
                        final int green = (color >>> 8) & 0xFF;
                        final int blue = color & 0xFF;

                        float stretchedRed =
                                Math.abs((red - lowRgb) / (highRgb - lowRgb));
                        if (stretchedRed > 1) {
                            stretchedRed = 1;
                        }
                        float stretchedGreen =
                                Math.abs((green - lowRgb) / (highRgb - lowRgb));
                        if (stretchedGreen > 1) {
                            stretchedGreen = 1;
                        }
                        float stretchedBlue =
                                Math.abs((blue - lowRgb) / (highRgb - lowRgb));
                        if (stretchedBlue > 1) {
                            stretchedBlue = 1;
                        }
                        final java.awt.Color outColor = new java.awt.Color(
                                stretchedRed, stretchedGreen, stretchedBlue);
                        inImage.setRGB(x, y, outColor.getRGB());
                    }
                }
                logger.debug("stretchContrast(): rescaled in {} msec ",
                        watch.timeElapsed());
            } else {
                logger.debug("stretchContrast(): not enough contrast to stretch.");
            }
        } else {
            logger.debug("stretchContrast(): can't stretch an indexed image.");
        }
        return inImage;
    }

    /**
     * @param inImage Image to filter
     * @param color   Color operation
     * @return Filtered image, or the input image if the given color operation
     *         is a no-op.
     */
    static BufferedImage transformColor(final BufferedImage inImage,
                                        final Color color) {
        BufferedImage filteredImage = inImage;
        final Stopwatch watch = new Stopwatch();
        switch (color) {
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

            logger.debug("transformColor(): filtered {}x{} image in {} msec",
                    inImage.getWidth(), inImage.getHeight(),
                    watch.timeElapsed());
        }
        return filteredImage;
    }

    /**
     * @param inImage Image to transpose.
     * @param transpose The transpose operation.
     * @return Transposed image.
     */
    static BufferedImage transposeImage(final BufferedImage inImage,
                                        final Transpose transpose) {
        final Stopwatch watch = new Stopwatch();
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

        logger.debug("transposeImage(): transposed image in {} msec",
                watch.timeElapsed());
        return outImage;
    }

}
