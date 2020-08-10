package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
import edu.illinois.library.cantaloupe.operation.CropByPixels;
import edu.illinois.library.cantaloupe.operation.CropToSquare;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.util.StringUtils;

/**
 * Encapsulates the region component of a URI.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#41-region">IIIF Image API 3.0:
 * Region</a>
 */
final class Region {

    /**
     * <p>Type of region specification, corresponding to the options available
     * in <a href="https://iiif.io/api/image/3.0/#41-region">IIIF Image API
     * 3.0: Region</a>, described verbatim.</p>
     */
    enum Type {

        /**
         * The full image is returned, without any cropping.
         */
        FULL,

        /**
         * The region to be returned is specified as a sequence of
         * percentages of the full image’s dimensions, as reported in the image
         * information document. Thus, x represents the number of pixels from
         * the 0 position on the horizontal axis, calculated as a percentage of
         * the reported width. w represents the width of the region, also
         * calculated as a percentage of the reported width. The same applies
         * to y and h respectively.
         */
        PERCENT,

        /**
         * The region of the full image to be returned is specified
         * in terms of absolute pixel values. The value of x represents the
         * number of pixels from the 0 position on the horizontal axis. The
         * value of y represents the number of pixels from the 0 position on
         * the vertical axis. Thus the x,y position 0,0 is the upper left-most
         * pixel of the image. w represents the width of the region and h
         * represents the height of the region in pixels.
         */
        PIXELS,

        /**
         * The region is defined as an area where the width and
         * height are both equal to the length of the shorter dimension of the
         * full image. The region may be positioned anywhere in the longer
         * dimension of the full image at the server’s discretion, and centered
         * is often a reasonable default.
         */
        SQUARE
    }

    private static final double DELTA = 0.00000001;

    private static final String FULL_REGION_KEYWORD    = "full";
    private static final String PERCENT_REGION_KEYWORD = "pct:";
    private static final String SQUARE_REGION_KEYWORD  = "square";

    private Double x, y, width, height;
    private Type type;

    /**
     * @param uriRegion Region component of a URI.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    static Region fromURI(String uriRegion) {
        final Region region = new Region();
        switch (uriRegion) {
            case FULL_REGION_KEYWORD:
                region.setType(Type.FULL);
                break;
            case SQUARE_REGION_KEYWORD:
                region.setType(Type.SQUARE);
                break;
            default:
                String csv;
                if (uriRegion.startsWith(PERCENT_REGION_KEYWORD)) {
                    region.setType(Type.PERCENT);
                    csv = uriRegion.substring(PERCENT_REGION_KEYWORD.length());
                } else {
                    region.setType(Type.PIXELS);
                    csv = uriRegion;
                }
                String[] parts = csv.split(",");
                if (parts.length == 4) {
                    region.setX(Double.parseDouble(parts[0]));
                    region.setY(Double.parseDouble(parts[1]));
                    region.setWidth(Double.parseDouble(parts[2]));
                    region.setHeight(Double.parseDouble(parts[3]));
                } else {
                    throw new IllegalClientArgumentException("Invalid region");
                }
                break;
        }
        return region;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Region) {
            final Region region = (Region) obj;
            if (Type.FULL.equals(getType()) &&
                    Type.FULL.equals(region.getType())) {
                return true;
            } else if (Type.SQUARE.equals(getType()) &&
                    Type.SQUARE.equals(region.getType())) {
                return true;
            }
            return getType().equals(region.getType()) &&
                    Math.abs(getX() - region.getX()) < DELTA &&
                    Math.abs(getY() - region.getY()) < DELTA &&
                    Math.abs(getWidth() - region.getWidth()) < DELTA &&
                    Math.abs(getHeight() - region.getHeight()) < DELTA;
        }
        return super.equals(obj);
    }

    Type getType() {
        return type;
    }

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    double getWidth() {
        return width;
    }

    double getHeight() {
        return height;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    void setType(Type type) {
        this.type = type;
    }

    void setX(double x) {
        if (x < 0) {
            throw new IllegalClientArgumentException("X must be >= 0");
        }
        this.x = x;
    }

    void setY(double y) {
        if (y < 0) {
            throw new IllegalClientArgumentException("Y must be >= 0");
        }
        this.y = y;
    }

    void setWidth(double width) {
        if (width <= 0) {
            throw new IllegalClientArgumentException("Width must be > 0");
        }
        this.width = width;
    }

    void setHeight(double height) {
        if (height <= 0) {
            throw new IllegalClientArgumentException("Height must be > 0");
        }
        this.height = height;
    }

    /**
     * @return Equivalent instance, or {@code null} if the {@link #getType()
     *         type} is {@link Type#FULL}.
     */
    Crop toCrop() {
        if (Type.FULL.equals(getType())) {
            return new CropByPercent(); // 100% crop
        } else if (Type.SQUARE.equals(getType())) {
            return new CropToSquare();
        } else if (Type.PERCENT.equals(getType())) {
            return new CropByPercent(
                    getX() / 100.0,
                    getY() / 100.0,
                    getWidth() / 100.0,
                    getHeight() / 100.0);
        } else {
            return new CropByPixels(
                    (int) Math.round(getX()),
                    (int) Math.round(getY()),
                    (int) Math.round(getWidth()),
                    (int) Math.round(getHeight()));
        }
    }

    /**
     * @return Value compatible with the region component of an IIIF URI.
     * @see    #toCanonicalString(Dimension)
     */
    @Override
    public String toString() {
        String str;
        switch (getType()) {
            case FULL:
                str = FULL_REGION_KEYWORD;
                break;
            case SQUARE:
                str = SQUARE_REGION_KEYWORD;
                break;
            default:
                String x, y;
                if (getType().equals(Type.PERCENT)) {
                    str = PERCENT_REGION_KEYWORD;
                    x = StringUtils.removeTrailingZeroes(getX());
                    y = StringUtils.removeTrailingZeroes(getY());
                } else {
                    str = "";
                    x = Long.toString(Math.round(getX()));
                    y = Long.toString(Math.round(getY()));
                }
                str += String.format("%s,%s,%s,%s", x, y,
                        StringUtils.removeTrailingZeroes(getWidth()),
                        StringUtils.removeTrailingZeroes(getHeight()));
                break;
        }
        return str;
    }

    /**
     * @param fullSize Full source image dimensions.
     * @return         Canonical value compatible with the region component of
     *                 an IIIF URI.
     * @see            #toString()
     */
    String toCanonicalString(Dimension fullSize) {
        if (Type.PERCENT.equals(getType())) {
            return String.format("%d,%d,%d,%d",
                    Math.round(getX() / 100.0 * fullSize.width()),
                    Math.round(getY() / 100.0 * fullSize.height()),
                    Math.round(getWidth() / 100.0 * fullSize.width()),
                    Math.round(getHeight() / 100.0 * fullSize.height()));
        }
        return toString();
    }

}
