package edu.illinois.library.cantaloupe.operation;

import com.mortennobel.imagescaling.ResampleFilter;
import com.mortennobel.imagescaling.ResampleFilters;
import edu.illinois.library.cantaloupe.util.StringUtil;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates an absolute or relative scale operation.</p>
 *
 * <p>Absolute instances will have a non-null width and/or height. Relative
 * instances will have a non-null percent and a null width and height.</p>
 */
public class Scale implements Operation {

    /**
     * <p>Represents a resample algorithm.</p>
     *
     * <p>N.B. Lowercase enum values should match configuration values.</p>
     */
    public enum Filter implements Operation {

        BELL("Bell", ResampleFilters.getBellFilter()),
        BICUBIC("Bicubic", ResampleFilters.getBiCubicFilter()),
        BOX("Box", ResampleFilters.getBoxFilter()),
        BSPLINE("B-Spline", ResampleFilters.getBSplineFilter()),
        HERMITE("Hermite", ResampleFilters.getHermiteFilter()),
        LANCZOS3("Lanczos3", ResampleFilters.getLanczos3Filter()),
        MITCHELL("Mitchell", ResampleFilters.getMitchellFilter()),
        TRIANGLE("Triangle", ResampleFilters.getTriangleFilter());

        private String name;
        private ResampleFilter resampleFilter;

        Filter(String name, ResampleFilter resampleFilter) {
            this.name = name;
            this.resampleFilter = resampleFilter;
        }

        public String getName() {
            return name;
        }

        /**
         * @param fullSize Full size of the source image on which the operation
         *                 is being applied.
         * @return The same dimension.
         */
        @Override
        public Dimension getResultingSize(Dimension fullSize) {
            return fullSize;
        }

        /**
         * @return True.
         */
        @Override
        public boolean hasEffect() {
            return true;
        }

        /**
         * @param fullSize Ignored.
         * @param opList Ignored.
         * @return True.
         */
        @Override
        public boolean hasEffect(Dimension fullSize, OperationList opList) {
            return hasEffect();
        }

        /**
         * @param fullSize Full size of the source image on which the operation
         *                 is being applied.
         * @return Map with a <code>name</code> key and string value
         *         corresponding to the filter name.
         */
        @Override
        public Map<String, Object> toMap(Dimension fullSize) {
            final Map<String,Object> map = new HashMap<>();
            map.put("class", Filter.class.getSimpleName());
            map.put("name", getName());
            return map;
        }

        /**
         * @return Equivalent ResampleFilter instance.
         */
        public ResampleFilter toResampleFilter() {
            return resampleFilter;
        }

        /**
         * @return The filter name.
         */
        @Override
        public String toString() {
            return getName();
        }

    }

    public enum Mode {
        ASPECT_FIT_HEIGHT, ASPECT_FIT_WIDTH, ASPECT_FIT_INSIDE,
        NON_ASPECT_FILL, FULL
    }

    private Filter filter;
    private Integer height;
    private Mode scaleMode = Mode.FULL;
    private Float percent;
    private Integer width;

    /**
     * No-op constructor.
     */
    public Scale() {}

    public Scale(float percent) {
        setPercent(percent);
        setMode(Mode.ASPECT_FIT_INSIDE);
    }

    /**
     * @param width  May be <code>null</code> if <code>mode</code> is
     *               {@link Mode#ASPECT_FIT_HEIGHT}.
     * @param height May be <code>null</code> if <code>mode</code> is
     *               {@link Mode#ASPECT_FIT_WIDTH}.
     * @param mode   Scale mode.
     */
    public Scale(Integer width, Integer height, Mode mode) {
        setWidth(width);
        setHeight(height);
        setMode(mode);
    }

    /**
     * @return Resample filter to prefer. May be null.
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * @return Absolute pixel height. May be null.
     */
    public Integer getHeight() {
        return height;
    }

    public Mode getMode() {
        return scaleMode;
    }

    /**
     * @return Float from 0 to 1. May be null.
     */
    public Float getPercent() {
        return percent;
    }

