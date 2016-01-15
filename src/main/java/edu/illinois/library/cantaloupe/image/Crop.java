package edu.illinois.library.cantaloupe.image;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates a cropping operation.</p>
 *
 * <p>Note that {@link #isFull()} can be assumed to take precedence over all
 * other properties.</p>
 */
public class Crop implements Operation {

    public enum Unit {
        PERCENT, PIXELS;
    }

    private float height = 0.0f;
    private boolean isFull = false;
    private Unit unit = Unit.PIXELS;
    private float width = 0.0f;
    private float x = 0.0f;
    private float y = 0.0f;

    /**
     * @param rect Rectangle to imitate.
     * @return Crop instance analogous to the given rectangle.
     */
    public static Crop fromRectangle(Rectangle rect) {
        Crop crop = new Crop();
        crop.setX(rect.x);
        crop.setY(rect.y);
        crop.setWidth(rect.width);
        crop.setHeight(rect.height);
        return crop;
    }

    /**
     * @return The height of the operation. If {@link #getUnit()} returns
     * {@link Unit#PERCENT}, this will be a percentage of the full image height
     * between 0 and 1.
     */
    public float getHeight() {
        return height;
    }

    /**
     * @param fullSize Full-sized image dimensions.
     * @return Crop coordinates relative to the given full-sized image
     * dimensions.
     */
    public Rectangle getRectangle(Dimension fullSize) {
        int x, y, width, height;
        if (this.isFull()) {
            x = 0;
            y = 0;
            width = fullSize.width;
            height = fullSize.height;
        } else if (this.getUnit().equals(Unit.PERCENT)) {
            x = Math.round(this.getX() * fullSize.width);
            y = Math.round(this.getY() * fullSize.height);
            width = Math.round(this.getWidth() * fullSize.width);
            height = Math.round(this.getHeight() * fullSize.height);
        } else {
            x = Math.round(this.getX());
            y = Math.round(this.getY());
            width = Math.round(this.getWidth());
            height = Math.round(this.getHeight());
        }
        // confine width and height to the source image bounds
        width = Math.min(width, fullSize.width - x);
        height = Math.min(height, fullSize.height - y);
        return new Rectangle(x, y, width, height);
    }

    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return getRectangle(fullSize).getSize();
    }

    public Unit getUnit() {
        return unit;
    }

    /**
     * @return The width of the operation. If {@link #getUnit()} returns
     * {@link Unit#PERCENT}, this will be a percentage of the full image width
     * between 0 and 1.
     */
    public float getWidth() {
        return width;
    }

    /**
     * @return The left bounding coordinate of the operation. If
     * {@link #getUnit()} returns {@link Unit#PERCENT}, this will be a
     * percentage of the full image width between 0 and 1.
     */
    public float getX() {
        return x;
    }

    /**
     * @return The top bounding coordinate of the operation. If
     * {@link #getUnit()} returns {@link Unit#PERCENT}, this will be a
     * percentage of the full image height between 0 and 1.
     */
    public float getY() {
        return y;
    }

    /**
     * @return Whether the crop specifies the full source area, i.e. whether it
     * is effectively a no-op.
     */
    public boolean isFull() {
        return this.isFull;
    }

    /**
     * @return Whether the crop is effectively a no-op.
     */
    @Override
    public boolean isNoOp() {
        if (this.isFull()) {
            return true;
        } else if (this.getUnit().equals(Unit.PERCENT) &&
                Math.abs(this.getWidth() - 1f) < 0.000001f &&
                Math.abs(this.getHeight() - 1f) < 0.000001f) {
            return true;
        }
        return false;
    }

    public void setFull(boolean isFull) {
        this.isFull = isFull;
    }

    public void setHeight(float height) throws IllegalArgumentException {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        } else if (this.getUnit().equals(Unit.PERCENT) && height > 1) {
            throw new IllegalArgumentException("Height percentage must be <= 1");
        }
        this.height = height;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public void setWidth(float width) throws IllegalArgumentException {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        } else if (this.getUnit().equals(Unit.PERCENT) && width > 1) {
            throw new IllegalArgumentException("Width percentage must be <= 1");
        }
        this.width = width;
    }

    public void setX(float x) throws IllegalArgumentException {
        if (x < 0) {
            throw new IllegalArgumentException("X must be a positive float");
        } else if (this.getUnit().equals(Unit.PERCENT) && x > 1) {
            throw new IllegalArgumentException("X percentage must be <= 1");
        }
        this.x = x;
    }

    public void setY(float y) throws IllegalArgumentException {
        if (y < 0) {
            throw new IllegalArgumentException("Y must be a positive float");
        } else if (this.getUnit().equals(Unit.PERCENT) && y > 1) {
            throw new IllegalArgumentException("Y percentage must be <= 1");
        }
        this.y = y;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map with <code>x</code>, <code>y</code>, <code>width</code>, and
     *         <code>height</code> keys and integer values corresponding to the
     *         absolute crop coordinates.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize) {
        final Rectangle rect = getRectangle(fullSize);
        final HashMap<String,Object> map = new HashMap<>();
        map.put("x", rect.x);
        map.put("y", rect.y);
        map.put("width", rect.width);
        map.put("height", rect.height);
        return map;
    }

    /**
     * Returns a string representation of the instance, guaranteed to uniquely
     * represent the instance. The format is:
     *
     * <dl>
     *     <dt>No-op</dt>
     *     <dd>none</dd>
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
        if (this.isNoOp()) {
            str += "none";
        } else {
            String x, y, width, height;
            if (this.getUnit().equals(Unit.PERCENT)) {
                x = NumberUtil.removeTrailingZeroes(this.getX() * 100) + "%";
                y = NumberUtil.removeTrailingZeroes(this.getY() * 100) + "%";
                width = NumberUtil.removeTrailingZeroes(this.getWidth() * 100) + "%";
                height = NumberUtil.removeTrailingZeroes(this.getHeight() * 100) + "%";
            } else {
                x = Integer.toString(Math.round(this.getX()));
                y = Integer.toString(Math.round(this.getY()));
                width = NumberUtil.removeTrailingZeroes(this.getWidth());
                height = NumberUtil.removeTrailingZeroes(this.getHeight());
            }
            str += String.format("%s,%s,%s,%s", x, y, width, height);
        }
        return str;
    }

}
