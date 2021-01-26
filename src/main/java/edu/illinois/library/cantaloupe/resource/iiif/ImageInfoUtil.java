package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.image.Dimension;

public final class ImageInfoUtil {

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
     * @param fullSize  Full size of the source image.
     * @param maxPixels Maximum allowed number of pixels.
     * @return          Minimum reduction factor to be able to fit below the
     *                  maximum allowed number of pixels.
     */
    public static int minReductionFactor(final Dimension fullSize,
                                         final long maxPixels) {
        if (maxPixels <= 0) {
            throw new IllegalArgumentException("maxPixels must be a positive number.");
        }
        int factor = 0;
        Dimension nextSize = new Dimension(fullSize);

        while (nextSize.area() > maxPixels) {
            nextSize.scale(0.5);
            factor++;
        }
        return factor;
    }

    /**
     * Calculates an optimal information tile size based on a given physical
     * image tile size.
     *
     * @param fullSize         Size of the full source image.
     * @param physicalTileSize Size of the source image's tiles. If the source
     *                         image is not natively tiled, this should be
     *                         equal to {@literal fullSize}.
     * @param minSize          Minimum allowed dimension.
     * @return                 Information tile size.
     */
    public static Dimension getTileSize(final Dimension fullSize,
                                        final Dimension physicalTileSize,
                                        final int minSize) {
        final double minW = Math.min(minSize, fullSize.width());
        final double minH = Math.min(minSize, fullSize.height());

        final Dimension infoTileSize = new Dimension(physicalTileSize);

        // If true, the image is not natively tiled. Use minSize as the tile
        // size.
        if (physicalTileSize.intWidth() == fullSize.intWidth()) {
            infoTileSize.setWidth(minW);
            infoTileSize.setHeight(minH);
            return infoTileSize;
        }

        while (infoTileSize.width() < minW || infoTileSize.height() < minH) {
            infoTileSize.scale(2);
        }

        // Limit tile dimensions to the full image size.
        infoTileSize.setWidth(Math.min(fullSize.width(), infoTileSize.width()));
        infoTileSize.setHeight(Math.min(fullSize.height(), infoTileSize.height()));
        return infoTileSize;
    }

    private ImageInfoUtil() {}

}