    /**
     * @param reducedSize Size of an image that has been halved <code>n</code>
     *                    times.
     * @param maxFactor Maximum factor to return.
     * @return Reduction factor appropriate for the instance.
     */
    public ReductionFactor getReductionFactor(final Dimension reducedSize,
                                              final int maxFactor) {
        ReductionFactor rf = new ReductionFactor();
        if (hasEffect()) {
            if (getPercent() != null) {
                rf = ReductionFactor.forScale(getPercent(), maxFactor);
            } else {
                switch (getMode()) {
                    case ASPECT_FIT_WIDTH:
                        double hvScale = (double) getWidth() /
                                (double) reducedSize.width;
                        rf = ReductionFactor.forScale(hvScale, maxFactor);
                        break;
                    case ASPECT_FIT_HEIGHT:
                        hvScale = (double) getHeight() /
                                (double) reducedSize.height;
                        rf = ReductionFactor.forScale(hvScale, maxFactor);
                        break;
                    case ASPECT_FIT_INSIDE:
                        double hScale = (double) getWidth() /
                                (double) reducedSize.width;
                        double vScale = (double) getHeight() /
                                (double) reducedSize.height;
                        rf = ReductionFactor.forScale(
                                Math.min(hScale, vScale), maxFactor);
                        break;
                }
            }
        }
        return rf;
    }

    /**
     * @param fullSize
     * @return Resulting scale when the scale is applied to the given full
     *         size; or null if the scale mode is {@link Mode#NON_ASPECT_FILL}.
     */
    public Float getResultingScale(Dimension fullSize) {
        Float scale = null;
        if (this.getPercent() != null) {
            scale = this.getPercent();
        } else {
            switch (this.getMode()) {
                case FULL:
                    scale = 1f;
                    break;
                case ASPECT_FIT_HEIGHT:
                    scale = (float) (this.getHeight() /
                            (double) fullSize.height);
                    break;
                case ASPECT_FIT_WIDTH:
                    scale = (float) (this.getWidth() /
                            (double) fullSize.width);
                    break;
                case ASPECT_FIT_INSIDE:
                    scale = (float) Math.min(
                            this.getWidth() / (double) fullSize.width,
                            this.getHeight() / (double) fullSize.height);
                    break;
                case NON_ASPECT_FILL:
                    break;
            }
        }
        return scale;
    }

