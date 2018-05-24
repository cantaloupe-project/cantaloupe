package edu.illinois.library.cantaloupe.operation;

/**
 * Represents a scale-reduction multiple with a corresponding scale of
 * <code>(1/2)^rf</code>.
 *
 * For example, a factor of 0 represents full scale; 1 represents 50% reduction;
 * 2 represents 75% reduction; etc.
 */
public class ReductionFactor {

    public int factor = 0;

    /**
     * Factory method.
     *
     * @param scalePercent Scale percentage greater than 0
     * @return Instance corresponding to the given scale.
     */
    public static ReductionFactor forScale(final double scalePercent) {
        return ReductionFactor.forScale(scalePercent, 0);
    }

    /**
     * Factory method.
     *
     * @param scalePercent Scale percentage greater than 0
     * @return Instance corresponding to the given scale.
     */
    public static ReductionFactor forScale(final double scalePercent,
                                           final int maxFactor) {
        return ReductionFactor.forScale(scalePercent, 0, maxFactor);
    }

    /**
     * Factory method.
     *
     * @param scalePercent Scale percentage greater than 0
     * @param minFactor    Minimum factor to return.
     * @param maxFactor    Maximum factor to return.
     * @return             Instance corresponding to the given scale.
     */
    public static ReductionFactor forScale(final double scalePercent,
                                           int minFactor, int maxFactor) {
        if (minFactor == 0) {
            minFactor = -999;
        }
        if (maxFactor == 0) {
            maxFactor = 999;
        }
        int factor = 0;

        if (scalePercent < 1.0) {
            double nextPct = 0.5;
            while (scalePercent <= nextPct && factor < maxFactor) {
                nextPct /= 2.0;
                factor++;
            }
        } else if (scalePercent > 1.0) {
            double nextPct = 2.0;
            while (scalePercent >= nextPct && factor > minFactor) {
                nextPct *= 2.0;
                factor--;
            }
        }
        if (factor < minFactor) {
            factor = minFactor;
        }
        if (factor > maxFactor) {
            factor = maxFactor;
        }
        return new ReductionFactor(factor);
    }

    /**
     * No-op constructor.
     */
    public ReductionFactor() {}

    public ReductionFactor(int factor) {
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
        return Float.hashCode(factor);
    }

    @Override
    public String toString() {
        return factor + "";
    }

}
