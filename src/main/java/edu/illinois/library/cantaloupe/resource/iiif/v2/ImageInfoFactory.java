package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.resource.iiif.Feature;
import edu.illinois.library.cantaloupe.resource.iiif.ImageInfoUtil;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
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

    /**
     * Will be used if {@link Key#IIIF_MIN_SIZE} is not set.
     */
    private static final int DEFAULT_MIN_SIZE = 64;

    /**
     * Will be used if {@link Key#IIIF_MIN_TILE_SIZE} is not set.
     */
    private static final int DEFAULT_MIN_TILE_SIZE = 512;

    private static final Set<ServiceFeature> SUPPORTED_SERVICE_FEATURES =
            Collections.unmodifiableSet(
                    EnumSet.of(ServiceFeature.SIZE_BY_CONFINED_WIDTH_HEIGHT,
                            ServiceFeature.SIZE_BY_WHITELISTED,
                            ServiceFeature.BASE_URI_REDIRECT,
                            ServiceFeature.CANONICAL_LINK_HEADER,
                            ServiceFeature.CORS,
                            ServiceFeature.JSON_LD_MEDIA_TYPE,
                            ServiceFeature.PROFILE_LINK_HEADER));

    private Set<ProcessorFeature> processorFeatures;
    private Set<Quality> processorQualities;
    private Set<Format> processorOutputFormats;
    private DelegateProxy delegateProxy;
    private int maxPixels, minSize, minTileSize;

    /**
     * @param processorFeatures      Return value of {@link
     *                               Processor#getSupportedFeatures()}.
     * @param processorQualities     Return value of {@link
     *                               Processor#getSupportedIIIF2Qualities()}.
     * @param processorOutputFormats Return value of {@link
     *                               Processor#getAvailableOutputFormats()}.
     */
    ImageInfoFactory(final Set<ProcessorFeature> processorFeatures,
                     final Set<Quality> processorQualities,
                     final Set<Format> processorOutputFormats) {
        Configuration config = Configuration.getInstance();
        maxPixels = config.getInt(Key.MAX_PIXELS, 0);
        minSize = config.getInt(Key.IIIF_MIN_SIZE, DEFAULT_MIN_SIZE);
        minTileSize = config.getInt(Key.IIIF_MIN_TILE_SIZE, DEFAULT_MIN_TILE_SIZE);

        this.processorFeatures = processorFeatures;
        this.processorQualities = processorQualities;
        this.processorOutputFormats = processorOutputFormats;
    }

    /**
     * @param imageURI        May be {@literal null}.
     * @param info            Info describing the image.
     * @param infoImageIndex  Index of the full/main image in the {@link Info}
     *                        argument's {@link Info#getImages()} list.
     * @param scaleConstraint May be {@literal null}.
     */
    ImageInfo<String,Object> newImageInfo(final String imageURI,
                                          final Info info,
                                          final int infoImageIndex,
                                          ScaleConstraint scaleConstraint) {
        if (scaleConstraint == null) {
            scaleConstraint = new ScaleConstraint(1, 1);
        }
        // We want to use the orientation-aware full size, which takes the
        // embedded orientation into account.
        final Dimension virtualSize = info.getOrientationSize(infoImageIndex);
        virtualSize.scaleBy(scaleConstraint.getScale());

        // Create a Map instance, which will eventually be serialized to JSON
        // and returned in the response body.
        final ImageInfo<String,Object> responseInfo = new ImageInfo<>();
        responseInfo.put("@context", "http://iiif.io/api/image/2/context.json");
        responseInfo.put("@id", imageURI);
        responseInfo.put("protocol", "http://iiif.io/api/image");
        responseInfo.put("width", virtualSize.intWidth());
        responseInfo.put("height", virtualSize.intHeight());

        // sizes -- this will be a 2^n series that will work for both multi-
        // and monoresolution images.
        final List<ImageInfo.Size> sizes = getSizes(virtualSize);
        responseInfo.put("sizes", sizes);

        // The max reduction factor is the maximum number of times the full
        // image size can be halved until it's smaller than minSize.
        final int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(virtualSize, minSize);

        // tiles -- this is not a canonical listing of tiles that are
        // actually encoded in the image, but rather a hint to the client as
        // to what is efficient to deliver.
        final Set<Dimension> uniqueTileSizes = new HashSet<>();

        // Find a tile width and height. If the image is not tiled,
        // calculate a tile size close to IIIF_MIN_TILE_SIZE pixels.
        // Otherwise, use the smallest multiple of the tile size above that
        // of image resolution 0.
        final List<ImageInfo.Tile> tiles = new ArrayList<>();
        responseInfo.put("tiles", tiles);

        info.getImages().forEach(image ->
                uniqueTileSizes.add(ImageInfoUtil.getTileSize(
                        virtualSize, image.getOrientationTileSize(),
                        minTileSize)));

        for (Dimension uniqueTileSize : uniqueTileSizes) {
            final ImageInfo.Tile tile = new ImageInfo.Tile();
            tile.width = (int) Math.ceil(uniqueTileSize.width());
            tile.height = (int) Math.ceil(uniqueTileSize.height());
            // Add every scale factor up to 2^RFmax.
            for (int i = 0; i <= maxReductionFactor; i++) {
                tile.scaleFactors.add((int) Math.pow(2, i));
            }
            tiles.add(tile);
        }

        final List<Object> profile = new ArrayList<>(2);
        responseInfo.put("profile", profile);

        final String complianceUri = ComplianceLevel.getLevel(
                SUPPORTED_SERVICE_FEATURES,
                processorFeatures,
                processorQualities,
                processorOutputFormats).getUri();
        profile.add(complianceUri);

        // formats
        Map<String, Object> profileMap = new HashMap<>();
        Set<String> formatStrings = new HashSet<>();
        for (Format format : processorOutputFormats) {
            formatStrings.add(format.getPreferredExtension());
        }
        profileMap.put("formats", formatStrings);
        profile.add(profileMap);

        // maxArea (maxWidth and maxHeight are not supported)
        if (maxPixels > 0) {
            profileMap.put("maxArea", maxPixels);
        }

        // qualities
        final Set<String> qualityStrings = new HashSet<>();
        for (Quality quality : processorQualities) {
            qualityStrings.add(quality.toString().toLowerCase());
        }
        profileMap.put("qualities", qualityStrings);

        // supports
        final Set<String> featureStrings = new HashSet<>();
        for (Feature feature : processorFeatures) {
            // If the info is being used for a virtual scale-constrained
            // version, sizeAboveFull should not be available.
            if (ProcessorFeature.SIZE_ABOVE_FULL.equals(feature) &&
                    (scaleConstraint.getNumerator() != 1 ||
                            scaleConstraint.getDenominator() != 1)) {
                continue;
            }
            featureStrings.add(feature.getName());
        }
        for (Feature feature : SUPPORTED_SERVICE_FEATURES) {
            featureStrings.add(feature.getName());
        }
        profileMap.put("supports", featureStrings);

        // additional keys
        if (delegateProxy != null) {
            try {
                final Map<String, Object> keyMap =
                        delegateProxy.getExtraIIIFInformationResponseKeys();
                responseInfo.putAll(keyMap);
            } catch (ScriptException e) {
                LOGGER.error(e.getMessage());
            }
        }

        return responseInfo;
    }

    /**
     * @param virtualSize Orientation-aware and {@link ScaleConstraint
     *                    scale-constrained} full size.
     */
    List<ImageInfo.Size> getSizes(Dimension virtualSize) {
        // This will be a 2^n series that will work for both multi- and
        // monoresolution images.
        final List<ImageInfo.Size> sizes = new ArrayList<>();

        // The min reduction factor is the smallest number of reductions that
        // are required in order to fit within maxPixels.
        final int minReductionFactor = (maxPixels > 0) ?
                ImageInfoUtil.minReductionFactor(virtualSize, maxPixels) : 0;
        // The max reduction factor is the number of times the full image
        // dimensions can be halved until they're smaller than minSize.
        final int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(virtualSize, minSize);

        for (double i = Math.pow(2, minReductionFactor);
             i <= Math.pow(2, maxReductionFactor);
             i *= 2) {
            final int width = (int) Math.round(virtualSize.width() / i);
            final int height = (int) Math.round(virtualSize.height() / i);
            sizes.add(0, new ImageInfo.Size(width, height));
        }
        return sizes;
    }

    void setDelegateProxy(DelegateProxy proxy) {
        this.delegateProxy = proxy;
    }

    /**
     * @param maxPixels Maximum number of pixels that will be used in {@literal
     *                  sizes} keys.
     */
    void setMaxPixels(int maxPixels) {
        this.maxPixels = maxPixels;
    }

    /**
     * @param minSize Minimum size that will be used in {@literal sizes} keys.
     */
    void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    /**
     * @param minTileSize Minimum size that will be used in a tile dimension.
     */
    void setMinTileSize(int minTileSize) {
        this.minTileSize = minTileSize;
    }

}
