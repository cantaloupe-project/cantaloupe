/*
 * Copyright 2013, Morten Nobel-Joergensen
 *
 * License: The BSD 3-Clause License
 * http://opensource.org/licenses/BSD-3-Clause
 */
package edu.illinois.library.cantaloupe.processor.resample;

import java.awt.Dimension;

public class DimensionConstraint {

    /**
     * Used when the destination size is fixed. This may not preserve the image
     * aspect radio.
     *
     * @param width  Destination dimension width.
     * @param height Destination dimension height.
     * @return Destination dimension area.
     */
    public static DimensionConstraint createAbsolutionDimension(final int width,
                                                                final int height) {
        assert width > 0 && height > 0 : "Dimension must be a positive integer";

        return new DimensionConstraint() {
            @Override
            public Dimension getDimension(Dimension dimension) {
                return new Dimension(width, height);
            }
        };
    }

    private DimensionConstraint() {}

    /**
     * <p>Returns the given dimension.</p>
     *
     * <p>Can be overridden to create user-defined behavior.</p>
     *
     * @param dimension Dimension of the source image.
     * @return Dimension of the scaled image.
     */
    public Dimension getDimension(Dimension dimension) {
        return dimension;
    }

}
