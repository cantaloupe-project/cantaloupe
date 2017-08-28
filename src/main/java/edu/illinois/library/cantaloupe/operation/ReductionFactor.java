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
        return ReductionFactor.forScale(scalePercent, maxFactor, 0);
    }

    /**
     * Factory method.
     *
     * @param scalePercent Scale percentage greater than 0
     * @param maxFactor Maximum factor to return.
     * @return Instance corresponding to the given scale.
     */
    public static ReductionFactor forScale(final double scalePercent,
                                           int maxFactor, int minFactor) {
        if (maxFactor == 0) {
            maxFactor = 9999;
        }
        if (minFactor == 0) {
            minFactor = -9999;
        }
        int factor = 0;

        if (scalePercent < 1) {
            double nextPct = 0.5f;
            while (scalePercent <= nextPct && factor < maxFactor) {
                nextPct /= 2f;
                factor++;
            }
        } else if (scalePercent > 1) {
            double nextPct = 2f;
            while (scalePercent >= nextPct && factor > minFactor) {
                nextPct *= 2f;
                factor--;
            }
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
        if (obj instanceof ReductionFactor) {
            return ((ReductionFactor) obj).factor == factor;
        }
        return super.equals(obj);
    }

    /**
     * @return Scale corresponding to the instance.
     */
    public double getScale() {
        return Math.pow(0.5f, this.factor);
    }

    @Override
    public int hashCode() {
        return Float.hashCode(factor);
    }

}
