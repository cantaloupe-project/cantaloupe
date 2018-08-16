package edu.illinois.library.cantaloupe.image;

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
     */
    public void scaleBy(double scale) {
        if (scale < DELTA) {
            throw new IllegalArgumentException("Scale must be positive.");
        }
        width *= scale;
        height *= scale;
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
        return intWidth() + "x" + intHeight();
    }

}
