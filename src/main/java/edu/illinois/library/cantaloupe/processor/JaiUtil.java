package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Transpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.media.jai.operator.TransposeDescriptor;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.util.HashMap;

abstract class JaiUtil {

    private static Logger logger = LoggerFactory.getLogger(JaiUtil.class);

    /**
     * @param inImage Image to crop
     * @param crop Crop operation
     * @return Cropped image, or the input image if the given operation is a
     * no-op.
     */
    public static RenderedOp cropImage(RenderedOp inImage, Crop crop) {
        return cropImage(inImage, crop, new ReductionFactor(0));
    }

    /**
     * Crops the given image taking into account a reduction factor
     * (<code>reductionFactor</code>). In other words, the dimensions of the
     * input image have already been halved <code>reductionFactor</code> times
     * but the given region is relative to the full-sized image.
     *
     * @param inImage Image to crop
     * @param crop Crop operation
     * @param rf Number of times the dimensions of
     *           <code>inImage</code> have already been halved
     *           relative to the full-sized version
     * @return Cropped image, or the input image if the given operation is a
     * no-op.
     */
    public static RenderedOp cropImage(RenderedOp inImage,
                                       Crop crop,
                                       ReductionFactor rf) {
        RenderedOp croppedImage = inImage;
        if (!crop.isNoOp()) {
            // calculate the region x, y, and actual width/height
            final double scale = rf.getScale();
            final double regionX = crop.getX() * scale;
            final double regionY = crop.getY() * scale;
            final double regionWidth = crop.getWidth() * scale;
            final double regionHeight = crop.getHeight() * scale;

            float x, y, requestedWidth, requestedHeight, croppedWidth,
                    croppedHeight;
            if (crop.getUnit().equals(Crop.Unit.PERCENT)) {
                x = (int) Math.round(regionX * inImage.getWidth());
                y = (int) Math.round(regionY * inImage.getHeight());
                requestedWidth = (int) Math.round(regionWidth *
                        inImage.getWidth());
                requestedHeight = (int) Math.round(regionHeight *
                        inImage.getHeight());
            } else {
                x = (int) Math.round(regionX);
                y = (int) Math.round(regionY);
                requestedWidth = (int) Math.round(regionWidth);
                requestedHeight = (int) Math.round(regionHeight);
            }
            // prevent width/height from exceeding the image bounds
            croppedWidth = (x + requestedWidth > inImage.getWidth()) ?
                    inImage.getWidth() - x : requestedWidth;
            croppedHeight = (y + requestedHeight > inImage.getHeight()) ?
                    inImage.getHeight() - y : requestedHeight;

            logger.debug("cropImage(): x: {}; y: {}; width: {}; height: {}",
                    x, y, croppedWidth, croppedHeight);

            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add(x);
            pb.add(y);
            pb.add(croppedWidth);
            pb.add(croppedHeight);
            croppedImage = JAI.create("crop", pb);
        }
        return croppedImage;
    }

