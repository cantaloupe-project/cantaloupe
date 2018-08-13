package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.resource.iiif.ImageInfoUtil;

import java.awt.Dimension;

final class ImageInfoFactory {

    private static final int MIN_SIZE = 64;

    /**
     * Will be used if {@link Key#IIIF_MIN_TILE_SIZE} is not set.
     */
    private static final int DEFAULT_MIN_TILE_SIZE = 512;

    ImageInfo newImageInfo(final String imageURI,
                           final Processor processor,
                           final Info.Image infoImage,
                           final int numResolutions,
                           ScaleConstraint scaleConstraint) {
        if (scaleConstraint == null) {
            scaleConstraint = new ScaleConstraint(1, 1);
        }
        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                processor.getSupportedFeatures(),
                processor.getSupportedIIIF1Qualities(),
                processor.getAvailableOutputFormats());

        final int minTileSize = Configuration.getInstance().
                getInt(Key.IIIF_MIN_TILE_SIZE, DEFAULT_MIN_TILE_SIZE);

        // Find a tile width and height. If the image is not tiled,
        // calculate a tile size close to MIN_TILE_SIZE_CONFIG_KEY pixels.
        // Otherwise, use the smallest multiple of the tile size above
        // MIN_TILE_SIZE_CONFIG_KEY of image resolution 0.
        final Dimension virtualSize = infoImage.getOrientationSize();
        virtualSize.width *= scaleConstraint.getScale();
        virtualSize.height *= scaleConstraint.getScale();
        Dimension virtualTileSize = infoImage.getOrientationTileSize();
        virtualTileSize.width *= scaleConstraint.getScale();
        virtualTileSize.height *= scaleConstraint.getScale();

        if (numResolutions > 0) {
            if (!virtualTileSize.equals(virtualSize)) {
                virtualTileSize = ImageInfoUtil.smallestTileSize(
                        virtualSize, virtualTileSize, minTileSize);
            }
        }

        // Create an Info instance, which will eventually be serialized
        // to JSON and sent as the response body.
        final ImageInfo imageInfo = new ImageInfo();
        imageInfo.id = imageURI;
        imageInfo.width = virtualSize.width;
        imageInfo.height = virtualSize.height;
        imageInfo.profile = complianceLevel.getUri();
        imageInfo.tileWidth = virtualTileSize.width;
        imageInfo.tileHeight = virtualTileSize.height;

        // scale factors
        int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(virtualSize, MIN_SIZE);
        for (int i = 0; i <= maxReductionFactor; i++) {
            imageInfo.scaleFactors.add((int) Math.pow(2, i));
        }

        // formats
        for (Format format : processor.getAvailableOutputFormats()) {
            imageInfo.formats.add(format.getPreferredExtension());
        }

        // qualities
        for (Quality quality : processor.getSupportedIIIF1Qualities()) {
            imageInfo.qualities.add(quality.toString().toLowerCase());
        }

        return imageInfo;
    }

}
