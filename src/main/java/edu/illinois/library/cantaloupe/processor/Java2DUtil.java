package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.overlay.ImageOverlay;
import edu.illinois.library.cantaloupe.operation.overlay.Position;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.overlay.StringOverlay;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.processor.resample.ResampleFilter;
import edu.illinois.library.cantaloupe.processor.resample.ResampleOp;
import edu.illinois.library.cantaloupe.util.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
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
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * <p>Collection of methods for operating on {@link BufferedImage}s.</p>
 *
 * <h1>{@link BufferedImage} Cliff's Notes</h1>
 *
 * <p>{@link BufferedImage}s can have a variety of different internal data
 * layouts that are partially dictated by their {@link BufferedImage#getType()
 * type}, which corresponds to <em>display hardware</em>, not necessarily
 * source image data. All of the standard types are limited to 8 bits per
 * sample; larger sample sizes require {@link BufferedImage#TYPE_CUSTOM}.
 * Unfortunately, whereas operations on many of the standard types (by e.g.
 * {@link Graphics2D}) tend to be well-optimized, that doesn't carry over to
 * {@link BufferedImage#TYPE_CUSTOM} and so most operations on images of this
 * type are relatively slow.</p>
 *
 * <h1>{@link BufferedImage} Type Cheat Sheet</h1>
 *
 * <dl>
 *     <dt>Color (non-indexed)</dt>
 *     <dd>
 *         <dl>
 *             <dt>&le;8 bits</dt>
 *             <dd>
 *                 <dl>
 *                     <dt>Alpha</dt>
 *                     <dd>{@link BufferedImage#TYPE_4BYTE_ABGR},
 *                     {@link BufferedImage#TYPE_INT_ARGB}</dd>
 *                     <dt>No Alpha</dt>
 *                     <dd>{@link BufferedImage#TYPE_3BYTE_BGR},
 *                     {@link BufferedImage#TYPE_INT_RGB},
 *                     {@link BufferedImage#TYPE_INT_BGR}</dd>
 *                 </dl>
 *             </dd>
 *             <dt>&gt;8 bits</dt>
 *             <dd>{@link BufferedImage#TYPE_CUSTOM}</dd>
 *         </dl>
 *     </dd>
 *     <dt>Gray</dt>
 *     <dd>
 *         <dl>
 *             <dt>&le;8 bits</dt>
 *             <dd>
 *                 <dl>
 *                     <dt>Alpha</dt>
 *                     <dd>{@link BufferedImage#TYPE_CUSTOM}</dd>
 *                     <dt>No Alpha</dt>
 *                     <dd>{@link BufferedImage#TYPE_BYTE_GRAY}</dd>
 *                 </dl>
 *             </dd>
 *             <dt>&gt;8 bits</dt>
 *             <dd>
 *                 <dl>
 *                     <dt>Alpha</dt>
 *                     <dd>{@link BufferedImage#TYPE_CUSTOM}</dd>
 *                     <dt>No Alpha</dt>
 *                     <dd>{@link BufferedImage#TYPE_USHORT_GRAY}</dd>
 *                 </dl>
 *             </dd>
 *         </dl>
 *     </dd>
 *     <dt>Bitonal</dt>
 *     <dd>{@link BufferedImage#TYPE_BYTE_BINARY}</dd>
 *     <dt>Indexed</dt>
 *     <dd>{@link BufferedImage#TYPE_BYTE_INDEXED}</dd>
 * </dl>
 */
public final class Java2DUtil {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Java2DUtil.class);

    /**
     * See the inline documentation in {@link #scale(BufferedImage, Scale,
     * ReductionFactor)} for a rationale for choosing this.
     */
    private static final Scale.Filter DEFAULT_DOWNSCALE_FILTER =
            Scale.Filter.BOX;

    /**
     * See the inline documentation in {@link #scale(BufferedImage, Scale,
     * ReductionFactor)} for a rationale for choosing this.
     */
    private static final Scale.Filter DEFAULT_UPSCALE_FILTER =
            Scale.Filter.BICUBIC;

    /**
     * Redacts regions from the given image.
     *
     * @param image           Image to redact.
     * @param appliedCrop     Crop already applied to {@literal image}.
     * @param reductionFactor Reduction factor already applied to
     *                        {@literal image}.
     * @param redactions      Regions of the image to redact.
     */
    static void applyRedactions(final BufferedImage image,
                                final Crop appliedCrop,
                                final ReductionFactor reductionFactor,
                                final Set<Redaction> redactions) {
        if (image != null && !redactions.isEmpty()) {
            final Stopwatch watch = new Stopwatch();
            final Dimension imageSize = new Dimension(
                    image.getWidth(), image.getHeight());

            final Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setColor(java.awt.Color.BLACK);

            for (final Redaction redaction : redactions) {
                final Rectangle redactionRegion =
                        redaction.getResultingRegion(imageSize, appliedCrop);
                redactionRegion.x *= reductionFactor.getScale();
                redactionRegion.y *= reductionFactor.getScale();
                redactionRegion.width *= reductionFactor.getScale();
                redactionRegion.height *= reductionFactor.getScale();

                if (!redactionRegion.isEmpty()) {
                    LOGGER.debug("applyRedactions(): applying {} at {},{}/{}x{}",
                            redaction, redactionRegion.x, redactionRegion.y,
                            redactionRegion.width, redactionRegion.height);
                    g2d.fill(redactionRegion);
                } else {
                    LOGGER.debug("applyRedactions(): {} is outside crop area; skipping",
                            redaction);
                }
            }
            g2d.dispose();
            LOGGER.debug("applyRedactions() executed in {}", watch);
        }
    }

    /**
     * Applies the given overlay to the given image. The overlay may be a
     * {@link StringOverlay string} or an {@link ImageOverlay image}.
     *
     * @param baseImage Image to apply the overlay on top of.
     * @param overlay   Overlay to apply to the base image.
     */
    static void applyOverlay(final BufferedImage baseImage,
                             final Overlay overlay) throws IOException {
        if (overlay instanceof ImageOverlay) {
            overlayImage(baseImage,
                    getOverlayImage((ImageOverlay) overlay),
                    overlay.getPosition(),
                    overlay.getInset());
        } else if (overlay instanceof StringOverlay) {
            overlayString(baseImage, (StringOverlay) overlay);
        }
    }

    /**
     * @param inImage Image to convert.
     * @return New image of type {@link BufferedImage#TYPE_INT_ARGB}, or the
     *         input image if it is not indexed.
     */
    static BufferedImage convertIndexedTo8BitARGB(BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
            final Stopwatch watch = new Stopwatch();

            outImage = new BufferedImage(
                    inImage.getWidth(), inImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = outImage.createGraphics();
            g2d.drawImage(inImage, 0, 0, null);
            g2d.dispose();

            LOGGER.debug("convertIndexedTo8BitARGB(): converted in {}", watch);
        }
        return outImage;
    }

    /**
     * N.B.: This method should only be invoked if {@link
     * Crop#hasEffect(Dimension, OperationList)} returns {@literal true}.
     *
     * @param inImage Image to crop.
     * @param crop    Crop operation.
     * @return        Cropped image, or the input image if the given operation
     *                is a no-op.
     */
    static BufferedImage crop(final BufferedImage inImage,
                              final Crop crop) {
        return crop(inImage, crop, new ReductionFactor());
    }

    /**
     * Crops the given image taking into account a reduction factor
     * ({@literal reductionFactor}). In other words, the dimensions of the
     * input image have already been halved {@literal reductionFactor} times
     * but the given region is relative to the full-sized image.
     *
     * @param inImage Image to crop.
     * @param crop    Crop operation. Clients should call
     *                {@link Operation#hasEffect(Dimension, OperationList)}
     *                before invoking.
     * @param rf      Number of times the dimensions of {@literal inImage} have
     *                already been halved relative to the full-sized version.
     * @return        Cropped image, or the input image if the given operation
     *                is a no-op.
     */
    static BufferedImage crop(BufferedImage inImage,
                              final Crop crop,
                              final ReductionFactor rf) {
        BufferedImage outImage = inImage;

        final Dimension croppedSize = crop.getResultingSize(
                new Dimension(inImage.getWidth(), inImage.getHeight()));

        if (crop.hasEffect() && (croppedSize.width != inImage.getWidth() ||
                croppedSize.height != inImage.getHeight())) {
            final Stopwatch watch = new Stopwatch();

            final Rectangle cropRegion = crop.getRectangle(
                    new Dimension(outImage.getWidth(), outImage.getHeight()), rf);

            outImage = inImage.getSubimage(cropRegion.x, cropRegion.y,
                    cropRegion.width, cropRegion.height);

            outImage = convertIndexedTo8BitARGB(outImage);

            LOGGER.debug("crop(): cropped {}x{} image to {} in {}",
                    inImage.getWidth(), inImage.getHeight(), crop, watch);
        }
        return outImage;
    }

    /**
     * @param overlay
     * @return Overlay image.
     */
    static BufferedImage getOverlayImage(ImageOverlay overlay)
            throws IOException {
        ImageReader reader = null;
        try (InputStream is = overlay.openStream()) {
            reader = new ImageReaderFactory().newImageReader(is, Format.PNG);
            return reader.read();
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    /**
     * @param baseImage    Image to overlay the image onto.
     * @param overlayImage Image to overlay.
     * @param position     Position of the overlaid image.
     * @param inset        Inset in pixels.
     */
    private static void overlayImage(final BufferedImage baseImage,
                                     final BufferedImage overlayImage,
                                     final Position position,
                                     final int inset) {
        if (overlayImage != null) {
            final Stopwatch watch = new Stopwatch();

            final Graphics2D g2d = baseImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(baseImage, 0, 0, null);

            if (Position.REPEAT.equals(position)) {
                int startX = Math.round(baseImage.getWidth() / 2f);
                int startY = Math.round(baseImage.getHeight() / 2f);
                while (startX >= 0) {
                    startX -= overlayImage.getWidth();
                }
                while (startY >= 0) {
                    startY -= overlayImage.getHeight();
                }
                for (int x = startX; x < baseImage.getWidth(); x += overlayImage.getWidth()) {
                    for (int y = startY; y < baseImage.getHeight(); y += overlayImage.getHeight()) {
                        g2d.drawImage(overlayImage, x, y, null);
                    }
                }
            } else {
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
                g2d.drawImage(overlayImage, overlayX, overlayY, null);
            }
            g2d.dispose();

            LOGGER.debug("overlayImage() executed in {}", watch);
        }
    }

    /**
     * Overlays a string onto an image.
     *
     * @param baseImage Image to overlay the string onto.
     * @param overlay   String to overlay onto the image.
     */
    private static void overlayString(final BufferedImage baseImage,
                                      final StringOverlay overlay) {
        if (overlay.hasEffect()) {
            final Stopwatch watch = new Stopwatch();

            final Graphics2D g2d = baseImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            // Graphics2D.drawString() does not understand newlines. Each line
            // has to be drawn separately.
            Font font = overlay.getFont();
            float fontSize = font.getSize();
            final int inset = overlay.getInset();
            final String[] lines = StringUtils.split(overlay.getString(), "\n");
            final int padding = getBoxPadding(overlay);
            int lineHeight;
            int totalHeight;
            int[] lineWidths;
            int maxLineWidth;
            boolean fits = false;

            // Starting at the initial font size, loop through smaller sizes
            // down to the minimum in order to find the largest that will fit
            // entirely within the image.
            while (true) {
                maxLineWidth = 0;
                g2d.setFont(font);
                final FontMetrics fm = g2d.getFontMetrics();
                lineHeight = fm.getHeight();
                totalHeight = lineHeight * lines.length;
                // Find the max line width.
                lineWidths = new int[lines.length];
                for (int i = 0; i < lines.length; i++) {
                    lineWidths[i] = fm.stringWidth(lines[i]);
                    if (lineWidths[i] > maxLineWidth) {
                        maxLineWidth = lineWidths[i];
                    }
                }

                // Will the overlay fit inside the image?
                if (maxLineWidth + (inset * 2) + (padding * 2) <= baseImage.getWidth() &&
                        totalHeight + (inset * 2) + (padding * 2) <= baseImage.getHeight()) {
                    fits = true;
                    break;
                } else {
                    if (fontSize - 1 >= overlay.getMinSize()) {
                        fontSize -= 1;
                        font = font.deriveFont(fontSize);
                    } else {
                        break;
                    }
                }
            }

            if (fits) {
                LOGGER.debug("overlayString(): using {}-point font ({} min; {} max)",
                        fontSize, overlay.getMinSize(),
                        overlay.getFont().getSize());

                g2d.drawImage(baseImage, 0, 0, null);

                final Rectangle bgBox = getBoundingBox(overlay, inset,
                        lineWidths, lineHeight,
                        new Dimension(baseImage.getWidth(), baseImage.getHeight()));

                // Draw the background, if it is not transparent.
                if (overlay.getBackgroundColor().getAlpha() > 0) {
                    g2d.setPaint(overlay.getBackgroundColor().toColor());
                    g2d.fillRect(bgBox.x, bgBox.y, bgBox.width,
                            bgBox.height);
                }

                // Draw each line individually.
                for (int i = 0; i < lines.length; i++) {
                    int x, y;
                    switch (overlay.getPosition()) {
                        case TOP_LEFT:
                            x = bgBox.x + padding;
                            y = bgBox.y + lineHeight * i + padding;
                            break;
                        case TOP_RIGHT:
                            x = bgBox.x + maxLineWidth - lineWidths[i] + padding;
                            y = bgBox.y + lineHeight * i + padding;
                            break;
                        case BOTTOM_LEFT:
                            x = bgBox.x + padding;
                            y = bgBox.y + lineHeight * i + padding;
                            break;
                        // case BOTTOM_RIGHT: will be handled in default:
                        case TOP_CENTER:
                            x = bgBox.x + Math.round((bgBox.width - lineWidths[i]) / 2f);
                            y = bgBox.y + lineHeight * i + padding;
                            break;
                        case BOTTOM_CENTER:
                            x = bgBox.x + Math.round((bgBox.width - lineWidths[i]) / 2f);
                            y = bgBox.y + lineHeight * i + padding;
                            break;
                        case LEFT_CENTER:
                            x = bgBox.x + padding;
                            y = bgBox.y + lineHeight * i + padding;
                            break;
                        case RIGHT_CENTER:
                            x = bgBox.x + maxLineWidth - lineWidths[i] + padding;
                            y = bgBox.y + lineHeight * i + padding;
                            break;
                        case CENTER:
                            x = bgBox.x + Math.round((bgBox.width - lineWidths[i]) / 2f);
                            y = bgBox.y + lineHeight * i + padding;
                            break;
                        default: // bottom right
                            x = bgBox.x + maxLineWidth - lineWidths[i] + padding;
                            y = bgBox.y + lineHeight * i + padding;
                            break;
                    }

                    // This is arbitrary fudge, but it seems to work OK.
                    y += lineHeight * 0.73;

                    // Draw the text outline.
                    if (overlay.getStrokeWidth() > 0.001f) {
                        final FontRenderContext frc = g2d.getFontRenderContext();
                        final GlyphVector gv = font.createGlyphVector(frc, lines[i]);
                        final Shape shape = gv.getOutline(x, y);
                        g2d.setStroke(new BasicStroke(overlay.getStrokeWidth()));
                        g2d.setPaint(overlay.getStrokeColor().toColor());
                        g2d.draw(shape);
                    }

                    // Draw the string.
                    g2d.setPaint(overlay.getColor().toColor());
                    g2d.drawString(lines[i], x, y);
                }
                LOGGER.debug("overlayString() executed in {}", watch);
            } else {
                LOGGER.debug("overlayString(): {}-point ({}x{}) text won't fit in {}x{} image",
                        fontSize,
                        maxLineWidth + inset,
                        totalHeight + inset,
                        baseImage.getWidth(),
                        baseImage.getHeight());
            }
            g2d.dispose();
        }
    }

    private static Rectangle getBoundingBox(final StringOverlay overlay,
                                            final int inset,
                                            final int[] lineWidths,
                                            final int lineHeight,
                                            final Dimension imageSize) {
        // If the overlay background is visible, add some padding between the
        // text and the margin.
        final int padding = getBoxPadding(overlay);
        final int boxWidth = NumberUtils.max(lineWidths) + padding * 2;
        final int boxHeight = lineHeight * lineWidths.length + padding * 2;
        int boxX, boxY;
        switch (overlay.getPosition()) {
            case TOP_LEFT:
                boxX = inset;
                boxY = inset;
                break;
            case TOP_CENTER:
                boxX = Math.round((imageSize.width - boxWidth) / 2f);
                boxY = inset;
                break;
            case TOP_RIGHT:
                boxX = imageSize.width - boxWidth - inset - padding;
                boxY = inset;
                break;
            case LEFT_CENTER:
                boxX = inset;
                boxY = Math.round((imageSize.height - boxHeight) / 2f);
                break;
            case RIGHT_CENTER:
                boxX = imageSize.width - boxWidth - inset - padding;
                boxY = Math.round((imageSize.height - boxHeight) / 2f);
                break;
            case CENTER:
                boxX = Math.round((imageSize.width - boxWidth) / 2f);
                boxY = Math.round((imageSize.height - boxHeight) / 2f);
                break;
            case BOTTOM_LEFT:
                boxX = inset;
                boxY = imageSize.height - boxHeight - inset - padding;
                break;
            // case BOTTOM_RIGHT: will be handled in default:
            case BOTTOM_CENTER:
                boxX = Math.round((imageSize.width - boxWidth) / 2f);
                boxY = imageSize.height - boxHeight - inset - padding;
                break;
            default: // bottom right
                boxX = imageSize.width - boxWidth - inset - padding;
                boxY = imageSize.height - boxHeight - inset - padding;
                break;
        }
        return new Rectangle(boxX, boxY, boxWidth, boxHeight);
    }

    private static int getBoxPadding(StringOverlay overlay) {
        return (overlay.getBackgroundColor().getAlpha() > 0) ? 5 : 0;
    }

    /**
     * @param colorModel Color model of the new image.
     * @param width      Width of the new image.
     * @param height     Height of the new image.
     * @param forceAlpha Whether the resulting image should definitely have
     *                   alpha.
     * @return           New image with the given color model and dimensions.
     */
    private static BufferedImage newImage(ColorModel colorModel,
                                          final int width,
                                          final int height,
                                          final boolean forceAlpha) {
        final boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();

        // Manually create a compatible ColorModel respecting forceAlpha.
        if (colorModel instanceof ComponentColorModel) {
            int[] componentSizes = colorModel.getComponentSize();
            // If the array does not contain an alpha element but we need it,
            // add it.
            if (!colorModel.hasAlpha() && forceAlpha) {
                int[] tmp = new int[componentSizes.length + 1];
                System.arraycopy(componentSizes, 0, tmp, 0, componentSizes.length);
                tmp[tmp.length - 1] = tmp[0];
                componentSizes = tmp;
            }
            colorModel = new ComponentColorModel(colorModel.getColorSpace(),
                    componentSizes, forceAlpha, isAlphaPremultiplied,
                    colorModel.getTransparency(), colorModel.getTransferType());
        }

        WritableRaster raster =
                colorModel.createCompatibleWritableRaster(width, height);
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }

    /**
     * Reduces an image's sample/component size to 8 bits if greater. This
     * involves copying it into a new {@link BufferedImage}, which is expensive.
     *
     * @param inImage Image to reduce.
     * @return        Reduced image, or the input image if it already is 8 bits
     *                or less.
     */
    static BufferedImage reduceTo8Bits(final BufferedImage inImage) {
        BufferedImage outImage = inImage;
        final ColorModel inColorModel = inImage.getColorModel();

        if (inColorModel.getComponentSize(0) > 8) {
            final Stopwatch watch = new Stopwatch();

            int type;
            if (inColorModel.getNumComponents() > 1) {
                type = inColorModel.hasAlpha() ?
                        BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            } else {
                type = BufferedImage.TYPE_BYTE_GRAY;
            }

            outImage = new BufferedImage(
                    inImage.getWidth(), inImage.getHeight(), type);
            final ColorConvertOp op = new ColorConvertOp(
                    inColorModel.getColorSpace(),
                    outImage.getColorModel().getColorSpace(), null);
            outImage.createGraphics().drawImage(inImage, op, 0, 0);
            LOGGER.debug("reduceTo8Bits(): converted in {}", watch);
        }
        return outImage;
    }

    /**
     * Removes alpha from an image. Transparent regions will be blended with an
     * undefined color. This involves copying the given image into a new
     * {@link BufferedImage}, which is expensive.
     *
     * @param inImage Image to remove the alpha channel from.
     * @return        Alpha-flattened image, or the input image if it has no
     *                alpha.
     */
    public static BufferedImage removeAlpha(final BufferedImage inImage) {
        return removeAlpha(inImage, null);
    }

    /**
     * Removes alpha from an image, blending transparent regions with the given
     * color. This involves copying the given image into a new {@link
     * BufferedImage}, which is expensive.
     *
     * @param inImage Image to remove the alpha channel from.
     * @return        Alpha-flattened image, or the input image if it has no
     *                alpha.
     */
    public static BufferedImage removeAlpha(final BufferedImage inImage,
                                            final Color bgColor) {
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

            if (bgColor != null) {
                g.setBackground(bgColor.toColor());
                g.clearRect(0, 0, inImage.getWidth(), inImage.getHeight());
            }

            g.drawImage(inImage, 0, 0, null);
            g.dispose();
            LOGGER.debug("removeAlpha(): executed in {}", watch);
        }
        return outImage;
    }

    /**
     * Alternative to {@link #rotate(BufferedImage, Rotate)} for orientations
     * other than {@link Orientation#ROTATE_0} when there is no
     * {@link Rotate} operation present.
     *
     * @param inImage     Image to rotate.
     * @param orientation Orientation.
     * @return            Rotated image, or the input image if the given
     *                    orientation is a no-op.
     */
    static BufferedImage rotate(final BufferedImage inImage,
                                final Orientation orientation) {
        return rotate(inImage, new Rotate(orientation.getDegrees()));
    }

    /**
     * @param inImage Image to rotate.
     * @param rotate  Rotate operation.
     * @return        Rotated image, or the input image if the given rotation
     *                is a no-op.
     */
    static BufferedImage rotate(final BufferedImage inImage,
                                final Rotate rotate) {
        BufferedImage outImage = inImage;
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
            tx.translate(canvasWidth / 2f, canvasHeight / 2f);
            // 2. rotate it
            tx.rotate(radians);
            // 1. translate the image so that it is rotated about the center
            tx.translate(-sourceWidth / 2f, -sourceHeight / 2f);

            switch (inImage.getType()) {
                case BufferedImage.TYPE_CUSTOM:
                    outImage = newImage(inImage.getColorModel(),
                            canvasWidth, canvasHeight, true);
                    break;
                case BufferedImage.TYPE_BYTE_BINARY:
                    outImage = new BufferedImage(
                            canvasWidth, canvasHeight,
                            BufferedImage.TYPE_INT_ARGB);
                    break;
                case BufferedImage.TYPE_USHORT_GRAY:
                    outImage = newImage(inImage.getColorModel(),
                            canvasWidth, canvasHeight, true);
                    break;
                default:
                    outImage = new BufferedImage(
                            canvasWidth, canvasHeight,
                            BufferedImage.TYPE_INT_ARGB);
                    break;
            }

            final Graphics2D g2d = outImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g2d.drawImage(inImage, tx, null);
            LOGGER.debug("rotate() executed in {}", watch);
        }
        return outImage;
    }

    /**
     * Scales an image.
     *
     * @param inImage Image to scale.
     * @param scale   Scale operation. Clients should call
     *                {@link Operation#hasEffect(Dimension, OperationList)}
     *                before invoking.
     * @return        Downscaled image, or the input image if the given scale
     *                is a no-op.
     */
    static BufferedImage scale(final BufferedImage inImage,
                               final Scale scale) {
        return scale(inImage, scale, new ReductionFactor(0));
    }

    /**
     * <p>Scales an image, taking an already-applied reduction factor into
     * account. In other words, the dimensions of the input image have already
     * been halved {@literal rf} times but the given size is relative to the
     * full-sized image.</p>
     *
     * <p>If one or both target dimensions would end up being less than three
     * pixels, an empty image (with the correct dimensions) will be
     * returned.</p>
     *
     * @param inImage Image to scale.
     * @param scale   Requested size ignoring any reduction factor. If no
     *                resample filter is set, a reasonable default will be used.
     *                Clients should call
     *                {@link Operation#hasEffect(Dimension, OperationList)}
     *                before invoking.
     * @param rf      Reduction factor that has already been applied to
     *                {@literal inImage}.
     * @return        Downscaled image, or the input image if the given scale
     *                is a no-op.
     */
    static BufferedImage scale(final BufferedImage inImage,
                               final Scale scale,
                               final ReductionFactor rf) {
        /*
        This method uses unnamed resampling code derived from
        com.mortennobel.imagescaling (see
        https://blog.nobel-joergensen.com/2008/12/20/downscaling-images-in-java/)
        as an alternative to the resampling available in Graphics2D.
        While the performance of Graphics2D.drawImage() is OK, the quality,
        even using RenderingHints.VALUE_INTERPOLATION_BILINEAR, is horrible.
        BufferedImage.getScaledInstance() is the opposite: great quality but
        very slow.

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

        // ResampleFilter requires both target dimensions to be at least 3
        // pixels. (OpenSeadragon has been known to request smaller.)
        // If one or both are less than that, then given that the picture is
        // virtually guaranteed to end up unrecognizable anyway, we will skip
        // the scaling step and return a fake image with the target dimensions.
        // Alternatives would be to use a different resampler, set a 3x3 floor,
        // or error out.
        BufferedImage scaledImage = inImage;
        if (targetSize.width >= 3 && targetSize.height >= 3) {
            if (scale.hasEffect() && (targetSize.width != sourceSize.width ||
                    targetSize.height != sourceSize.height)) {
                final Stopwatch watch = new Stopwatch();

                final ResampleOp resampleOp = new ResampleOp(
                        targetSize.width, targetSize.height);

                // Try to use the requested resample filter.
                ResampleFilter filter = null;
                if (scale.getFilter() != null) {
                    filter = scale.getFilter().toResampleFilter();
                }
                // No particular filter requested, so select a default.
                if (filter == null) {
                    if (targetSize.width < sourceSize.width ||
                            targetSize.height < sourceSize.height) {
                        filter = DEFAULT_DOWNSCALE_FILTER.toResampleFilter();
                    } else {
                        filter = DEFAULT_UPSCALE_FILTER.toResampleFilter();
                    }
                }
                resampleOp.setFilter(filter);

                scaledImage = resampleOp.filter(inImage, null);

                LOGGER.debug("scale(): scaled {}x{} image to {}x{} using " +
                                "a {} filter in {}",
                        sourceSize.width, sourceSize.height,
                        targetSize.width, targetSize.height,
                        filter.getName(), watch);
            }
        } else {
            scaledImage = new BufferedImage(targetSize.width, targetSize.height,
                    inImage.getType());
        }
        return scaledImage;
    }

    /**
     * @param inImage Image to sharpen.
     * @param sharpen Sharpen operation.
     * @return        Sharpened image.
     */
    static BufferedImage sharpen(final BufferedImage inImage,
                                 final Sharpen sharpen) {
        BufferedImage sharpenedImage = inImage;
        if (sharpen.hasEffect()) {
            if (inImage.getWidth() > 2 && inImage.getHeight() > 2) {
                final Stopwatch watch = new Stopwatch();

                final ResampleOp resampleOp = new ResampleOp(
                        inImage.getWidth(), inImage.getHeight());
                resampleOp.setUnsharpenMask(sharpen.getAmount());
                sharpenedImage = resampleOp.filter(inImage, null);

                LOGGER.debug("sharpen(): sharpened by {} in {}",
                        sharpen.getAmount(), watch);
            } else {
                LOGGER.debug("sharpen(): image must be at least 3 " +
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
     * @param image Image to stretch.
     */
    static void stretchContrast(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
            LOGGER.debug("stretchContrast(): can't stretch an indexed image.");
            return;
        }

        // Stretch only if there is at least this difference between
        // minimum and maximum luminance.
        final float threshold = 0.01f;
        final float maxColor =
                (float) Math.pow(2, image.getColorModel().getComponentSize(0));

        final Stopwatch watch = new Stopwatch();
        final int minX = image.getMinX();
        final int minY = image.getMinY();
        final int width = image.getWidth();
        final int height = image.getHeight();
        float lowRgb = maxColor, highRgb = 0;

        // Scan every pixel to find the darkest and brightest.
        for (int x = minX; x < minX + width; x++) {
            for (int y = minY; y < minY + height; y++) {
                final int color = image.getRGB(x, y);
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
                    final int color = image.getRGB(x, y);
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
                    image.setRGB(x, y, outColor.getRGB());
                }
            }
            LOGGER.debug("stretchContrast(): rescaled in {}", watch);
        } else {
            LOGGER.debug("stretchContrast(): not enough contrast to stretch.");
        }
    }

    /**
     * @param inImage        Image to filter.
     * @param colorTransform Operation to apply.
     * @return               Filtered image, or the input image if the given
     *                       operation is a no-op.
     */
    static BufferedImage transformColor(final BufferedImage inImage,
                                        final ColorTransform colorTransform) {
        BufferedImage outImage = inImage;
        final Stopwatch watch = new Stopwatch();

        switch (colorTransform) {
            case GRAY:
                outImage = convertIndexedTo8BitARGB(outImage);
                convertPixelsToGray(outImage);
                break;
            case BITONAL:
                if (inImage.getType() != BufferedImage.TYPE_BYTE_BINARY) {
                    outImage = new BufferedImage(
                            inImage.getWidth(),
                            inImage.getHeight(),
                            BufferedImage.TYPE_BYTE_BINARY);
                }
                if (outImage != inImage) {
                    Graphics2D g2d = outImage.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
                    g2d.drawImage(inImage, 0, 0, null);

                    LOGGER.debug("transformColor(): transformed {}x{} image in {}",
                            inImage.getWidth(), inImage.getHeight(), watch);
                }
                break;
        }
        return outImage;
    }

    /**
     * Converts an image to grayscale in-place.
     */
    private static void convertPixelsToGray(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb & 0xFF);
                int gray = (r + g + b) / 3;
                int val = (rgb & 0xff000000) | (gray << 16) | (gray << 8) | gray;
                image.setRGB(x, y, val);
            }
        }
    }

    /**
     * @param inImage   Image to transpose.
     * @param transpose Operation to apply.
     * @return          Transposed image.
     */
    static BufferedImage transpose(final BufferedImage inImage,
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

        LOGGER.debug("transpose(): transposed image in {}", watch);
        return outImage;
    }

    private Java2DUtil() {}

}
