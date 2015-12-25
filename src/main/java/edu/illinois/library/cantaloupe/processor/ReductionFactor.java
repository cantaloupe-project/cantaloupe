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

}
