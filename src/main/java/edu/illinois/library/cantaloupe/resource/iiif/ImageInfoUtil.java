package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.image.Dimension;

public class ImageInfoUtil {

    /**
     * @param fullSize     Full size of the source image.
     * @param minDimension Minimum allowed dimension.
     * @return             Maximum reduction factor to be able to fit above the
     *                     minimum allowed dimension.
     */
    public static int maxReductionFactor(final Dimension fullSize,
                                         final int minDimension) {
        if (minDimension <= 0) {
            throw new IllegalArgumentException("minDimension must be a positive number.");
        }
        double nextDimension = Math.min(fullSize.width(), fullSize.height());
        int factor = -1;
        for (int i = 0; i < 9999; i++) {
            nextDimension /= 2.0;
            if (nextDimension < minDimension) {
                factor = i;
                break;
            }
        }
        return factor;
    }

    /**
     * @param fullSize Full size of the source image.
     * @param maxPixels Maximum allowed number of pixels.
     * @return Minimum reduction factor to be able to fit below the maximum
     *         allowed number of pixels.
     */
    public static int minReductionFactor(final Dimension fullSize,
                                         final int maxPixels) {
        if (maxPixels <= 0) {
            throw new IllegalArgumentException("maxPixels must be a positive number.");
        }
        int factor = 0;
        Dimension nextSize = new Dimension(fullSize);

        while (nextSize.width() * nextSize.height() > maxPixels) {
            nextSize.setWidth(nextSize.width() / 2.0);
            nextSize.setHeight(nextSize.height() / 2.0);
            factor++;
        }
        return factor;
    }

    /**
     * <p>Given full-size image dimensions, calculates the smallest tile size
     * above the given minimum dimension based on the series {@literal
     * 1/(2^n)}.</p>
     *
     * <p>N.B.: Dimensions should be rounded up for display, to prevent clients
     * from requesting narrow edge tiles.</p>
     *
     * @param fullSize     Full size of the source image.
     * @param minDimension Minimum allowed dimension.
     * @return             Tile size.
     */
    public static Dimension smallestTileSize(final Dimension fullSize,
                                             final int minDimension) {
        Dimension size = new Dimension(fullSize);
        Dimension nextSize = new Dimension(size);
        while (nextSize.width() >= minDimension &&
                nextSize.height() >= minDimension) {
            size = new Dimension(nextSize);
            nextSize.setWidth(nextSize.width() / 2.0);
            nextSize.setHeight(nextSize.height() / 2.0);
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
     * @param fullSize       Size of the full source image.
     * @param nativeTileSize Size of the source image's tiles.
     * @param minDimension   Minimum allowed dimension.
     * @return Tile size.
     */
    public static Dimension smallestTileSize(final Dimension fullSize,
                                             Dimension nativeTileSize,
                                             final int minDimension) {
        final double minWidth = Math.min(minDimension, fullSize.width());
        final double minHeight = Math.min(minDimension, fullSize.height());
        if (nativeTileSize == null) {
            nativeTileSize = new Dimension(fullSize);
        }
        final Dimension tileSize = new Dimension(nativeTileSize);
        while (tileSize.width() < minWidth || tileSize.height() < minHeight) {
            tileSize.setWidth(tileSize.width() * 2);
            tileSize.setHeight(tileSize.height() * 2);
        }
        tileSize.setWidth(Math.min(fullSize.width(), tileSize.width()));
        tileSize.setHeight(Math.min(fullSize.height(), tileSize.height()));
        return tileSize;
    }

}
