package edu.illinois.library.cantaloupe.resource.iiif.v3;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.resource.iiif.ImageInfoUtil;
import edu.illinois.library.cantaloupe.script.DelegateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class ImageInfoFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageInfoFactory.class);

    private static final String CONTEXT =
            "http://iiif.io/api/image/3/context.json";

    /**
     * Used when {@link Key#IIIF_MIN_SIZE} is not set.
     */
    private static final int DEFAULT_MIN_SIZE = 64;

    /**
     * Used when {@link Key#IIIF_MIN_TILE_SIZE} is not set.
     */
    private static final int DEFAULT_MIN_TILE_SIZE = 512;

    private static final String PROFILE  = "level2";
    private static final String PROTOCOL = "http://iiif.io/api/image";
    private static final String TYPE     = "ImageService3";

    private DelegateProxy delegateProxy;
    private double maxScale;
    private int maxPixels, minSize, minTileSize;

    ImageInfoFactory() {
        var config  = Configuration.getInstance();
        maxPixels   = config.getInt(Key.MAX_PIXELS, 0);
        maxScale    = config.getDouble(Key.MAX_SCALE, Double.MAX_VALUE);
        minSize     = config.getInt(Key.IIIF_MIN_SIZE, DEFAULT_MIN_SIZE);
        minTileSize = config.getInt(Key.IIIF_MIN_TILE_SIZE, DEFAULT_MIN_TILE_SIZE);
    }

    /**
     * @param processorOutputFormats Return value of {@link
     *                               Processor#getAvailableOutputFormats()}.
     * @param info                   Instance describing the image.
     * @param infoImageIndex         Index of the full/main image in the {@link
     *                               Info} argument's {@link Info#getImages()}
     *                               list.
     * @param scaleConstraint        May be {@code null}.
     */
    ImageInfo<String,Object> newImageInfo(final Set<Format> processorOutputFormats,
                                          final String imageURI,
                                          final Info info,
                                          final int infoImageIndex,
                                          ScaleConstraint scaleConstraint) {
        if (scaleConstraint == null) {
            scaleConstraint = new ScaleConstraint(1, 1);
        }
        // We want to use the orientation-aware full size, which takes the
        // embedded orientation into account.
        final Metadata metadata = info.getMetadata();
        final Orientation orientation = (metadata != null) ?
                metadata.getOrientation() : Orientation.ROTATE_0;
        final Dimension virtualSize =
                orientation.adjustedSize(info.getSize(infoImageIndex));
        virtualSize.scale(scaleConstraint.getRational().doubleValue());

        // Create an instance, which will later be serialized as JSON and
        // returned in the response body.
        final ImageInfo<String,Object> responseInfo = new ImageInfo<>();
        responseInfo.put("@context", CONTEXT);
        responseInfo.put("id", imageURI);
        responseInfo.put("type", TYPE);
        responseInfo.put("protocol", PROTOCOL);
        responseInfo.put("profile", PROFILE);
        responseInfo.put("width", virtualSize.intWidth());
        responseInfo.put("height", virtualSize.intHeight());

        { // maxArea
            // N.B.: maxWidth and maxHeight are not supported as maxArea more
            // succinctly fulfills the "emergency brake" role.
            int effectiveMaxPixels = getEffectiveMaxPixels(virtualSize);
            if (effectiveMaxPixels > 0) {
                responseInfo.put("maxArea", effectiveMaxPixels);
            }
        }

        { // sizes
            var sizes = getSizes(virtualSize);
            responseInfo.put("sizes", sizes);
        }

        { // tiles
            var tiles = getTiles(virtualSize, orientation, info.getImages());
            responseInfo.put("tiles", tiles);
        }

        { // extraQualities
            var qualityStrings = Arrays.stream(Quality.values())
                    .filter(q -> !Quality.DEFAULT.equals(q))
                    .map(q -> q.toString().toLowerCase())
                    .collect(Collectors.toList());
            responseInfo.put("extraQualities", qualityStrings);
        }

        { // extraFormats
            var formatStrings = processorOutputFormats.stream()
                    .filter(f -> !"jpg".equals(f.getKey()) && !"png".equals(f.getKey()))
                    .map(Format::getPreferredExtension)
                    .collect(Collectors.toList());
            responseInfo.put("extraFormats", formatStrings);
        }

        { // extraFeatures
            ScaleConstraint sc = scaleConstraint;
            var featureStrings = Arrays.stream(Feature.values())
                    // sizeUpscaling is not available if the info is being used
                    // for a virtual scale-constrained version, or if upscaling
                    // is disallowed in the configuration.
                    .filter(f -> !(Feature.SIZE_UPSCALING.equals(f) && (sc.hasEffect() || maxScale <= 1)))
                    .map(Feature::getName)
                    .collect(Collectors.toList());
            responseInfo.put("extraFeatures", featureStrings);
        }

        // additional keys
        if (delegateProxy != null) {
            try {
                var keyMap = delegateProxy.getExtraIIIF3InformationResponseKeys();
                responseInfo.putAll(keyMap);
            } catch (ScriptException e) {
                LOGGER.error(e.getMessage());
            }
        }

        return responseInfo;
    }

    /**
     * Returns a 2^n series that will work for both multi-and monoresolution
     * images.
     *
     * @param virtualSize Orientation-aware and {@link ScaleConstraint
     *                    scale-constrained} full size.
     */
    List<ImageInfo.Size> getSizes(Dimension virtualSize) {
        // This will be a 2^n series that will work for both multi- and
        // monoresolution images.
        final List<ImageInfo.Size> sizes = new ArrayList<>();

        // The min reduction factor is the smallest number of reductions that
        // are required in order to fit within maxPixels.
        final int effectiveMaxPixels = getEffectiveMaxPixels(virtualSize);
        final int minReductionFactor = (effectiveMaxPixels > 0) ?
                ImageInfoUtil.minReductionFactor(virtualSize, effectiveMaxPixels) : 0;
        // The max reduction factor is the number of times the full image
        // dimensions can be halved until they're smaller than minSize.
        final int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(virtualSize, minSize);

        for (double i = Math.pow(2, minReductionFactor);
             i <= Math.pow(2, maxReductionFactor);
             i *= 2) {
            final int width  = (int) Math.round(virtualSize.width() / i);
            final int height = (int) Math.round(virtualSize.height() / i);
            sizes.add(0, new ImageInfo.Size(width, height));
        }
        return sizes;
    }

    /**
     * Finds a tile width and height.
     *
     * If the image is not tiled, a tile size is chosen that is close to the
     * minimum allowed. Otherwise, the smallest multiple of the tile size above
     * that of image resolution 0 is used.
     *
     * This is not a canonical listing of tiles that are actually encoded in
     * the image, but instead a hint to the client as to what is efficient to
     * deliver (which may or may not match the physical tile size or a multiple
     * of it).
     */
    List<ImageInfo.Tile> getTiles(Dimension virtualSize,
                                  Orientation orientation,
                                  List<Info.Image> images) {
        final List<ImageInfo.Tile> tiles     = new ArrayList<>();
        final Set<Dimension> uniqueTileSizes = new HashSet<>();

        images.forEach(image ->
                uniqueTileSizes.add(ImageInfoUtil.getTileSize(
                        virtualSize,
                        orientation.adjustedSize(image.getTileSize()),
                        minTileSize)));

        // The max reduction factor is the maximum number of times the full
        // image size can be halved until it's smaller than minSize.
        final int maxReductionFactor =
                ImageInfoUtil.maxReductionFactor(virtualSize, minSize);

        for (Dimension uniqueTileSize : uniqueTileSizes) {
            final ImageInfo.Tile tile = new ImageInfo.Tile();
            tile.width  = (int) Math.ceil(uniqueTileSize.width());
            tile.height = (int) Math.ceil(uniqueTileSize.height());
            // Add every scale factor up to 2^RFmax.
            for (int i = 0; i <= maxReductionFactor; i++) {
                tile.scaleFactors.add((int) Math.pow(2, i));
            }
            tiles.add(tile);
        }
        return tiles;
    }

    /**
     * @param fullSize Full source image size.
     * @return         The smaller of {@link #maxPixels} or the area at {@link
     *                 #maxScale}.
     */
    private int getEffectiveMaxPixels(Dimension fullSize) {
        final double area = fullSize.width() * fullSize.height();
        return (int) Math.min(area * maxScale, maxPixels);
    }

    void setDelegateProxy(DelegateProxy proxy) {
        this.delegateProxy = proxy;
    }

    /**
     * @param maxPixels Maximum number of pixels that will be used in {@code
     *                  sizes} keys.
     */
    void setMaxPixels(int maxPixels) {
        this.maxPixels = maxPixels;
    }

    /**
     * @param maxScale Maximum allowed scale.
     */
    void setMaxScale(double maxScale) {
        this.maxScale = maxScale;
    }

    /**
     * @param minSize Minimum size that will be used in {@code sizes} keys.
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
