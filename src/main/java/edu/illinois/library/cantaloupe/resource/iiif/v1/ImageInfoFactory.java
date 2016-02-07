package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.resource.iiif.ImageInfoUtil;

import java.awt.Dimension;
import java.util.List;

abstract class ImageInfoFactory {

    /** Will be used to calculate a maximum scale factor. */
    private static final int MIN_SIZE = 64;

    /** Minimum size that will be used in info.json "tiles" keys. */
    private static final int MIN_TILE_SIZE = 512;

    public static ImageInfo newImageInfo(final String imageUri,
                                         final Processor processor)
            throws ProcessorException {
        final Dimension fullSize = processor.getSize();

        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                processor.getSupportedFeatures(),
                processor.getSupportedIiif1_1Qualities(),
                processor.getAvailableOutputFormats());

        // Find a tile width and height. If the image is not tiled,
        // calculate a tile size close to MIN_TILE_SIZE pixels. Otherwise,
        // use the smallest multiple of the tile size above MIN_TILE_SIZE
        // of image resolution 0.
        Dimension tileSize =
                ImageInfoUtil.smallestTileSize(fullSize, MIN_TILE_SIZE);

        final List<Dimension> tileSizes = processor.getTileSizes();
        if (tileSizes.size() > 0 &&
                (tileSizes.get(0).width != fullSize.width ||
                        tileSizes.get(0).height != fullSize.height)) {
            tileSize = ImageInfoUtil.
                    smallestTileSize(fullSize, tileSizes.get(0), MIN_TILE_SIZE);
        }

        // Create an ImageInfo instance, which will eventually be serialized
        // to JSON.
        final ImageInfo imageInfo = new ImageInfo();
        imageInfo.id = imageUri;
        imageInfo.width = fullSize.width;
        imageInfo.height = fullSize.height;
        imageInfo.profile = complianceLevel.getUri();
        imageInfo.tileWidth = tileSize.width;
        imageInfo.tileHeight = tileSize.height;

        // scale factors
        for (short i = 0; i < 5; i++) {
            imageInfo.scaleFactors.add((int) Math.pow(2, i));
        }

        // formats
        for (OutputFormat format : processor.getAvailableOutputFormats()) {
            imageInfo.formats.add(format.getExtension());
        }

        // qualities
        for (Quality quality : processor.getSupportedIiif1_1Qualities()) {
            imageInfo.qualities.add(quality.toString().toLowerCase());
        }

        return imageInfo;
    }

}
