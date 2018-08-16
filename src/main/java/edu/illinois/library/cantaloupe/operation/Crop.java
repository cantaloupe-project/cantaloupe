package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates a cropping operation, which can be expressed in {@link
 * Unit#PIXELS pixels} or {@link Unit#PERCENT} terms. Pixel-based operations
 * must be translated to &quot;effective&quot; coordinates for use, using one
 * of the {@link #getRectangle} variants.</p>
 *
 * <p>Note that {@link #isFull()} should be assumed to take precedence over all
 * other properties.</p>
 */
public class Crop implements Operation {

    public enum Shape {
        ARBITRARY, SQUARE
    }

    public enum Unit {
        PERCENT, PIXELS
    }

    private static final double DELTA = 0.00000001;

    private boolean isFrozen, isFull;
    private Shape shape = Shape.ARBITRARY;
    private Unit unit = Unit.PIXELS;
    private double x, y, width, height;

    /**
     * No-op constructor.
     */
    public Crop() {}

    /**
     * Constructor for {@link Unit#PIXELS}-based instances.
     */
    public Crop(int x, int y, int width, int height) {
        setUnit(Unit.PIXELS);
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }

    public Crop(int x, int y, int width, int height,
                Orientation orientation,
                Dimension fullSize) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
        applyOrientation(orientation, fullSize);
    }

    /**
     * Constructor for {@link Unit#PERCENT}-based instances.
     */
    public Crop(double x, double y, double width, double height) {
        setUnit(Unit.PERCENT);
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
    }

    /**
     * Modifies the coordinates of the instance to adapt to an image that is
     * to be treated as rotated. (As in e.g. the case of an EXIF Orientation
     * tag describing the rotation of un-rotated image data.)
     *
     * @param orientation Orientation of the image. If {@literal null}, the
     *                    invocation will be a no-op.
     * @param fullSize    Dimensions of the un-rotated image.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void applyOrientation(Orientation orientation, Dimension fullSize) {
        checkFrozen();
        if (orientation == null) {
            return;
        }
        if (!hasEffect() || getWidth() < 1 || getHeight() < 1) {
            return;
        }
        switch (orientation) {
            case ROTATE_90:
                double originalX = getX();
                setX(getY());
                double y = fullSize.height() - originalX - getWidth();
                setY(y >= 0 ? y : 0);
                // Swap width and height
                double originalW = getWidth();
                setWidth(getHeight());
                setHeight(Math.min(fullSize.height() - originalX, originalW));
                break;
            case ROTATE_180:
                setX(fullSize.width() - getX() - getWidth());
                setY(fullSize.height() - getY() - getHeight());
                break;
            case ROTATE_270:
                double originalY = getY();
                setY(getX());
                setX(fullSize.width() - originalY - getHeight());
                // Swap width and height
                originalW = getWidth();
                setWidth(getHeight());
                double height = (originalW <= fullSize.height() - getY()) ?
                        originalW : fullSize.height() - getY();
                setHeight(height);
                break;
        }
    }

    private void checkFrozen() {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Crop) {
            return obj.toString().equals(toString());
        }
        return super.equals(obj);
    }

    @Override
    public void freeze() {
        isFrozen = true;
    }

    /**
     * @return The height of the operation. If {@link #getUnit()} returns
     *         {@link Unit#PERCENT}, this will be a percentage of the full
     *         image height between 0 and 1.
     */
    public double getHeight() {
        return height;
    }

    /**
     * @param fullSize Full-sized image dimensions.
     * @return         Rectangle relative to the given full dimensions.
     */
    public Rectangle getRectangle(Dimension fullSize) {
        return getRectangle(fullSize, new ReductionFactor(),
                new ScaleConstraint(1, 1));
    }

    /**
     * @param fullSize        Full-sized image dimensions.
     * @param scaleConstraint Scale constraint yet to be applied to the input
     *                        image. The instance is expressed relative to this
     *                        constraint rather than to {@literal fullSize}.
     * @return                Rectangle relative to the given full dimensions.
     */
    public Rectangle getRectangle(Dimension fullSize,
                                  ScaleConstraint scaleConstraint) {
        return getRectangle(fullSize, new ReductionFactor(), scaleConstraint);
    }

    /**
     * @param reducedSize     Size of the input image, which has been reduced
     *                        by {@literal reductionFactor}.
     * @param reductionFactor Factor by which the full-sized image has been
     *                        reduced to become {@literal reducedSize}.
     * @param scaleConstraint Scale constraint yet to be applied to the input
     *                        image. The instance is expressed relative to this
     *                        constraint rather than to {@literal reducedSize}
     *                        or the full image size.
     * @return                Rectangle relative to the given reduced
     *                        dimensions.
     */
    public Rectangle getRectangle(Dimension reducedSize,
                                  ReductionFactor reductionFactor,
                                  ScaleConstraint scaleConstraint) {
        final double rfScale = reductionFactor.getScale();
        final double scScale = scaleConstraint.getScale();

        final double scale = rfScale / scScale;
        final double regionX = getX() * scale;
        final double regionY = getY() * scale;
        final double regionWidth = getWidth() * scale;
        final double regionHeight = getHeight() * scale;

        double x, y, requestedWidth, requestedHeight,
                croppedWidth, croppedHeight;
        if (isFull()) {
            x = 0;
            y = 0;
            requestedWidth = reducedSize.width();
            requestedHeight = reducedSize.height();
        } else if (Shape.SQUARE.equals(getShape())) {
            final double shortestSide =
                    Math.min(reducedSize.width(), reducedSize.height());
            x = (reducedSize.width() - shortestSide) / 2.0;
            y = (reducedSize.height() - shortestSide) / 2.0;
            requestedWidth = requestedHeight = shortestSide;
        } else if (Unit.PERCENT.equals(getUnit())) {
            x = regionX * reducedSize.width() * scScale;
            y = regionY * reducedSize.height() * scScale;
            requestedWidth = regionWidth * reducedSize.width() * scScale;
            requestedHeight = regionHeight * reducedSize.height() * scScale;
        } else {
            x = regionX;
            y = regionY;
            requestedWidth = regionWidth;
            requestedHeight = regionHeight;
        }
        // Confine width and height to the image bounds.
        croppedWidth = (x + requestedWidth > reducedSize.width()) ?
                reducedSize.width() - x : requestedWidth;
        croppedWidth = Math.max(croppedWidth, 0);
        croppedHeight = (y + requestedHeight > reducedSize.height()) ?
                reducedSize.height() - y : requestedHeight;
        croppedHeight = Math.max(croppedHeight, 0);
        return new Rectangle(x, y, croppedWidth, croppedHeight);
    }

    @Override
    public Dimension getResultingSize(Dimension fullSize,
                                      ScaleConstraint scaleConstraint) {
        return getRectangle(fullSize, scaleConstraint).size();
    }

    public Shape getShape() {
        return shape;
    }

    public Unit getUnit() {
        return unit;
    }

    /**
     * @return The width of the operation. If {@link #getUnit()} returns
     *         {@link Unit#PERCENT}, this will be a percentage of the full
     *         image width between 0 and 1.
     */
    public double getWidth() {
        return width;
    }

    /**
     * @return The left bounding coordinate of the operation. If
     * {@link #getUnit()} returns {@link Unit#PERCENT}, this will be a
     * percentage of the full image width between 0 and 1.
     */
    public double getX() {
        return x;
    }

    /**
     * @return The top bounding coordinate of the operation. If
     *         {@link #getUnit()} returns {@link Unit#PERCENT}, this will be a
     *         percentage of the full image height between 0 and 1.
     */
    public double getY() {
        return y;
    }

    /**
     * This method may produce false positives. {@link #hasEffect(Dimension,
     * OperationList)} should be used instead where possible.
     *
     * @return Whether the crop is effectively a no-op.
     */
    @Override
    public boolean hasEffect() {
        if (isFull()) {
            return false;
        } else if (Unit.PERCENT.equals(getUnit()) &&
                Math.abs(getWidth() - 1) < DELTA &&
                Math.abs(getHeight() - 1) < DELTA) {
            return false;
        }
        return true;
    }

    /**
     * @param fullSize
     * @param opList
     * @return Whether the crop is effectively a no-op.
     */
    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        if (!hasEffect() && !Unit.PERCENT.equals(getUnit())) {
            return false;
        } else if (Shape.SQUARE.equals(getShape()) &&
                fullSize.width() != fullSize.height()) {
            return true;
        } else if (Unit.PIXELS.equals(getUnit())) {
            if (getX() > 0 || getY() > 0) {
                return true;
            } else if ((Math.abs(fullSize.width() - getWidth()) > DELTA || Math.abs(fullSize.height() - getHeight()) > DELTA) &&
                    (getWidth() < fullSize.width() || getHeight() < fullSize.height())) {
                return true;
            }
        } else if (Unit.PERCENT.equals(getUnit())) {
            return getX() > DELTA || getY() > DELTA ||
                    Math.abs((getWidth() * fullSize.width()) -
                            (getX() * fullSize.width()) - fullSize.width()) > DELTA ||
                    Math.abs((getHeight() * fullSize.height()) -
                            (getY() * fullSize.height()) - fullSize.height()) > DELTA;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @return Whether the crop specifies the full source area, i.e. whether it
     *         is effectively a no-op.
     */
    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean isFull) {
        this.isFull = isFull;
    }

    /**
     * @param height Height to set.
     * @throws IllegalArgumentException if the given height is invalid.
     * @throws IllegalStateException    if the instance is frozen.
     */
    public void setHeight(double height) {
        checkFrozen();
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        }
        if (Unit.PERCENT.equals(getUnit()) && height > 1) {
            throw new IllegalArgumentException("Height percentage must be <= 1");
        }
        this.height = height;
    }

    /**
     * @param shape Shape to set.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setShape(Shape shape) {
        checkFrozen();
        this.shape = shape;
    }

    /**
     * @param unit Unit to set.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setUnit(Unit unit) {
        checkFrozen();
        this.unit = unit;
    }

    /**
     * @param width Width to set.
     * @throws IllegalArgumentException if the given width is invalid.
     * @throws IllegalStateException    if the instance is frozen.
     */
    public void setWidth(double width) {
        checkFrozen();
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        }
        if (Unit.PERCENT.equals(getUnit()) && width > 1) {
            throw new IllegalArgumentException("Width percentage must be <= 1");
        }
        this.width = width;
    }

    /**
     * @param x X coordinate to set.
     * @throws IllegalArgumentException If the given X coordinate is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setX(double x) {
        checkFrozen();
        if (x < 0) {
            throw new IllegalArgumentException("X must be positive");
        }
        if (Unit.PERCENT.equals(getUnit()) && x > 1) {
            throw new IllegalArgumentException("X percentage must be <= 1");
        }
        this.x = x;
    }

    /**
     * @param y Y coordinate to set.
     * @throws IllegalArgumentException If the given Y coordinate is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setY(double y) {
        checkFrozen();
        if (y < 0) {
            throw new IllegalArgumentException("Y must be positive");
        }
        if (Unit.PERCENT.equals(getUnit()) && y > 1) {
            throw new IllegalArgumentException("Y percentage must be <= 1");
        }
        this.y = y;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return         Map with {@literal class}, {@literal x}, {@literal y},
     *                 {@literal width}, and {@literal height} keys and integer
     *                 values corresponding to the absolute crop coordinates.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize,
                                    ScaleConstraint scaleConstraint) {
        final Rectangle rect = getRectangle(fullSize, scaleConstraint);
        final Map<String,Object> map = new HashMap<>();
        map.put("class", getClass().getSimpleName());
        map.put("x", rect.intX());
        map.put("y", rect.intY());
        map.put("width", rect.intWidth());
        map.put("height", rect.intHeight());
        return Collections.unmodifiableMap(map);
    }

    /**
     * <p>Returns a string representation of the instance, guaranteed to
     * uniquely represent the instance. The format is:</p>
     *
     * <dl>
     *     <dt>No-op</dt>
     *     <dd>none</dd>
     *     <dt>Square</dt>
     *     <dd>square</dd>
     *     <dt>Percent</dt>
     *     <dd>x%,y%,w%,h%</dd>
     *     <dt>Pixels</dt>
     *     <dd>x,y,w,h</dd>
     * </dl>
     *
     * @return String representation of the instance.
     */
    @Override
    public String toString() {
        String str = "";
        if (hasEffect()) {
            String x, y, width, height;
            if (Shape.SQUARE.equals(getShape())) {
                str += "square";
            } else {
                if (Unit.PERCENT.equals(getUnit())) {
                    x = StringUtils.removeTrailingZeroes(getX() * 100) + "%";
                    y = StringUtils.removeTrailingZeroes(getY() * 100) + "%";
                    width = StringUtils.removeTrailingZeroes(getWidth() * 100) + "%";
                    height = StringUtils.removeTrailingZeroes(getHeight() * 100) + "%";
                } else {
                    x = Long.toString(Math.round(getX()));
                    y = Long.toString(Math.round(getY()));
                    width = StringUtils.removeTrailingZeroes(getWidth());
                    height = StringUtils.removeTrailingZeroes(getHeight());
                }
                str += String.format("%s,%s,%s,%s", x, y, width, height);
            }
        } else {
            str += "none";
        }
        return str;
    }

    /**
     * Checks the crop intersection and dimensions.
     *
     * {@inheritDoc}
     */
    @Override
    public void validate(Dimension fullSize,
                         ScaleConstraint scaleConstraint) throws ValidationException {
        if (!isFull()) {
            try {
                Dimension resultingSize =
                        getResultingSize(fullSize, scaleConstraint);
                if (resultingSize.isEmpty()) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException e) {
                throw new ValidationException(
                        "Crop area is outside the bounds of the source image.");
            }
        }
    }

}