    /**
     * @param fullSize
     * @return Resulting dimensions when the scale is applied to the given full
     *         size.
     */
    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        Dimension size = new Dimension(fullSize.width, fullSize.height);
        if (this.getPercent() != null) {
            size.width *= this.getPercent();
            size.height *= this.getPercent();
        } else {
            switch (this.getMode()) {
                case ASPECT_FIT_HEIGHT:
                    double scalePct = this.getHeight() / (double) size.height;
                    size.width = (int) Math.round(size.width * scalePct);
                    size.height = (int) Math.round(size.height * scalePct);
                    break;
                case ASPECT_FIT_WIDTH:
                    scalePct = this.getWidth() / (double) size.width;
                    size.width = (int) Math.round(size.width * scalePct);
                    size.height = (int) Math.round(size.height * scalePct);
                    break;
                case ASPECT_FIT_INSIDE:
                    scalePct = Math.min(
                            this.getWidth() / (double) size.width,
                            this.getHeight() / (double) size.height);
                    size.width = (int) Math.round(size.width * scalePct);
                    size.height = (int) Math.round(size.height * scalePct);
                    break;
                case NON_ASPECT_FILL:
                    size.width = this.getWidth();
                    size.height = this.getHeight();
                    break;
            }
        }
        return size;
    }

    /**
     * @return Absolute pixel width. May be null.
     */
    public Integer getWidth() {
        return width;
    }

    @Override
    public boolean hasEffect() {
        return (!Mode.FULL.equals(getMode())) &&
                ((getPercent() != null && Math.abs(getPercent() - 1f) > 0.000001f) ||
                        (getPercent() == null && (getHeight() != null || getWidth() != null)));
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        if (!hasEffect()) {
            return false;
        }

        Dimension cropSize = fullSize;
        for (Operation op : opList) {
            if (op instanceof Crop) {
                cropSize = op.getResultingSize(cropSize);
            }
        }

        switch (getMode()) {
            case ASPECT_FIT_WIDTH:
                return getWidth() != cropSize.width;
            case ASPECT_FIT_HEIGHT:
                return getHeight() != cropSize.height;
            default:
                if (getPercent() != null) {
                    return Math.abs(this.getPercent() - 1f) > 0.000001f;
                }
                return getWidth() != cropSize.width || getHeight() != cropSize.height;
        }
    }

    /**
     * @param comparedToSize
     * @return Whether the instance would effectively upscale the image it is
     *         applied to, i.e. whether the resulting image would have more
     *         pixels.
     */
    public boolean isUp(Dimension comparedToSize) {
        Dimension resultingSize = getResultingSize(comparedToSize);
        return resultingSize.width * resultingSize.height >
                comparedToSize.width * comparedToSize.height;
    }

    /**
     * @param filter Resample filter to prefer.
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * @param height Integer greater than 0
     * @throws IllegalArgumentException
     */
    public void setHeight(Integer height) throws IllegalArgumentException {
        if (height != null && height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        }
        this.height = height;
    }

    /**
     * N.B. Invoking this method also sets the instance's mode to
     * {@link Mode#ASPECT_FIT_INSIDE}.
     *
     * @param percent Float greater than 0.
     * @throws IllegalArgumentException
     */
    public void setPercent(Float percent) throws IllegalArgumentException {
        if (percent != null && percent <= 0) {
            throw new IllegalArgumentException("Percent must be greater than zero");
        }
        this.setMode(Mode.ASPECT_FIT_INSIDE);
        this.percent = percent;
    }

    public void setMode(Mode scaleMode) {
        this.scaleMode = scaleMode;
    }

    /**
     * @param width Integer greater than 0.
     * @throws IllegalArgumentException
     */
    public void setWidth(Integer width) throws IllegalArgumentException {
        if (width != null && width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        }
        this.width = width;
    }

    /**
     * @param fullSize Full size of the source image on which the operation
     *                 is being applied.
     * @return Map with <code>width</code> and <code>height</code> keys
     *         and integer values corresponding to the resulting pixel size of
     *         the operation.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize) {
        final Dimension resultingSize = getResultingSize(fullSize);
        final Map<String,Object> map = new HashMap<>();
        map.put("class", Scale.class.getSimpleName());
        map.put("width", resultingSize.width);
        map.put("height", resultingSize.height);
        return map;
    }

    /**
     * <p>Returns a string representation of the instance, guaranteed to
     * uniquely represent the instance. The format is:</p>
     *
     * <dl>
     *     <dt>No-op</dt>
     *     <dd><code>none</code></dd>
     *     <dt>Percent</dt>
     *     <dd><code>nnn%(,filter)</code></dd>
     *     <dt>Aspect-fit-inside</dt>
     *     <dd><code>!w,h(,filter)</code></dd>
     *     <dt>Other</dt>
     *     <dd><code>w,h(,filter)</code></dd>
     * </dl>
     *
     * @return String representation of the instance.
     */
    @Override
    public String toString() {
        String str = "";
        if (!hasEffect()) {
            return "none";
        } else if (this.getPercent() != null) {
            str += StringUtil.removeTrailingZeroes(this.getPercent() * 100) + "%";
        } else {
            if (this.getMode().equals(Mode.ASPECT_FIT_INSIDE)) {
                str += "!";
            }
            if (this.getWidth() != null && this.getWidth() > 0) {
                str += this.getWidth();
            }
            str += ",";
            if (this.getHeight() != null && this.getHeight() > 0) {
                str += this.getHeight();
            }
        }
        if (getFilter() != null) {
            str += "," + getFilter().name().toLowerCase();
        }
        return str;
    }

}
