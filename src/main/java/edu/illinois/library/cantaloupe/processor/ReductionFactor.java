package edu.illinois.library.cantaloupe.processor;

/**
 * Represents a scale-reduction multiple with a corresponding scale of 1/(2^rf).
 *
 * For example, a factor of 0 represents full scale; 1 represents 50% reduction;
 * 2 represents 75% reduction; etc.
 */
class ReductionFactor {

    public int factor = 0;

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
