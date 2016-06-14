/*
 * Copyright 2013, Morten Nobel-Joergensen
 *
 * License: The BSD 3-Clause License
 * http://opensource.org/licenses/BSD-3-Clause
 */
package com.mortennobel.imagescaling;

/**
 * The Mitchell resample filter.
 */
final class MitchellFilter implements ResampleFilter
{
	private static final float B = 1.0f / 3.0f;
	private static final float C = 1.0f / 3.0f;

	public float getSamplingRadius() {
		return 2.0f;
	}

	public final float apply(float value)
	{
		if (value < 0.0f)
		{
			value = -value;
		}
		float tt = value * value;
		if (value < 1.0f)
		{
			value = (((12.0f - 9.0f * B - 6.0f * C) * (value * tt))
			+ ((-18.0f + 12.0f * B + 6.0f * C) * tt)
			+ (6.0f - 2f * B));
			return value / 6.0f;
		}
		else
		if (value < 2.0f)
		{
			value = (((-1.0f * B - 6.0f * C) * (value * tt))
			+ ((6.0f * B + 30.0f * C) * tt)
			+ ((-12.0f * B - 48.0f * C) * value)
			+ (8.0f * B + 24 * C));
			return value / 6.0f;
		}
		else
		{
			return 0.0f;
		}
	}

	public String getName() {
		return "BSpline";
	}
}

