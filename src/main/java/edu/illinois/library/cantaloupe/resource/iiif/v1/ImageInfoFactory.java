package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.resource.iiif.ImageInfoUtil;

import java.util.Set;

final class ImageInfoFactory {

    private static final int MIN_SIZE = 64;

    /**
     * Will be used if {@link Key#IIIF_MIN_TILE_SIZE} is not set.
     */
    private static final int DEFAULT_MIN_TILE_SIZE = 512;

    ImageInfo newImageInfo(final String imageURI,
                           final Set<Format> availableOutputFormats,
                           final Info info,
                           final int imageIndex,
                           ScaleConstraint scaleConstraint) {
        if (scaleConstraint == null) {
            scaleConstraint = new ScaleConstraint(1, 1);
        }
        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                availableOutputFormats);

        final int minTileSize = Configuration.getInstance().
                getInt(Key.IIIF_MIN_TILE_SIZE, DEFAULT_MIN_TILE_SIZE);

        // Find a tile width and height. If the image is not tiled,
        // calculate a tile size close to Key.IIIF_MIN_TILE_SIZE pixels.
        // Otherwise, use the smallest multiple of the tile size above
        // Key.IIIF_MIN_TILE_SIZE of image resolution 0.
        final Metadata metadata = info.getMetadata();
        final Orientation orientation = (metadata != null) ?
                metadata.getOrientation() : Orientation.ROTATE_0;
        final Dimension virtualSize = orientation.adjustedSize(info.getSize());
        final double scScale = scaleConstraint.getRational().doubleValue();
        virtualSize.scale(scScale);
        Dimension virtualTileSize = orientation.adjustedSize(
                info.getImages().get(imageIndex).getTileSize());
        virtualTileSize.scale(scScale);

        if (info.getNumResolutions() > 0) {
            if (!virtualTileSize.equals(virtualSize)) {
                virtualTileSize = ImageInfoUtil.getTileSize(
                        virtualSize, virtualTileSize, minTileSize);
            }
        }

        // Create an Info instance, which will eventually be serialized
        // to JSON and sent as the response body.
        final ImageInfo imageInfo = new ImageInfo();
        imageInfo.id              = imageURI;
        imageInfo.width           = virtualSize.intWidth();
        imageInfo.height          = virtualSize.intHeight();
        imageInfo.profile         = complianceLevel.getUri();
        // Round up to prevent clients from requesting narrow edge tiles.
        imageInfo.tileWidth       = (int) Math.ceil(virtualTileSize.width());
        imageInfo.tileHeight      = (int) Math.ceil(virtualTileSize.height());

        // scale factors
        int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(virtualSize, MIN_SIZE);
        for (int i = 0; i <= maxReductionFactor; i++) {
            imageInfo.scaleFactors.add((int) Math.pow(2, i));
        }

        // formats
        for (Format format : availableOutputFormats) {
            imageInfo.formats.add(format.getPreferredExtension());
        }

        // qualities
        for (Quality quality : Quality.values()) {
            imageInfo.qualities.add(quality.toString().toLowerCase());
        }

        return imageInfo;
    }

}
