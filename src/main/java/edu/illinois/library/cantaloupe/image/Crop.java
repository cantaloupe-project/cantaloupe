package edu.illinois.library.cantaloupe.image;

import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * <p>Encapsulates a cropping operation.</p>
 *
 * <p>Note that {@link #isFull()} can be assumed to take precedence over all
 * other properties.</p>
 */
public class Crop {

    private Float height = 0.0f;
    private boolean isFull = false;
    private boolean isPercent = false;
    private Float width = 0.0f;
    private Float x = 0.0f;
    private Float y = 0.0f;

    /**
     * @return The height of the operation. If {@link #isPercent()} returns
     * <code>true</code>, this will be a percentage of the full image height
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
        } else if (this.isPercent()) {
            x = Math.round((this.getX() / 100f) * fullSize.width);
            y = Math.round((this.getY() / 100f) * fullSize.height);
            width = Math.round((this.getWidth() / 100f) * fullSize.width);
            height = Math.round((this.getHeight() / 100f) * fullSize.height);
        } else {
            x = Math.round(this.getX());
            y = Math.round(this.getY());
            width = Math.round(this.getWidth());
            height = Math.round(this.getHeight());
        }
        return new Rectangle(x, y, width, height);
    }

    /**
     * @return The width of the operation. If {@link #isPercent()} returns
     * <code>true</code>, this will be a percentage of the full image width
     * between 0 and 1.
     */
    public float getWidth() {
        return width;
    }

    /**
     * @return The left bounding coordinate of the operation. If
     * {@link #isPercent()} returns <code>true</code>, this will be a
     * percentage of the full image width between 0 and 1.
     */
    public float getX() {
        return x;
    }

    /**
     * @return The top bounding coordinate of the operation. If
     * {@link #isPercent()} returns <code>true</code>, this will be a
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

    public boolean isPercent() {
        return this.isPercent;
    }

    public void setFull(boolean isFull) {
        this.isFull = isFull;
    }

    public void setHeight(Float height) throws IllegalArgumentException {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        }
        this.height = height;
    }

    public void setPercent(boolean isPercent) {
        this.isPercent = isPercent;
    }

    public void setWidth(Float width) throws IllegalArgumentException {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        }
        this.width = width;
    }

    public void setX(Float x) throws IllegalArgumentException {
        if (x < 0) {
            throw new IllegalArgumentException("X must be a positive float");
        }
        this.x = x;
    }

    public void setY(Float y) throws IllegalArgumentException {
        if (y < 0) {
            throw new IllegalArgumentException("Y must be a positive float");
        }
        this.y = y;
    }

    /**
     * @return String representation of the instance, guaranteed to represent
     * the instance, but not guaranteed to be meaningful.
     */
    @Override
    public String toString() {
        String str = "";
        if (this.isFull()) {
            str += "full";
        } else {
            String x, y;
            if (this.isPercent()) {
                x = NumberUtil.removeTrailingZeroes(this.getX());
                y = NumberUtil.removeTrailingZeroes(this.getY());
                str += "pct:";
            } else {
                x = Integer.toString(Math.round(this.getX()));
                y = Integer.toString(Math.round(this.getY()));
            }
            str += String.format("%s,%s,%s,%s", x, y,
                    NumberUtil.removeTrailingZeroes(this.getWidth()),
                    NumberUtil.removeTrailingZeroes(this.getHeight()));
        }
        return str;
    }

}
