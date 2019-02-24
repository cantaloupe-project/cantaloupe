package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.util.StringUtils;

public class CropByPixels extends Crop implements Operation {

    private int x, y, width, height;

    /**
     * @param x      X origin in the range {@literal 0 <= x}.
     * @param y      Y origin in the range {@literal 0 <= y}.
     * @param width  Width in the range {@literal 0 < width}.
     * @param height Height in the range {@literal 0 < height}.
     * @throws IllegalArgumentException if any of the arguments are invalid.
     */
    public CropByPixels(int x, int y, int width, int height) {
        super();
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }

    /**
     * @return The X origin of the operation, expressed in pixels.
     */
    public int getX() {
        return x;
    }

    /**
     * @return The Y origin of the operation, expressed in pixels.
     */
    public int getY() {
        return y;
    }

    /**
     * @return The width of the operation, expressed in pixels.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The height of the operation, expressed in pixels.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param reducedSize     Size of the input image, reduced by {@literal
     *                        reductionFactor}.
     * @param reductionFactor Factor by which the full-sized image has been
     *                        reduced to become {@literal reducedSize}.
     * @param scaleConstraint Scale constraint yet to be applied to the input
     *                        image. The instance is expressed relative to this
     *                        constraint rather than to {@literal reducedSize}
     *                        or the full image size.
     * @return                Rectangle relative to the given reduced
     *                        dimensions.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public Rectangle getRectangle(Dimension reducedSize,
                                  ReductionFactor reductionFactor,
                                  ScaleConstraint scaleConstraint) {
        final double rfScale = reductionFactor.getScale();
        final double scScale = scaleConstraint.getRational().doubleValue();
        final double scale   = rfScale / scScale;

        double x      = getX() * scale;
        double y      = getY() * scale;
        double width  = getWidth() * scale;
        double height = getHeight() * scale;

        switch (orientation) {
            case ROTATE_90: // image is rotated counterclockwise
                double initialX = x;
                double initialW = width;
                x = y;
                y = reducedSize.height() - initialX - width;
                y = (y < 0) ? 0 : y;
                width = height;
                height = Math.min(reducedSize.intHeight() - initialX, initialW);
                break;
            case ROTATE_180:
                x = reducedSize.width() - x - width;
                y = reducedSize.height() - y - height;
                x = (x < 0) ? 0 : x;
                y = (y < 0) ? 0 : y;
                break;
            case ROTATE_270: // image is rotated clockwise
                double initialY = y;
                y = x;
                x = reducedSize.width() - initialY - height;
                x = (x < 0) ? 0 : x;
                initialW = width;
                width = height;
                height = (initialW <= reducedSize.height() - y) ?
                        initialW :
                        reducedSize.height() - y;
                break;
        }

        // Clip dimensions to the image bounds.
        width = (x + width > reducedSize.width()) ?
                reducedSize.width() - x : width;
        height = (y + height > reducedSize.height()) ?
                reducedSize.height() - y : height;
        return new Rectangle(x, y, width, height);
    }

    /**
     * May produce false positives. {@link #hasEffect(Dimension,
     * OperationList)} should be used instead where possible.
     *
     * @return Whether the crop is not effectively a no-op.
     */
    @Override
    public boolean hasEffect() {
        return true;
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        if (!hasEffect()) {
            return false;
        } else {
            if (getX() > 0 || getY() > 0) {
                return true;
            } else if ((Math.abs(fullSize.width() - getWidth()) > DELTA ||
                    Math.abs(fullSize.height() - getHeight()) > DELTA) &&
                    (getWidth() < fullSize.width() || getHeight() < fullSize.height())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param x X coordinate to set.
     * @throws IllegalArgumentException If the given X coordinate is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setX(int x) {
        checkFrozen();
        if (x < 0) {
            throw new IllegalArgumentException("X must >= 0");
        }
        this.x = x;
    }

    /**
     * @param y Y coordinate to set.
     * @throws IllegalArgumentException If the given Y coordinate is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setY(int y) {
        checkFrozen();
        if (y < 0) {
            throw new IllegalArgumentException("Y must be >= 0");
        }
        this.y = y;
    }

    /**
     * @param width Width to set.
     * @throws IllegalArgumentException if the given width is invalid.
     * @throws IllegalStateException    if the instance is frozen.
     */
    public void setWidth(int width) {
        checkFrozen();
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be > 0");
        }
        this.width = width;
    }

    /**
     * @param height Height to set.
     * @throws IllegalArgumentException if the given height is invalid.
     * @throws IllegalStateException    if the instance is frozen.
     */
    public void setHeight(int height) {
        checkFrozen();
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be > 0");
        }
        this.height = height;
    }

    @Override
    public String toString() {
        return String.format("%d,%d,%s,%s",
                getX(), getY(),
                StringUtils.removeTrailingZeroes(getWidth()),
                StringUtils.removeTrailingZeroes(getHeight()));
    }

}
