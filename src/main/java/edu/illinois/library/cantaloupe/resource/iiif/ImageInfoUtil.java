package edu.illinois.library.cantaloupe.resource.iiif;

import java.awt.Dimension;

public class ImageInfoUtil {

    /**
     * @param fullSize Full size of the source image.
     * @param minDimension Minimum allowed dimension.
     * @return Maximum reduction factor to be able to fit above the minimum
     *         allowed dimension.
     */
    public static int maxReductionFactor(final Dimension fullSize,
                                         final int minDimension) {
        if (minDimension <= 0) {
            throw new IllegalArgumentException("minDimension must be a positive number.");
        }
        int nextDimension = Math.min(fullSize.width, fullSize.height);
        int factor = -1;
        for (int i = 0; i < 9999; i++) {
            nextDimension /= 2f;
            if (nextDimension < minDimension) {
                factor = i;
                break;
            }
        }
        return factor;
    }

    }

    /**
     * Given full-size image dimensions, calculates the smallest tile size
     * above the given minimum dimension based on the series 1/(2^n).
     *
     * @param fullSize Full size of the source image.
     * @param minDimension Minimum allowed dimension.
     * @return Tile size
     */
    public static Dimension smallestTileSize(final Dimension fullSize,
                                             final int minDimension) {
        Dimension size = (Dimension) fullSize.clone();
        Dimension nextSize = (Dimension) size.clone();
        while (nextSize.width >= minDimension && nextSize.height >= minDimension) {
            size = (Dimension) nextSize.clone();
            // Round up fractional pixels to prevent narrow edge tiles.
            nextSize.width = (int) Math.ceil(nextSize.width / 2f);
            nextSize.height = (int) Math.ceil(nextSize.height / 2f);
        }
        return size;
    }

    /**
     * <p>Given a native tile size, calculates the smallest multiple above the
     * given minimum dimension.</p>
     *
     * <p>If the minimum dimension is greater than the full size dimension, the
     * full size dimensions will be used as the minimum dimensions.</p>
     *
     * @param fullSize Size of the full source image.
     * @param nativeTileSize Size of the source image's tiles.
     * @param minDimension Minimum allowed dimension.
     * @return Tile size
     */
    public static Dimension smallestTileSize(final Dimension fullSize,
                                             Dimension nativeTileSize,
                                             final int minDimension) {
        final int minWidth = Math.min(minDimension, fullSize.width);
        final int minHeight = Math.min(minDimension, fullSize.height);
        if (nativeTileSize == null) {
            nativeTileSize = (Dimension) fullSize.clone();
        }
        final Dimension tileSize = (Dimension) nativeTileSize.clone();
        while (tileSize.width < minWidth || tileSize.height < minHeight) {
            tileSize.width *= 2;
            tileSize.height *= 2;
        }
        tileSize.width = Math.min(fullSize.width, tileSize.width);
        tileSize.height = Math.min(fullSize.height, tileSize.height);
        return tileSize;
    }

}
