package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.util.StringUtils;

/**
 * Encapsulates the size component of a URI.
 *
 * @see <a href="https://iiif.io/api/image/3.0/#42-size">IIIF Image API 3.0:
 * Size</a>
 */
final class Size {

    /**
     * <p>Type of size specification, corresponding to the options available
     * in <a href="https://iiif.io/api/image/3.0/#42-size">IIIF Image API 3.0:
     * Size</a>.</p>
     *
     * <p>This does not take into account the leading carat indicating
     * "upscaling allowed;" that is handled by {@link #isUpscalingAllowed()}
     * which can work in conjunction with any of these.</p>
     */
    enum Type {

        /**
         * Represents a {@code max} size argument.
         */
        MAX,

        /**
         * Represents a size argument in {@code w,} format.
         */
        ASPECT_FIT_WIDTH,

        /**
         * Represents a size argument in {@code ,h} format.
         */
        ASPECT_FIT_HEIGHT,

        /**
         * Represents a size argument in {@code w,h} format.
         */
        NON_ASPECT_FILL,

        /**
         * Represents a size argument in {@code !w,h} format.
         */
        ASPECT_FIT_INSIDE

    }

    private static final String MAX_SIZE_KEYWORD = "max";
    private static final String PERCENT_KEYWORD  = "pct";

    private Integer width, height;
    private Float percent;
    private Type type;
    private boolean isUpscalingAllowed;

