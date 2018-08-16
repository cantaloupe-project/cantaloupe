package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.processor.resample.ResampleFilter;
import edu.illinois.library.cantaloupe.processor.resample.ResampleFilters;
import edu.illinois.library.cantaloupe.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Encapsulates an absolute (pixel-based) or relative (percentage-based)
 * scale operation.</p>
 *
 * <p>Absolute instances will have a non-null width and/or height. Relative
 * instances will have a non-null percent and a null width and height.</p>
 *
 * <p>N.B.: The accessors ({@link #getWidth()}, {@link #setWidth(Integer)},
 * etc.) define the scale operation, but they should not be used when figuring
 * out how to apply an instance to an image. For that, {@link
 * #getResultingSize} and {@link #getResultingScale} should be used
 * instead.</p>
 */
public class Scale implements Operation {

    /**
     * <p>Represents a resample algorithm.</p>
     *
     * <p>N.B.: Lowercase enum values match configuration values.</p>
     */
    public enum Filter implements Operation {

        BELL("Bell",         ResampleFilters.getBellFilter()),
        BICUBIC("Bicubic",   ResampleFilters.getBiCubicFilter()),
        BOX("Box",           ResampleFilters.getBoxFilter()),
        BSPLINE("B-Spline",  ResampleFilters.getBSplineFilter()),
        HERMITE("Hermite",   ResampleFilters.getHermiteFilter()),
        LANCZOS3("Lanczos3", ResampleFilters.getLanczos3Filter()),
        MITCHELL("Mitchell", ResampleFilters.getMitchellFilter()),
        TRIANGLE("Triangle", ResampleFilters.getTriangleFilter());

        private String name;
        private ResampleFilter resampleFilter;

        Filter(String name, ResampleFilter resampleFilter) {
            this.name = name;
            this.resampleFilter = resampleFilter;
        }

        /**
         * Does nothing.
         */
        @Override
        public void freeze() {
            // no-op
        }

        public String getName() {
            return name;
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
         * @param opList   Ignored.
         * @return         True.
         */
        @Override
        public boolean hasEffect(Dimension fullSize, OperationList opList) {
            return hasEffect();
        }

        /**
         * @return Map with a {@literal name} key and string value
         *         corresponding to the filter name.
         */
        @Override
        public Map<String, Object> toMap(Dimension fullSize,
                                         ScaleConstraint scaleConstraint) {
            final Map<String,Object> map = new HashMap<>();
            map.put("class", Filter.class.getSimpleName());
            map.put("name", getName());
            return Collections.unmodifiableMap(map);
        }

        /**
         * @return Equivalent {@link ResampleFilter} instance.
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

        /**
         * Scale to fit the X axis inside a rectangle's X axis, maintaining
         * aspect ratio.
         */
        ASPECT_FIT_WIDTH,

        /**
         * Scale to fit the Y axis inside a rectangle's Y axis, maintaining
         * aspect ratio.
         */
        ASPECT_FIT_HEIGHT,

        /**
         * Scale to fit entirely inside a rectangle, maintaining aspect ratio.
         */
        ASPECT_FIT_INSIDE,

        /**
         * Fill an arbitrary rectangle without necessarily maintaining aspect
         * ratio.
         */
        NON_ASPECT_FILL,

        /**
         * Full scale.
         */
        FULL
    }

    private static final double DELTA = 0.00000001;

    private Filter filter;
    private boolean isFrozen;
    private Mode scaleMode = Mode.FULL;

    /**
     * Stores percentages. If set, {@link #width} and {@link #height} must be
     * {@literal null}.
     */
    private Double percent;

    /**
     * Stores pixel sizes. If either are set, {@link #percent} must be
     * {@literal null}.
     */
    private Integer width, height;

    /**
     * No-op constructor.
     */
    public Scale() {}

    /**
     * Percent-based constructor.
     *
     * @param percent Value between {@literal 0} and {@literal 1} to represent
     *                a downscale, or above {@literal 1} to represent an
     *                upscale.
     */
    public Scale(double percent) {
        setPercent(percent);
    }

    /**
     * Pixel-based constructor.
     *
     * @param width  May be {@literal null} if {@literal mode} is {@link
     *               Mode#ASPECT_FIT_HEIGHT}.
     * @param height May be {@literal null} if {@literal mode} is {@link
     *               Mode#ASPECT_FIT_WIDTH}.
     * @param mode   Scale mode.
     */
    public Scale(Integer width, Integer height, Mode mode) {
        setWidth(width);
        setHeight(height);
        setMode(mode);
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
        } else if (obj instanceof Scale) {
            Scale other = (Scale) obj;
            return other.toString().equals(toString());
        }
        return super.equals(obj);
    }

    @Override
    public void freeze() {
        isFrozen = true;
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
     * @return Double from 0 to 1. May be null.
     */
    public Double getPercent() {
        return percent;
    }

    /**
     * <p>Finds the required additional differential scaling of an image at a
     * reduced resolution level.</p>
     *
     * <p>For example, the client has requested a scale of 45%, and the reader
     * has returned an image with a {@link ReductionFactor#factor} of
     * {@literal 1} (50%). The amount that that intermediate image must be
     * further downscaled will be returned.</p>
     *
     * @param reducedSize     Image dimensions, which have been reduced {@link
     *                        ReductionFactor#factor} times.
     * @param reductionFactor Reduction factor that has reduced a source image
     *                        to {@literal reducedSize}.
     * @param scaleConstraint Scale constraint relative to the full source
     *                        image dimensions.
     * @return                Scale yet to be applied to an intermediate image
     *                        of the given reduced size with the given reduction
     *                        factor, or {@literal null} if the scale mode is
     *                        {@link Mode#NON_ASPECT_FILL}. {@literal 1}
     *                        indicates no scaling needed.
     */
    public Double getDifferentialScale(final Dimension reducedSize,
                                       final ReductionFactor reductionFactor,
                                       final ScaleConstraint scaleConstraint) {
        if (Mode.FULL.equals(getMode())) {
            return 1.0;
        } else if (Mode.NON_ASPECT_FILL.equals(getMode())) {
            return null;
        }

        final double scale = getResultingScale(reducedSize, scaleConstraint);
        final double rfScale = reductionFactor.getScale();

        return scale / rfScale;
    }

    /**
     * @param reducedSize     Size of an image that has been halved {@literal
     *                        n} times.
     * @param scaleConstraint Scale constraint relative to the full source
     *                        image dimensions.
     * @param maxFactor       Maximum factor to return.
     * @return                Reduction factor appropriate for the instance.
     */
    public ReductionFactor getReductionFactor(final Dimension reducedSize,
                                              final ScaleConstraint scaleConstraint,
                                              final int maxFactor) {
        final double scScale = scaleConstraint.getScale();
        ReductionFactor rf = new ReductionFactor();

        if (getPercent() != null) {
            rf = ReductionFactor.forScale(getPercent() * scScale);
        } else {
            switch (getMode()) {
                case FULL:
                    rf = ReductionFactor.forScale(scScale);
                    break;
                case ASPECT_FIT_WIDTH:
                    double hvScale = getWidth() / reducedSize.width() * scScale;
                    rf = ReductionFactor.forScale(hvScale);
                    break;
                case ASPECT_FIT_HEIGHT:
                    hvScale = getHeight() / reducedSize.height() * scScale;
                    rf = ReductionFactor.forScale(hvScale);
                    break;
                case ASPECT_FIT_INSIDE:
                    double hScale = getWidth() / reducedSize.width() * scScale;
                    double vScale = getHeight() / reducedSize.height() * scScale;
                    rf = ReductionFactor.forScale(Math.min(hScale, vScale));
                    break;
            }
        }
        if (rf.factor > maxFactor) {
            rf.factor = maxFactor;
        }
        return rf;
    }

    /**
     * @param fullSize        Full source image dimensions.
     * @param scaleConstraint Scale constraint relative to the full source
     *                        image dimensions. The instance is expressed
     *                        relative to the constrained {@literal fullSize}.
     * @return                Resulting scale when the instance is applied to
     *                        the given full size; or {@literal null} if the
     *                        scale mode is {@link Mode#NON_ASPECT_FILL}.
     */
    public Double getResultingScale(Dimension fullSize,
                                    ScaleConstraint scaleConstraint) {
        if (getPercent() != null) {
            return getPercent() * scaleConstraint.getScale();
        }
        switch (getMode()) {
            case FULL:
                return scaleConstraint.getScale();
            case ASPECT_FIT_HEIGHT:
                return getHeight() / fullSize.height();
            case ASPECT_FIT_WIDTH:
                return getWidth() / fullSize.width();
            case ASPECT_FIT_INSIDE:
                return Math.min(
                        getWidth() / fullSize.width(),
                        getHeight() / fullSize.height());
            default:
                return null;
        }
    }

    /**
     * @param fullSize Full source image size.
     * @return         Resulting dimensions when the scale is applied to the
     *                 given full size.
     */
    @Override
    public Dimension getResultingSize(Dimension fullSize,
                                      ScaleConstraint scaleConstraint) {
        return getResultingSize(
                fullSize, new ReductionFactor(0), scaleConstraint);
    }

    /**
     * @param imageSize       Image whose dimensions have been halved {@link
     *                        ReductionFactor#factor} times.
     * @param reductionFactor Number of times the given dimensions have been
     *                        halved.
     * @param scaleConstraint Scale constraint relative to the full source
     *                        image dimensions.
     * @return                Resulting dimensions when the scale is applied to
     *                        the constrained view of the given full size.
     */
    public Dimension getResultingSize(Dimension imageSize,
                                      ReductionFactor reductionFactor,
                                      ScaleConstraint scaleConstraint) {
        final Dimension size = new Dimension(imageSize);
        final double rfScale = reductionFactor.getScale();
        final double scScale = scaleConstraint.getScale();

        if (getPercent() != null) {
            final double scalePct = getPercent() * (scScale / rfScale);
            size.setWidth(size.width() * scalePct);
            size.setHeight(size.height() * scalePct);
        } else {
            switch (getMode()) {
                case FULL:
                    double scalePct = scScale / rfScale;
                    size.setWidth(size.width() * scalePct);
                    size.setHeight(size.height() * scalePct);
                    break;
                case ASPECT_FIT_HEIGHT:
                    scalePct = getHeight() / size.height();
                    size.setWidth(size.width() * scalePct);
                    size.setHeight(size.height() * scalePct);
                    break;
                case ASPECT_FIT_WIDTH:
                    scalePct = getWidth() / size.width();
                    size.setWidth(size.width() * scalePct);
                    size.setHeight(size.height() * scalePct);
                    break;
                case ASPECT_FIT_INSIDE:
                    scalePct = Math.min(
                            getWidth() / size.width(),
                            getHeight() / size.height());
                    size.setWidth(size.width() * scalePct);
                    size.setHeight(size.height() * scalePct);
                    break;
                case NON_ASPECT_FILL:
                    size.setWidth(getWidth());
                    size.setHeight(getHeight());
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
                ((getPercent() != null && Math.abs(getPercent() - 1) > DELTA) ||
                        (getPercent() == null && (getHeight() != null || getWidth() != null)));
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        if (opList.getScaleConstraint().hasEffect()) {
            return true;
        }

        Dimension cropSize = fullSize;
        for (Operation op : opList) {
            if (op instanceof Crop) {
                cropSize = op.getResultingSize(
                        cropSize, opList.getScaleConstraint());
            }
        }

        switch (getMode()) {
            case FULL:
                return false;
            case ASPECT_FIT_WIDTH:
                return (Math.abs(getWidth() - cropSize.width()) > DELTA);
            case ASPECT_FIT_HEIGHT:
                return (Math.abs(getHeight() - cropSize.height()) > DELTA);
            default:
                if (getPercent() != null) {
                    return Math.abs(getPercent() - 1) > DELTA;
                }
                return (Math.abs(getWidth() - cropSize.width()) > DELTA ||
                        Math.abs(getHeight() - cropSize.height()) > DELTA);
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * @param comparedToSize
     * @param comparedToScaleConstraint
     * @return Whether the instance would effectively upscale the image it is
     *         applied to, i.e. whether the resulting image would have more
     *         pixels.
     */
    public boolean isUp(Dimension comparedToSize,
                        ScaleConstraint comparedToScaleConstraint) {
        Dimension resultingSize = getResultingSize(
                comparedToSize, comparedToScaleConstraint);
        return ((resultingSize.width() * resultingSize.height()) -
                (comparedToSize.width() * comparedToSize.height()) > DELTA);
    }

    /**
     * @param filter Resample filter to prefer.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setFilter(Filter filter) {
        checkFrozen();
        this.filter = filter;
    }

    /**
     * @param height Integer greater than 0.
     * @throws IllegalArgumentException if the given height is invalid.
     * @throws IllegalStateException if the instance is frozen.
     */
    public void setHeight(Integer height) {
        checkFrozen();
        if (height != null && height <= 0) {
            throw new IllegalArgumentException("Height must be a positive integer");
        }
        this.height = height;
    }

    /**
     * N.B.: Invoking this method also sets the instance's mode to
     * {@link Mode#ASPECT_FIT_INSIDE}.
     *
     * @param percent Double &gt; 0 and &le; 1.
     * @throws IllegalArgumentException If the given percent is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setPercent(Double percent) {
        checkFrozen();
        if (percent != null && percent <= 0) {
            throw new IllegalArgumentException("Percent must be greater than zero");
        }
        this.setMode(Mode.ASPECT_FIT_INSIDE);
        this.percent = percent;
    }

    /**
     * @param scaleMode Scale mode to set.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setMode(Mode scaleMode) {
        checkFrozen();
        this.scaleMode = scaleMode;
    }

    /**
     * @param width Integer greater than 0.
     * @throws IllegalArgumentException If the given width is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setWidth(Integer width) {
        checkFrozen();
        if (width != null && width <= 0) {
            throw new IllegalArgumentException("Width must be a positive integer");
        }
        this.width = width;
    }

    /**
     * @param fullSize        Full size of the source image on which the
     *                        operation is being applied.
     * @param scaleConstraint Scale constraint.
     * @return                Map with {@literal width} and {@literal height}
     *                        keys and integer values corresponding to the
     *                        resulting pixel size of the operation.
     */
    @Override
    public Map<String,Object> toMap(Dimension fullSize,
                                    ScaleConstraint scaleConstraint) {
        final Dimension resultingSize =
                getResultingSize(fullSize, scaleConstraint);
        final Map<String,Object> map = new HashMap<>();
        map.put("class", Scale.class.getSimpleName());
        map.put("width", resultingSize.intWidth());
        map.put("height", resultingSize.intHeight());
        return Collections.unmodifiableMap(map);
    }

    /**
     * <p>Returns a string representation of the instance, guaranteed to
     * uniquely represent the instance. The format is:</p>
     *
     * <dl>
     *     <dt>No-op</dt>
     *     <dd>{@literal none}</dd>
     *     <dt>Percent</dt>
     *     <dd>{@literal nnn%(,filter)}</dd>
     *     <dt>Aspect-fit-inside</dt>
     *     <dd>{@literal !w,h(,filter)}</dd>
     *     <dt>Other</dt>
     *     <dd>{@literal w,h(,filter)}</dd>
     * </dl>
     *
     * @return String representation of the instance.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        if (!hasEffect()) {
            builder.append("none");
        } else if (getPercent() != null) {
            builder.append(StringUtils.removeTrailingZeroes(getPercent() * 100));
            builder.append("%");
        } else {
            if (Mode.ASPECT_FIT_INSIDE.equals(getMode())) {
                builder.append("!");
            }
            if (getWidth() != null && getWidth() > 0) {
                builder.append(getWidth());
            }
            builder.append(",");
            if (getHeight() != null && getHeight() > 0) {
                builder.append(getHeight());
            }
        }
        if (getFilter() != null) {
            builder.append(",");
            builder.append(getFilter().name().toLowerCase());
        }
        return builder.toString();
    }

}
