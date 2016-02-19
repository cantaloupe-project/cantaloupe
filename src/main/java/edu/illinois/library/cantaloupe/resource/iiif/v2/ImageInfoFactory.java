package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorException;
import edu.illinois.library.cantaloupe.resource.iiif.Feature;
import edu.illinois.library.cantaloupe.resource.iiif.ImageInfoUtil;
import edu.illinois.library.cantaloupe.script.DelegateScriptDisabledException;
import edu.illinois.library.cantaloupe.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

abstract class ImageInfoFactory {

    private static Logger logger = LoggerFactory.
            getLogger(ImageInfoFactory.class);

    public static final String MIN_TILE_SIZE_CONFIG_KEY =
            "endpoint.iiif.min_tile_size";

    /** Minimum size that will be used in info.json "sizes" keys. */
    private static final int MIN_SIZE = 64;

    /** Delegate script method that returns the JSON object (actually Ruby
     * hash) corresponding to the "service" key. */
    private static final String SERVICE_DELEGATE_METHOD = "get_iiif2_service";

    /** Will be populated in the static initializer. */
    private static final Set<ServiceFeature> SUPPORTED_SERVICE_FEATURES =
            new HashSet<>();

    static {
        SUPPORTED_SERVICE_FEATURES.addAll(Arrays.asList(
                ServiceFeature.SIZE_BY_WHITELISTED,
                ServiceFeature.BASE_URI_REDIRECT,
                ServiceFeature.CANONICAL_LINK_HEADER,
                ServiceFeature.CORS,
                ServiceFeature.JSON_LD_MEDIA_TYPE,
                ServiceFeature.PROFILE_LINK_HEADER));
    }

    public static ImageInfo newImageInfo(final Identifier identifier,
                                         final String imageUri,
                                         final Processor processor,
                                         final edu.illinois.library.cantaloupe.processor.ImageInfo cacheInfo)
            throws ProcessorException {
        final Dimension fullSize = cacheInfo.getSize();
        // Create an ImageInfo instance, which will eventually be serialized
        // to JSON and sent as the response body.
        final ImageInfo imageInfo = new ImageInfo();
        imageInfo.id = imageUri;
        imageInfo.width = fullSize.width;
        imageInfo.height = fullSize.height;

        final String complianceUri = ComplianceLevel.getLevel(
                SUPPORTED_SERVICE_FEATURES,
                processor.getSupportedFeatures(),
                processor.getSupportedIiif2_0Qualities(),
                processor.getAvailableOutputFormats()).getUri();
        imageInfo.profile.add(complianceUri);

        // sizes -- this will be a 2^n series that will work for both multi-
        // and monoresolution images.
        final int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(fullSize, MIN_SIZE);
        for (double i = 2; i <= Math.pow(2, maxReductionFactor); i *= 2) {
            final int width = (int) Math.round(fullSize.width / i);
            final int height = (int) Math.round(fullSize.height / i);
            if (width < MIN_SIZE || height < MIN_SIZE) {
                break;
            }
            ImageInfo.Size size = new ImageInfo.Size(width, height);
            imageInfo.sizes.add(0, size);
        }

        // tiles -- this is not a canonical listing of tiles that are
        // actually encoded in the image, but rather a hint to the client as
        // to what can be delivered efficiently.
        final Set<Dimension> uniqueTileSizes = new HashSet<>();

        final int minTileSize = Application.getConfiguration().
                getInt(MIN_TILE_SIZE_CONFIG_KEY, 1024);

        // Find a tile width and height. If the image is not tiled,
        // calculate a tile size close to MIN_TILE_SIZE_CONFIG_KEY pixels.
        // Otherwise, use the smallest multiple of the tile size above that
        // of image resolution 0.
        if (cacheInfo.getImages().size() == 1 &&
                (cacheInfo.getImages().get(0).tileWidth == fullSize.width ||
                        cacheInfo.getImages().get(0).tileWidth == 0) &&
                (cacheInfo.getImages().get(0).tileHeight == fullSize.height ||
                        cacheInfo.getImages().get(0).tileHeight == 0)) {
            uniqueTileSizes.add(
                    ImageInfoUtil.smallestTileSize(fullSize, minTileSize));
        } else {
            for (edu.illinois.library.cantaloupe.processor.ImageInfo.Image image : cacheInfo.getImages()) {
                uniqueTileSizes.add(
                        ImageInfoUtil.smallestTileSize(fullSize,
                                image.getTileSize(), minTileSize));
            }
        }
        for (Dimension uniqueTileSize : uniqueTileSizes) {
            final ImageInfo.Tile tile = new ImageInfo.Tile();
            tile.width = uniqueTileSize.width;
            tile.height = uniqueTileSize.height;
            // Add every scale factor up to 2^n.
            for (int i = 0; i <= maxReductionFactor; i++) {
                tile.scaleFactors.add((int) Math.pow(2, i));
            }
            imageInfo.tiles.add(tile);
        }

        // formats
        Map<String, Set<String>> profileMap = new HashMap<>();
        Set<String> formatStrings = new HashSet<>();
        for (Format format : processor.getAvailableOutputFormats()) {
            formatStrings.add(format.getPreferredExtension());
        }
        profileMap.put("formats", formatStrings);
        imageInfo.profile.add(profileMap);

        // qualities
        Set<String> qualityStrings = new HashSet<>();
        for (Quality quality : processor.getSupportedIiif2_0Qualities()) {
            qualityStrings.add(quality.toString().toLowerCase());
        }
        profileMap.put("qualities", qualityStrings);

        // supports
        Set<String> featureStrings = new HashSet<>();
        for (Feature feature : processor.getSupportedFeatures()) {
            featureStrings.add(feature.getName());
        }
        for (Feature feature : SUPPORTED_SERVICE_FEATURES) {
            featureStrings.add(feature.getName());
        }
        profileMap.put("supports", featureStrings);

        // service
        try {
            final String[] args = { identifier.toString() };
            imageInfo.service = (Map) ScriptEngineFactory.getScriptEngine().
                    invoke(SERVICE_DELEGATE_METHOD, args);
        } catch (DelegateScriptDisabledException e) {
            logger.info("Delegate script disabled; skipping service " +
                    "information.");
        } catch (ScriptException | IOException e) {
            logger.error(e.getMessage());
        }

        return imageInfo;
    }

}
