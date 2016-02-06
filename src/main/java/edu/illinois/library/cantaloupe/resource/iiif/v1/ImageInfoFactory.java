package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.util.List;

abstract class ImageInfoFactory {

    private static Logger logger = LoggerFactory.
            getLogger(ImageInfoFactory.class);

    /** Minimum size that will be used in info.json "tiles" keys. */
    private static final int MIN_TILE_SIZE = 256;

    public static ImageInfo newImageInfo(final String imageUri,
                                         final Processor processor)
            throws ProcessorException {
        final Dimension fullSize = processor.getSize();

        final ComplianceLevel complianceLevel = ComplianceLevel.getLevel(
                processor.getSupportedFeatures(),
                processor.getSupportedIiif1_1Qualities(),
                processor.getAvailableOutputFormats());


        // Find a tile width and height. If the image is not tiled, calculate
        // a tile size close to MIN_TILE_SIZE pixels. Otherwise, use the
        // tile size of image resolution 0.
        Dimension tileSize = calculateTileSize(fullSize, MIN_TILE_SIZE);;
        try {
            final List<Dimension> tileSizes = processor.getTileSizes();
            if (tileSizes.size() > 0 &&
                    (tileSizes.get(0).width != fullSize.width ||
                            tileSizes.get(0).height != fullSize.height)) {
                tileSize = calculateTileSize(tileSizes.get(0), MIN_TILE_SIZE);
            }
        } catch (ProcessorException e) {
            logger.error(e.getMessage(), e);
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

    /**
     * Returns the closest tile size to the given minimum dimension based on
     * the series of 1/(2^n).
     *
     * @param fullSize Full size of the source image.
     * @param minDimension Minimum allowed dimension.
     * @return Tile size
     */
    private static Dimension calculateTileSize(Dimension fullSize,
                                               int minDimension) {
        Dimension size = new Dimension(fullSize.width, fullSize.height);
        int nextWidth = size.width;
        int nextHeight = size.height;
        for (int i = 0; i < 9999; i++) {
            nextWidth /= 2f;
            nextHeight /= 2f;
            if (nextWidth < minDimension || nextHeight < minDimension) {
                return size;
            }
            size.width = nextWidth;
            size.height = nextHeight;
        }
        return fullSize;
    }

}
