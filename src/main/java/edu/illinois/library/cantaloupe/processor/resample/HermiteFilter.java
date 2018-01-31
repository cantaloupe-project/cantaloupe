/*
 * Copyright 2013, Morten Nobel-Joergensen
 *
 * License: The BSD 3-Clause License
 * http://opensource.org/licenses/BSD-3-Clause
 */
package edu.illinois.library.cantaloupe.processor.resample;

/**
 * Hermite resampling filter.
 */
class HermiteFilter implements ResampleFilter {

    public float getSamplingRadius() {
        return 1.0f;
    }

    public float apply(float value) {
        if (value < 0.0f) {
            value = -value;
        }
        if (value < 1.0f) {
            return (2.0f * value - 3.0f) * value * value + 1.0f;
        } else {
            return 0.0f;
        }
    }

    public String getName() {
        return "Hermite";
    }

}