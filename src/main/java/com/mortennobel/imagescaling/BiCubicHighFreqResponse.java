/*
 * Copyright 2013, Morten Nobel-Joergensen
 *
 * License: The BSD 3-Clause License
 * http://opensource.org/licenses/BSD-3-Clause
 */
package com.mortennobel.imagescaling;

/**
 * @author Heinz Doerr
 */
final class BiCubicHighFreqResponse extends BiCubicFilter {

	public BiCubicHighFreqResponse() {
		super(-1.f);
	}

	@Override
	public String getName() {
		return "BiCubicHighFreqResponse";
	}

}