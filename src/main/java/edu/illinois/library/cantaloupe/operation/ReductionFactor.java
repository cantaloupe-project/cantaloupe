package edu.illinois.library.cantaloupe.operation;

/**
 * Represents a scale-reduction multiple with a corresponding scale of
 * {@literal (1/2)^rf}.
 *
 * For example, a factor of 0 represents full scale; 1 represents 50% reduction;
 * 2 represents 75% reduction; etc.
 */
public final class ReductionFactor {

    /**
     * Set to a value that will allow one pixel of leeway in an image up to
     * 99999x99999.
     */
    public static final double DEFAULT_TOLERANCE = 0.00001;

    public int factor = 0;

    /**
     * Factory method. Uses {@link #DEFAULT_TOLERANCE}.
     *
     * @param scalePercent Scale percentage.
     * @return             Instance corresponding to the given scale.
     */
    public static ReductionFactor forScale(final double scalePercent) {
        return forScale(scalePercent, DEFAULT_TOLERANCE);
    }

    /**
     * Factory method.
     *
     * @param scalePercent Scale percentage.
     * @param tolerance    Leeway that could allow a larger factor to be
     *                     selected.
     * @return             Instance corresponding to the given scale. If the
     *                     given scale is greater than 1, the factor will be
     *                     {@literal 0}.
     */
    public static ReductionFactor forScale(final double scalePercent,
                                           final double tolerance) {
        int factor = 0;

        if (scalePercent < 1.0) {
            final int maxFactor = 99;
            double nextPct = 0.5;
            while (scalePercent <= nextPct + tolerance & factor < maxFactor) {
                nextPct /= 2.0;
                factor++;
            }
        }
        return new ReductionFactor(factor);
    }

    /**
     * No-op constructor.
     */
    public ReductionFactor() {}

    public ReductionFactor(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException(
                    "Factor must be greater than or equal to 0");
        }
        this.factor = factor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ReductionFactor) {
            return ((ReductionFactor) obj).factor == factor;
        }
        return super.equals(obj);
    }

    /**
     * @return Scale corresponding to the instance.
     */
    public double getScale() {
        return Math.pow(0.5, this.factor);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(factor);
    }

    @Override
    public String toString() {
        return factor + "";
    }

}
