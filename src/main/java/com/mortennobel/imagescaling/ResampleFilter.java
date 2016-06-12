/*
 * Copyright 2013, Morten Nobel-Joergensen
 *
 * License: The BSD 3-Clause License
 * http://opensource.org/licenses/BSD-3-Clause
 */
package com.mortennobel.imagescaling;

public interface ResampleFilter {
    public float getSamplingRadius();

    float apply(float v);

	public abstract String getName();
}
