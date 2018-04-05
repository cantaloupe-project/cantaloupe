package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.resource.iiif.Feature;
import edu.illinois.library.cantaloupe.resource.iiif.ImageInfoUtil;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ImageInfoFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageInfoFactory.class);

    private static final Set<ServiceFeature> SUPPORTED_SERVICE_FEATURES =
            Collections.unmodifiableSet(
                    EnumSet.of(ServiceFeature.SIZE_BY_CONFINED_WIDTH_HEIGHT,
                            ServiceFeature.SIZE_BY_WHITELISTED,
                            ServiceFeature.BASE_URI_REDIRECT,
                            ServiceFeature.CANONICAL_LINK_HEADER,
                            ServiceFeature.CORS,
                            ServiceFeature.JSON_LD_MEDIA_TYPE,
                            ServiceFeature.PROFILE_LINK_HEADER));

    /**
     * @param imageURI  May be {@literal null}.
     * @param processor
     * @param info
     * @param proxy     May be {@literal null}.
     */
    ImageInfo<String,Object> newImageInfo(final String imageURI,
                                          final Processor processor,
                                          final Info info,
                                          final DelegateProxy proxy) {
        final Configuration config = Configuration.getInstance();

        // We want to use the orientation-aware full size, which takes the
        // embedded orientation into account.
        final Dimension virtualSize = info.getOrientationSize();

        // Create a Map instance, which will eventually be serialized to JSON
        // and returned in the response body.
        final ImageInfo<String,Object> responseInfo = new ImageInfo<>();
        responseInfo.put("@context", "http://iiif.io/api/image/2/context.json");
        responseInfo.put("@id", imageURI);
        responseInfo.put("protocol", "http://iiif.io/api/image");
        responseInfo.put("width", virtualSize.width);
        responseInfo.put("height", virtualSize.height);

        // sizes -- this will be a 2^n series that will work for both multi-
        // and monoresolution images.
        final List<ImageInfo.Size> sizes = getSizes(info.getOrientationSize());
        responseInfo.put("sizes", sizes);

        final int minSize = getMinSize();
        final int maxPixels = getMaxPixels();

        // The max reduction factor is the maximum number of times the full
        // image size can be halved until it's smaller than minSize.
        final int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(virtualSize, minSize);

        // tiles -- this is not a canonical listing of tiles that are
        // actually encoded in the image, but rather a hint to the client as
        // to what can be delivered efficiently.
        final Set<Dimension> uniqueTileSizes = new HashSet<>();

        final int minTileSize = config.getInt(Key.IIIF_MIN_TILE_SIZE, 1024);

        // Find a tile width and height. If the image is not tiled,
        // calculate a tile size close to IIIF_MIN_TILE_SIZE pixels.
        // Otherwise, use the smallest multiple of the tile size above that
        // of image resolution 0.
        final List<ImageInfo.Tile> tiles = new ArrayList<>();
        responseInfo.put("tiles", tiles);

        final Info.Image firstImage =
                info.getImages().get(0);

        // Find the virtual tile size based on the virtual full image size.
        final Dimension virtualTileSize = firstImage.getOrientationTileSize();

        if (info.getImages().size() == 1 &&
                virtualTileSize.equals(virtualSize)) {
            uniqueTileSizes.add(
                    ImageInfoUtil.smallestTileSize(virtualSize, minTileSize));
        } else {
            for (Info.Image image : info.getImages()) {
                uniqueTileSizes.add(
                        ImageInfoUtil.smallestTileSize(virtualSize,
                                image.getOrientationTileSize(), minTileSize));
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
            tiles.add(tile);
        }

        final List<Object> profile = new ArrayList<>();
        responseInfo.put("profile", profile);

        final String complianceUri = ComplianceLevel.getLevel(
                SUPPORTED_SERVICE_FEATURES,
                processor.getSupportedFeatures(),
                processor.getSupportedIIIF2Qualities(),
                processor.getAvailableOutputFormats()).getUri();
        profile.add(complianceUri);

        // formats
        Map<String, Object> profileMap = new HashMap<>();
        Set<String> formatStrings = new HashSet<>();
        for (Format format : processor.getAvailableOutputFormats()) {
            formatStrings.add(format.getPreferredExtension());
        }
        profileMap.put("formats", formatStrings);
        profile.add(profileMap);

        // maxArea (maxWidth and maxHeight are currently not supported)
        if (maxPixels > 0) {
            profileMap.put("maxArea", maxPixels);
        }

        // qualities
        Set<String> qualityStrings = new HashSet<>();
        for (Quality quality : processor.getSupportedIIIF2Qualities()) {
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

        // additional keys
        if (proxy != null) {
            try {
                final Map<String, Object> keyMap =
                        proxy.getExtraIIIFInformationResponseKeys();
                responseInfo.putAll(keyMap);
            } catch (ScriptException e) {
                LOGGER.error(e.getMessage());
            }
        }

        return responseInfo;
    }

    /**
     * @param virtualSize Orientation-aware full size, which takes an embedded
     *                    orientation flag into account.
     */
    List<ImageInfo.Size> getSizes(Dimension virtualSize) {
        // This will be a 2^n series that will work for both multi- and
        // monoresolution images.
        final List<ImageInfo.Size> sizes = new ArrayList<>();

        final int minSize = getMinSize();
        final int maxPixels = getMaxPixels();

        // The min reduction factor is the smallest number of reductions that
        // are required in order to fit within max pixels.
        final int minReductionFactor = (maxPixels > 0) ?
                ImageInfoUtil.minReductionFactor(virtualSize, maxPixels) : 0;
        // The max reduction factor is the maximum number of times the full
        // image size can be halved until it's smaller than minSize.
        final int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(virtualSize, minSize);

        for (double i = Math.pow(2, minReductionFactor);
             i <= Math.pow(2, maxReductionFactor);
             i *= 2) {
            final int width = (int) Math.round(virtualSize.width / i);
            final int height = (int) Math.round(virtualSize.height / i);
            sizes.add(0, new ImageInfo.Size(width, height));
        }
        return sizes;
    }

    /**
     * @return Maximum number of pixels that will be used in {@literal sizes}
     *         keys.
     */
    private int getMaxPixels() {
        return Configuration.getInstance().getInt(Key.MAX_PIXELS, 0);
    }

    /**
     * @return Minimum size that will be used in {@literal sizes} keys.
     */
    private int getMinSize() {
        return Configuration.getInstance().getInt(Key.IIIF_MIN_SIZE, 64);
    }

}
