package edu.illinois.library.cantaloupe.processor;

/**
 * Represents a scale-reduction multiple with a corresponding scale of 1/(2^rf).
 *
 * For example, a factor of 0 represents full scale; 1 represents 50% reduction;
 * 2 represents 75% reduction; etc.
 */
class ReductionFactor {

    public int factor = 0;

    /**
     * Factory method.
     *
     * @param scalePercent Scale percentage between 0 and 1
     * @param maxFactor Maximum factor to return.
     * @return Instance corresponding to the given scale.
     */
    public static ReductionFactor forScale(final double scalePercent,
                                           int maxFactor) {
        if (maxFactor == 0) {
            maxFactor = 9999;
        }
        short factor = 0;
        double nextPct = 0.5f;
        while (scalePercent <= nextPct && factor < maxFactor) {
            nextPct /= 2.0f;
            factor++;
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
     * @return Scale corresponding to the instance (1/(2^rf)).
     */
    public double getScale() {
        double scale = 1f;
        for (int i = 0; i < this.factor; i++) {
            scale /= 2;
        }
        return scale;
    }

}
