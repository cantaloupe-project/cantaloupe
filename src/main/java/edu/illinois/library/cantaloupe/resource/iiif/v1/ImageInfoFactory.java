package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.resource.iiif.ImageInfoUtil;

import java.awt.Dimension;

abstract class ImageInfoFactory {

    public static final String MIN_TILE_SIZE_CONFIG_KEY =
            "endpoint.iiif.min_tile_size";

    /** Will be used to calculate a maximum scale factor. */
    private static final int MIN_SIZE = 64;

    public static ImageInfo newImageInfo(final String imageUri,
                                         final Processor processor,
                                         final edu.illinois.library.cantaloupe.processor.ImageInfo cacheInfo)
            throws ProcessorException {
        final Dimension fullSize = cacheInfo.getSize();
        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                processor.getSupportedFeatures(),
                processor.getSupportedIiif1_1Qualities(),
                processor.getAvailableOutputFormats());

        final int minTileSize = Configuration.getInstance().
                getInt(MIN_TILE_SIZE_CONFIG_KEY, 1024);

        // Find a tile width and height. If the image is not tiled,
        // calculate a tile size close to MIN_TILE_SIZE_CONFIG_KEY pixels.
        // Otherwise, use the smallest multiple of the tile size above
        // MIN_TILE_SIZE_CONFIG_KEY of image resolution 0.
        Dimension tileSize =
                ImageInfoUtil.smallestTileSize(fullSize, minTileSize);

        if (cacheInfo.getImages().size() > 0) {
            edu.illinois.library.cantaloupe.processor.ImageInfo.Image firstImage =
                    cacheInfo.getImages().get(0);
            if (firstImage.tileWidth != fullSize.width ||
                    firstImage.tileHeight != fullSize.height) {
                tileSize = ImageInfoUtil.
                        smallestTileSize(fullSize, firstImage.getTileSize(),
                                minTileSize);
            }
        }

        // Create an ImageInfo instance, which will eventually be serialized
        // to JSON and sent as the response body.
        final ImageInfo imageInfo = new ImageInfo();
        imageInfo.id = imageUri;
        imageInfo.width = fullSize.width;
        imageInfo.height = fullSize.height;
        imageInfo.profile = complianceLevel.getUri();
        imageInfo.tileWidth = tileSize.width;
        imageInfo.tileHeight = tileSize.height;

        // scale factors
        int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(fullSize, MIN_SIZE);
        for (int i = 0; i <= maxReductionFactor; i++) {
            imageInfo.scaleFactors.add((int) Math.pow(2, i));
        }

        // formats
        for (Format format : processor.getAvailableOutputFormats()) {
            imageInfo.formats.add(format.getPreferredExtension());
        }

        // qualities
        for (Quality quality : processor.getSupportedIiif1_1Qualities()) {
            imageInfo.qualities.add(quality.toString().toLowerCase());
        }

        return imageInfo;
    }

}
