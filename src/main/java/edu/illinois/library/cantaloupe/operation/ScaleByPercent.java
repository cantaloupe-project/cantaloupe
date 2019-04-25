package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.util.StringUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * N.B.: {@link #getPercent()} should not be used when figuring out how to
 * apply an instance to an image. For that, {@link #getResultingSize} and
 * {@link #getResultingScales} should be used instead.
 */
public class ScaleByPercent extends Scale implements Operation {

    private double percent = 1;

    /**
     * No-op constructor for a 100% instance.
     */
    public ScaleByPercent() {
    }

    /**
     * Percent-based constructor.
     *
     * @param percent Value between {@literal 0} and {@literal 1} to represent
     *                a downscale, or above {@literal 1} to represent an
     *                upscale.
     */
    public ScaleByPercent(double percent) {
        setPercent(percent);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ScaleByPercent) {
            ScaleByPercent other = (ScaleByPercent) obj;
            return Math.abs(getPercent() - other.getPercent()) < DELTA &&
                    Objects.equals(other.getFilter(), getFilter());
        }
        return super.equals(obj);
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
     * @return Double from 0 to 1. May be null.
     */
    public double getPercent() {
        return percent;
    }

    @Override
    public ReductionFactor getReductionFactor(final Dimension reducedSize,
                                              final ScaleConstraint scaleConstraint,
                                              final int maxFactor) {
        final double scScale = scaleConstraint.getRational().doubleValue();
        ReductionFactor rf = ReductionFactor.forScale(getPercent() * scScale);

        if (rf.factor > maxFactor) {
            rf.factor = maxFactor;
        }
        return rf;
    }

    @Override
    public double[] getResultingScales(Dimension fullSize,
                                       ScaleConstraint scaleConstraint) {
        final double[] result = new double[2];
        result[0] = result[1] = getPercent() *
                scaleConstraint.getRational().doubleValue();
        return result;
    }

    @Override
    public Dimension getResultingSize(Dimension imageSize,
                                      ReductionFactor reductionFactor,
                                      ScaleConstraint scaleConstraint) {
        final double rfScale  = reductionFactor.getScale();
        final double scScale  = scaleConstraint.getRational().doubleValue();
        final double scalePct = getPercent() * (scScale / rfScale);
        final Dimension size  = new Dimension(imageSize);
        size.setWidth(size.width() * scalePct);
        size.setHeight(size.height() * scalePct);
        return size;
    }

    @Override
    public boolean hasEffect() {
        return Math.abs(getPercent() - 1) > DELTA;
    }

    @Override
    public boolean hasEffect(Dimension fullSize, OperationList opList) {
        if (opList.getScaleConstraint().hasEffect()) {
            return true;
        }
        return Math.abs(getPercent() - 1) > DELTA;
    }

    @Override
    public int hashCode() {
        final int[] codes = new int[2];
        codes[0] = Double.hashCode(getPercent());
        if (getFilter() != null) {
            codes[1] = getFilter().hashCode();
        }
        return Arrays.hashCode(codes);
    }

    /**
     * @param percent Double &gt; 0 and &le; 1.
     * @throws IllegalArgumentException If the given percent is invalid.
     * @throws IllegalStateException If the instance is frozen.
     */
    public void setPercent(double percent) {
        checkFrozen();
        if (percent < DELTA) {
            throw new IllegalArgumentException("Percent must be greater than zero");
        }
        this.percent = percent;
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
     * </dl>
     *
     * @return String representation of the instance.
     */
    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append(StringUtils.removeTrailingZeroes(getPercent() * 100));
        builder.append("%");
        if (getFilter() != null) {
            builder.append(",");
            builder.append(getFilter().name().toLowerCase());
        }
        return builder.toString();
    }

}
