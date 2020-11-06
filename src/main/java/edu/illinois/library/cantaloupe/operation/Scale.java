package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.processor.resample.ResampleFilter;
import edu.illinois.library.cantaloupe.processor.resample.ResampleFilters;

import java.util.Map;

/**
 * Encapsulates a scaling operation. Subclasses implement absolute
 * (pixel-based) or relative (percentage-based) behavior.
 */
public abstract class Scale implements Operation {

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

        private final String name;
        private final ResampleFilter resampleFilter;

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
            return Map.of(
                    "class", Filter.class.getSimpleName(),
                    "name", getName());
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

    static final double DELTA = 0.00000001;

    private Filter filter;
    private boolean isFrozen;

    void checkFrozen() {
        if (isFrozen) {
            throw new IllegalStateException("Instance is frozen.");
        }
    }

    public void freeze() {
        isFrozen = true;
    }

    /**
     * @return Resample filter to prefer. May be {@code null}.
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * <p>Finds the required additional differential scaling of an image at a
     * reduced resolution level.</p>
     *
     * <p>For example, the client has requested a scale of 45%, and the reader
     * has returned an image with a {@link ReductionFactor#factor} of
     * {@literal 1} (50%). The amount that that intermediate image must be
     * further downscaled is returned.</p>
     *
     * @param reducedSize     Image dimensions, which have been reduced {@link
     *                        ReductionFactor#factor} times.
     * @param reductionFactor Reduction factor that has reduced a source image
     *                        to {@code reducedSize}.
     * @param scaleConstraint Scale constraint relative to the full source
     *                        image dimensions.
     * @return                Two-element array containing the X and Y scales
     *                        yet to be applied to an intermediate image of the
     *                        given reduced size with the given reduction
     *                        factor. {@literal 1} indicates no scaling needed.
     */
    public double[] getDifferentialScales(final Dimension reducedSize,
                                          final ReductionFactor reductionFactor,
                                          final ScaleConstraint scaleConstraint) {
        final double[] scales = getResultingScales(reducedSize, scaleConstraint);
        final double rfScale  = reductionFactor.getScale();
        return new double[] {
                scales[0] / rfScale,
                scales[1] / rfScale
        };
    }

    /**
     * @param reducedSize     Size of an image that has been halved {@literal
     *                        n} times.
     * @param scaleConstraint Scale constraint relative to the full source
     *                        image dimensions.
     * @param maxFactor       Maximum factor to return.
     * @return                Reduction factor appropriate for the instance.
     */
    public abstract ReductionFactor getReductionFactor(Dimension reducedSize,
                                                       ScaleConstraint scaleConstraint,
                                                       int maxFactor);

    /**
     * @param fullSize        Full source image dimensions.
     * @param scaleConstraint Scale constraint relative to the full source
     *                        image dimensions. The instance is expressed
     *                        relative to the constrained {@code fullSize}.
     * @return                Two-element array containing the resulting X and
     *                        Y scales when the instance is applied to the
     *                        given full size.
     */
    public abstract double[] getResultingScales(Dimension fullSize,
                                                ScaleConstraint scaleConstraint);

    /**
     * @param fullSize Full source image size.
     * @return         Resulting dimensions when the scale is applied to the
     *                 given full size.
     */
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
    public abstract Dimension getResultingSize(Dimension imageSize,
                                               ReductionFactor reductionFactor,
                                               ScaleConstraint scaleConstraint);

    /**
     * @return Whether the instance would effectively upscale the Y axis of the
     *         image it is applied to.
     */
    public boolean isHeightUp(Dimension comparedToSize,
                              ScaleConstraint comparedToScaleConstraint) {
        Dimension resultingSize = getResultingSize(
                comparedToSize, comparedToScaleConstraint);
        return (resultingSize.height() - comparedToSize.height() > DELTA);
    }

    /**
     * @return Whether the instance would effectively upscale the image it is
     *         applied to on both axes.
     */
    public boolean isUp(Dimension comparedToSize,
                        ScaleConstraint comparedToScaleConstraint) {
        Dimension resultingSize = getResultingSize(
                comparedToSize, comparedToScaleConstraint);
        return ((resultingSize.width() * resultingSize.height()) -
                (comparedToSize.width() * comparedToSize.height()) > DELTA);
    }

    /**
     * @return Whether the instance would effectively upscale the X axis of the
     *         image it is applied to.
     */
    public boolean isWidthUp(Dimension comparedToSize,
                             ScaleConstraint comparedToScaleConstraint) {
        Dimension resultingSize = getResultingSize(
                comparedToSize, comparedToScaleConstraint);
        return (resultingSize.width() - comparedToSize.width() > DELTA);
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
     * @param fullSize        Full size of the source image on which the
     *                        operation is being applied.
     * @param scaleConstraint Scale constraint.
     * @return                Map with {@code width} and {@code height}
     *                        keys and integer values corresponding to the
     *                        resulting pixel size of the operation.
     */
    public Map<String,Object> toMap(Dimension fullSize,
                                    ScaleConstraint scaleConstraint) {
        final Dimension resultingSize =
                getResultingSize(fullSize, scaleConstraint);
        return Map.of(
                "class", getClass().getSimpleName(),
                "width", resultingSize.intWidth(),
                "height", resultingSize.intHeight());
    }

}