    /**
     * @param uriSize The {@literal size} component of a URI.
     * @return        Size corresponding to the argument.
     * @throws IllegalClientArgumentException if the argument is invalid.
     */
    static Size fromURI(String uriSize) {
        Size size = new Size();

        // Decode the path component.
        uriSize = uriSize.replace("%5E", "^");

        if (uriSize.startsWith("^")) {
            size.setUpscalingAllowed(true);
            uriSize = uriSize.substring(1);
        }

        try {
            if (MAX_SIZE_KEYWORD.equals(uriSize)) {
                size.setType(Type.MAX);
            } else {
                if (uriSize.endsWith(",")) {
                    size.setType(Type.ASPECT_FIT_WIDTH);
                    size.setWidth(Integer.parseInt(StringUtils.stripEnd(uriSize, ",")));
                } else if (uriSize.startsWith(",")) {
                    size.setType(Type.ASPECT_FIT_HEIGHT);
                    size.setHeight(Integer.parseInt(StringUtils.stripStart(uriSize, ",")));
                } else if (uriSize.startsWith("pct:")) {
                    size.setType(Type.ASPECT_FIT_INSIDE);
                    size.setPercent(Float.parseFloat(StringUtils.stripStart(uriSize, PERCENT_KEYWORD + ":")));
                } else if (uriSize.startsWith("!")) {
                    size.setType(Type.ASPECT_FIT_INSIDE);
                    String[] parts = StringUtils.stripStart(uriSize, "!").split(",");
                    if (parts.length == 2) {
                        size.setWidth(Integer.parseInt(parts[0]));
                        size.setHeight(Integer.parseInt(parts[1]));
                    }
                } else {
                    size.setType(Type.NON_ASPECT_FILL);
                    String[] parts = uriSize.split(",");
                    if (parts.length == 2) {
                        size.setWidth(Integer.parseInt(parts[0]));
                        size.setHeight(Integer.parseInt(parts[1]));
                    } else {
                        throw new IllegalClientArgumentException("Invalid size");
                    }
                }
            }
        } catch (IllegalClientArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalClientArgumentException("Invalid size");
        }
        return size;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Size) {
            Size otherSize = (Size) obj;
            return toString().equals(otherSize.toString());
        }
        return false;
    }

    Integer getHeight() {
        return height;
    }

    /**
     * @return Float from 0-100
     */
    Float getPercent() {
        return percent;
    }

    Type getType() {
        return type;
    }

    Integer getWidth() {
        return width;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    boolean isUpscalingAllowed() {
        return isUpscalingAllowed;
    }

    void setHeight(Integer height) {
        if (height != null && height <= 0) {
            throw new IllegalClientArgumentException(
                    "Height must be a positive integer");
        }
        this.height = height;
    }

    /**
     * @param percent Float from 0-100
     * @throws IllegalClientArgumentException
     */
    void setPercent(Float percent) {
        if (percent != null && percent <= 0) {
            throw new IllegalClientArgumentException("Percent must be positive");
        }
        this.percent = percent;
    }

    void setType(Type type) {
        this.type = type;
    }

    void setUpscalingAllowed(boolean isUpscalingAllowed) {
        this.isUpscalingAllowed = isUpscalingAllowed;
    }

    void setWidth(Integer width) {
        if (width != null && width <= 0) {
            throw new IllegalClientArgumentException(
                    "Width must be a positive integer");
        }
        this.width = width;
    }

    /**
     * @param maxScale Maximum scale allowed by the application configuration.
     */
    Scale toScale(double maxScale) {
        if (getPercent() != null) {
            return new ScaleByPercent(getPercent() / 100.0);
        }
        switch (getType()) {
            case MAX:
                return new ScaleByPercent(isUpscalingAllowed() ? maxScale : 1);
            case ASPECT_FIT_WIDTH:
                return new ScaleByPixels(
                        getWidth(), null, ScaleByPixels.Mode.ASPECT_FIT_WIDTH);
            case ASPECT_FIT_HEIGHT:
                return new ScaleByPixels(
                        null, getHeight(), ScaleByPixels.Mode.ASPECT_FIT_HEIGHT);
            case ASPECT_FIT_INSIDE:
                return new ScaleByPixels(
                        getWidth(), getHeight(), ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
            case NON_ASPECT_FILL:
                return new ScaleByPixels(
                        getWidth(), getHeight(), ScaleByPixels.Mode.NON_ASPECT_FILL);
            default:
                throw new IllegalArgumentException(
                        "Unknown scale mode. This is probably a bug.");
        }
    }

    /**
     * @return Value compatible with the size component of a URI.
     */
    @Override
    public String toString() {
        String str = "";
        if (isUpscalingAllowed()) {
            str += "^";
        }
        if (Type.MAX.equals(getType())) {
            str += MAX_SIZE_KEYWORD;
        } else if (getPercent() != null) {
            str += PERCENT_KEYWORD + ":" +
                    StringUtils.removeTrailingZeroes(getPercent());
        } else {
            if (Type.ASPECT_FIT_INSIDE.equals(getType())) {
                str += "!";
            }
            if (getWidth() != null && getWidth() > 0) {
                str += getWidth();
            }
            str += ",";
            if (getHeight() != null && getHeight() > 0) {
                str += getHeight();
            }
        }
        return str;
    }

    /**
     * @param fullSize Full source image dimensions.
     * @return         Canonical value compatible with the size component of a
     *                 URI.
     * @see            #toString()
     */
    String toCanonicalString(Dimension fullSize) {
        if (Type.MAX.equals(getType())) {
            return toString();
        }

        StringBuilder b = new StringBuilder();
        long w, h;
        boolean isUp;

        if (getPercent() != null) {
            isUp = (getPercent() > 100);
            w = Math.round(fullSize.width() * getPercent() / 100.0);
            h = Math.round(fullSize.height() * getPercent() / 100.0);
        } else {
            if (getWidth() == null || getWidth() == 0) {
                double scale = getHeight() / fullSize.height();
                w = Math.round(fullSize.width() * scale);
            } else {
                w = getWidth();
            }
            if (getHeight() == null || getHeight() == 0) {
                double scale = w / fullSize.width();
                h = Math.round(fullSize.height() * scale);
            } else {
                h = getHeight();
            }
            isUp = (w > fullSize.width() || h > fullSize.height());
        }
        if (isUp) {
            b.append("^");
        }
        b.append(w);
        b.append(",");
        b.append(h);
        return b.toString();
    }

}