    private static RenderingHints defaultRenderingHints(Dimension tileSize) {
        final ImageLayout tileLayout = new ImageLayout();
        tileLayout.setTileWidth(tileSize.width);
        tileLayout.setTileHeight(tileSize.height);

        final TileCache tileCache = JAI.getDefaultInstance().getTileCache();
        tileCache.setMemoryCapacity(0);

        final HashMap<RenderingHints.Key, Object> map = new HashMap<>();
        map.put(JAI.KEY_TILE_CACHE, tileCache);
        map.put(JAI.KEY_IMAGE_LAYOUT, tileLayout);
        map.put(JAI.KEY_INTERPOLATION,
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
        return new RenderingHints(map);
    }

    /**
     * @param inImage Image to filter
     * @param filter Filter operation
     * @return Filtered image, or the input image if the given filter operation
     * is a no-op.
     */
    @SuppressWarnings({"deprecation"}) // really, JAI itself is basically deprecated
    public static RenderedOp filterImage(RenderedOp inImage, Filter filter) {
        RenderedOp filteredImage = inImage;
        if (!filter.isNoOp()) {
            // convert to grayscale
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            final int numBands = OpImage.getExpandedNumBands(
                    inImage.getSampleModel(), inImage.getColorModel());
            double[][] matrix = new double[1][numBands + 1];
            matrix[0][0] = 0.114;
            matrix[0][1] = 0.587;
            matrix[0][2] = 0.299;
            for (int i = 3; i <= numBands; i++) {
                matrix[0][i] = 0;
            }
            pb.add(matrix);
            filteredImage = JAI.create("bandcombine", pb, null);
            if (filter == Filter.BITONAL) {
                pb = new ParameterBlock();
                pb.addSource(filteredImage);
                pb.add(1.0 * 128);
                filteredImage = JAI.create("binarize", pb);
            }
        }
        return filteredImage;
    }

    /**
     * @param inImage Image to reformat
     * @param tileSize JAI tile size
     * @return Reformatted image
     */
    public static RenderedOp reformatImage(PlanarImage inImage,
                                           Dimension tileSize) {
        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(inImage);
        return JAI.create("format", pb, defaultRenderingHints(tileSize));
    }

    /**
     * @param inImage Image to rotate
     * @param rotate Rotate operation
     * @return Rotated image, or the input image if the given rotate operation
     * is a no-op.
     */
    public static RenderedOp rotateImage(RenderedOp inImage,
                                         Rotate rotate) {
        RenderedOp rotatedImage = inImage;
        if (!rotate.isNoOp()) {
            logger.debug("rotateImage(): rotating {} degrees",
                    rotate.getDegrees());

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(rotatedImage);
            pb.add(inImage.getWidth() / 2.0f);
            pb.add(inImage.getHeight() / 2.0f);
            pb.add((float) Math.toRadians(rotate.getDegrees()));
            pb.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
            rotatedImage = JAI.create("rotate", pb);
        }
        return rotatedImage;
    }

    /**
     * @param inImage Image to scale
     * @param scale Scale operation
     * @return Scaled image, or the input image if the given scale is a no-op.
     */
    public static RenderedOp scaleImage(RenderedOp inImage, Scale scale) {
        return scaleImage(inImage, scale, new ReductionFactor(0));
    }

    /**
     * Scales an image using JAI, taking an already-applied reduction factor
     * into account. (In other words, the dimensions of the input image have
     * already been halved <code>reductionFactor</code> times but the given
     * size is relative to the full-sized image.)
     *
     * @param inImage Image to scale
     * @param scale Requested size ignoring any reduction factor
     * @param rf Reduction factor that has already been applied to
     *                        <code>inImage</code>
     * @return Scaled image, or the input image if the given scale is a no-op.
     */
    public static RenderedOp scaleImage(RenderedOp inImage, Scale scale,
                                        ReductionFactor rf) {
        RenderedOp scaledImage = inImage;
        if (!scale.isNoOp()) {
            final int sourceWidth = inImage.getWidth();
            final int sourceHeight = inImage.getHeight();
            final Dimension scaledSize = scale.getResultingSize(
                    new Dimension(sourceWidth, sourceHeight));

            double xScale = scaledSize.width / (double) sourceWidth;
            double yScale = scaledSize.height / (double) sourceHeight;
            if (scale.getPercent() != null) {
                xScale = scale.getPercent() / rf.getScale();
                yScale = scale.getPercent() / rf.getScale();
            }

            logger.debug("scaleImage(): width: {}%; height: {}%",
                    xScale * 100, yScale * 100);
            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add((float) xScale);
            pb.add((float) yScale);
            pb.add(0.0f);
            pb.add(0.0f);
            // JAI bug: The Interpolation.INTERP_BI* interpolations may result
            // in an ArrayIndexOutOfBoundsException in PlanarImage.cobbleByte()
            // during writing, for some images that border the right edge.
            // For example: /iiif/2/56324x18006-tiled-pyramidal.tif/32768,0,23556,18006/737,/0/default.jpg
            // The quality of nearest-neighbor is terrible, but better than
            // nothing.
            // (This also happens when using the "SubsampleAverage" operator
            // instead.)
            // (Incremental downscaling by halves doesn't solve the problem,
            // nor improve output quality.)
            // TODO: see if JAI-EXT is affected
            // TODO: improve downscale quality somehow
            // TODO: add a JaiProcessor.scale_mode option offering this or INTERP_BILINEAR?
            pb.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
            scaledImage = JAI.create("scale", pb);
        }
        return scaledImage;
    }

    /**
     * @param inImage Image to transpose.
     * @param transpose The transpose operation.
     * @return Transposed image, or the input image if the given transpose
     * operation is a no-op.
     */
    public static RenderedOp transposeImage(RenderedOp inImage,
                                            Transpose transpose) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(inImage);
        switch (transpose) {
            case HORIZONTAL:
                logger.debug("transposeImage(): horizontal");
                pb.add(TransposeDescriptor.FLIP_HORIZONTAL);
                break;
            case VERTICAL:
                logger.debug("transposeImage(): vertical");
                pb.add(TransposeDescriptor.FLIP_VERTICAL);
                break;
        }
        return JAI.create("transpose", pb);
    }

}
