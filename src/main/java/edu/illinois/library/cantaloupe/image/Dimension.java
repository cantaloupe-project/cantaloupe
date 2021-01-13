package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.util.StringUtils;

/**
 * <p>Two-dimensional area, typically an image area. Values are stored as
 * doubles, in contrast to {@link java.awt.Dimension}.</p>
 *
 * <p>Zero-dimensions are allowed, but not negative ones.</p>
 */
public final class Dimension {

    private static final double DELTA = 0.00000001;

    private double width, height;

    /**
     * @param size       Pre-scaled size.
     * @param scaledArea Area to fill.
     * @return           Resulting dimensions when {@code size} is scaled to
     *                   fill {@code scaledArea}.
     */
    public static Dimension ofScaledArea(Dimension size, int scaledArea) {
        double aspectRatio = size.width() / size.height();
        double height      = Math.sqrt(scaledArea / aspectRatio);
        double width       = scaledArea / height;
        return new Dimension(width, height);
    }

    /**
     * Double constructor.
     */
    public Dimension(double width, double height) {
        setWidth(width);
        setHeight(height);
    }

    /**
     * Integer constructor. The arguments will be converted to doubles for
     * storage.
     */
    public Dimension(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    /**
     * Copy constructor.
     */
    public Dimension(Dimension dimension) {
        this(dimension.width(), dimension.height());
    }

    public double area() {
        return width * height;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Dimension) {
            Dimension other = (Dimension) obj;
            return (Math.abs(other.width() - width()) < DELTA &&
                    Math.abs(other.height() - height()) < DELTA);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(Double.hashCode(width()) +
                Double.hashCode(height()));
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public int intArea() {
        return (int) Math.round(area());
    }

    /**
     * @return Rounded width.
     */
    public int intWidth() {
        return (int) Math.round(width);
    }

    /**
     * @return Rounded height.
     */
    public int intHeight() {
        return (int) Math.round(height);
    }

    /**
     * Swaps width and height.
     */
    public void invert() {
        double tmp = this.width;
        //noinspection SuspiciousNameCombination
        this.width = this.height;
        this.height = tmp;
    }

    /**
     * @return {@literal true} if either dimension is &lt; {@literal 0.5}.
     */
    public boolean isEmpty() {
        return (width() < 0.5 || height() < 0.5);
    }

    /**
     * Rescales both dimensions by the given amount.
     *
     * @param amount Positive number with {@literal 1} indicating no scale.
     */
    public void scale(double amount) {
        scaleX(amount);
        scaleY(amount);
    }

    /**
     * Rescales the X dimension by the given amount.
     *
     * @param amount Positive number with {@literal 1} indicating no scale.
     */
    public void scaleX(double amount) {
        if (amount < DELTA) {
            throw new IllegalArgumentException("Scale must be positive.");
        }
        width *= amount;
    }

    /**
     * Rescales the Y dimension by the given amount.
     *
     * @param amount Positive number with {@literal 1} indicating no scale.
     */
    public void scaleY(double amount) {
        if (amount < DELTA) {
            throw new IllegalArgumentException("Scale must be positive.");
        }
        height *= amount;
    }

    public void setWidth(double width) {
        if (width < 0) {
            throw new IllegalArgumentException("Width must be >= 0");
        }
        this.width = width;
    }

    public void setWidth(int width) {
        setWidth((double) width);
    }

    public void setHeight(double height) {
        if (height < 0) {
            throw new IllegalArgumentException("Height must be >= 0");
        }
        this.height = height;
    }

    public void setHeight(int height) {
        setHeight((double) height);
    }

    @Override
    public String toString() {
        return StringUtils.removeTrailingZeroes(width()) + "x" +
                StringUtils.removeTrailingZeroes(height());
    }

}
