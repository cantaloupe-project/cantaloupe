package edu.illinois.library.cantaloupe.processor;

import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.Transpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.media.jai.operator.TransposeDescriptor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Iterator;

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
            final double scale = ProcessorUtil.getScale(rf);
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
            double[][] matrixRgb = { { 0.114, 0.587, 0.299, 0 } };
            double[][] matrixRgba = { { 0.114, 0.587, 0.299, 0, 0 } };
            if (OpImage.getExpandedNumBands(inImage.getSampleModel(),
                    inImage.getColorModel()) == 4) {
                pb.add(matrixRgba);
            } else {
                pb.add(matrixRgb);
            }
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
     * @param readableChannel
     * @return
     */
    public static RenderedImage readImage(
            ReadableByteChannel readableChannel) throws IOException {
        final ParameterBlockJAI pbj = new ParameterBlockJAI("ImageRead");
        pbj.setParameter("Input", ImageIO.createImageInputStream(readableChannel));
        return JAI.create("ImageRead", pbj,
                defaultRenderingHints(new Dimension(512, 512)));
    }

    /**
     * @param inputFile
     * @param sourceFormat
     * @param ops
     * @param fullSize
     * @param reductionFactor
     * @return The read image. Use {@link #reformatImage} to convert into a
     * RenderedOp.
     * @throws IOException
     * @throws ProcessorException
     */
    public static RenderedImage readImage(File inputFile,
                                          SourceFormat sourceFormat,
                                          OperationList ops,
                                          Dimension fullSize,
                                          ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return doReadImage(inputFile, sourceFormat, ops, fullSize,
                reductionFactor);
    }

    /**
     * @param readableChannel
     * @param sourceFormat
     * @param ops
     * @param fullSize
     * @param reductionFactor
     * @return The read image. Use {@link #reformatImage} to convert into a
     * RenderedOp.
     * @throws IOException
     * @throws ProcessorException
     */
    public static RenderedImage readImage(ReadableByteChannel readableChannel,
                                          SourceFormat sourceFormat,
                                          OperationList ops,
                                          Dimension fullSize,
                                          ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return doReadImage(readableChannel, sourceFormat, ops, fullSize,
                reductionFactor);
    }

    /**
     * @param inputSource {@link InputStream} or {@link File}
     * @param sourceFormat
     * @param ops
     * @param fullSize
     * @param reductionFactor
     * @return
     * @throws IOException
     * @throws UnsupportedSourceFormatException
     */
    private static RenderedImage doReadImage(Object inputSource,
                                             SourceFormat sourceFormat,
                                             OperationList ops,
                                             Dimension fullSize,
                                             ReductionFactor reductionFactor)
            throws IOException, UnsupportedSourceFormatException {
        RenderedImage image;
        switch (sourceFormat) {
            case TIF:
                image = readImageWithTiffImageDecoder(inputSource, ops, fullSize,
                        reductionFactor);
                break;
            default:
                final ParameterBlockJAI pbj = new ParameterBlockJAI("ImageRead");
                pbj.setParameter("Input", inputSource);
                image = JAI.create("ImageRead", pbj,
                        defaultRenderingHints(new Dimension(512, 512)));
                break;
        }
        if (image == null) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        }
        return image;
    }

    /**
     * Reads a TIFF image using the JAI TIFFImageDecoder.
     *
     * @param inputSource {@link InputStream} or {@link File}
     * @param ops
     * @param fullSize
     * @param reductionFactor
     * @return
     * @throws IOException
     * @throws IllegalArgumentException if <code>inputSource</code> is invalid
     */
    public static RenderedImage readImageWithTiffImageDecoder(
            Object inputSource, OperationList ops, Dimension fullSize,
            ReductionFactor reductionFactor) throws IOException {
        RenderedImage image = null;
        try {
            ImageDecoder dec;
            if (inputSource instanceof ReadableByteChannel) {
                dec = ImageCodec.createImageDecoder("tiff",
                        Channels.newInputStream((ReadableByteChannel) inputSource), null);
            } else if (inputSource instanceof File) {
                dec = ImageCodec.createImageDecoder("tiff",
                        (File) inputSource, null);
            } else {
                throw new IllegalArgumentException("Invalid inputSource parameter");
            }
            if (dec != null) {
                Crop crop = new Crop();
                crop.setFull(true);
                Scale scale = new Scale();
                scale.setMode(Scale.Mode.FULL);
                for (Operation op : ops) {
                    if (op instanceof Crop) {
                        crop = (Crop) op;
                    } else if (op instanceof Scale) {
                        scale = (Scale) op;
                    }
                }
                image = getSmallestUsableImage(dec, fullSize, crop, scale,
                        reductionFactor);
            }
        } finally {
            if (inputSource instanceof InputStream) {
                ((InputStream) inputSource).close();
            }
        }
        return image;
    }

    /**
     * Returns the smallest image fitting the requested size from the given
     * reader. Useful for e.g. pyramidal TIFF.
     *
     * @param decoder ImageDecoder with input source already set
     * @param fullSize
     * @param crop Requested crop
     * @param scale Requested scale
     * @param rf Set by reference
     * @return
     * @throws IOException
     */
    private static RenderedImage getSmallestUsableImage(ImageDecoder decoder,
                                                        Dimension fullSize,
                                                        Crop crop,
                                                        Scale scale,
                                                        ReductionFactor rf)
            throws IOException {
        RenderedImage bestImage = null;
        if (!scale.isNoOp()) {
            // Pyramidal TIFFs will have > 1 "page," each half the dimensions of
            // the next larger.
            int numImages = decoder.getNumPages();
            if (numImages > 1) {
                logger.debug("Detected multi-resolution image with {} levels",
                        numImages);
                final Rectangle regionRect = crop.getRectangle(fullSize);

                // Loop through the tiles from smallest to largest to find the
                // first one that fits the requested scale
                for (int i = numImages - 1; i >= 0; i--) {
                    final RenderedImage tile = decoder.decodeAsRenderedImage(i);
                    final double tileScale = (double) tile.getWidth() /
                            (double) fullSize.width;
                    boolean fits = false;
                    if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                        fits = (scale.getWidth() / (float) regionRect.width <= tileScale);
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                        fits = (scale.getHeight() / (float) regionRect.height <= tileScale);
                    } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                        fits = (scale.getWidth() / (float) regionRect.width <= tileScale &&
                                scale.getHeight() / (float) regionRect.height <= tileScale);
                    } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                        fits = (scale.getWidth() / (float) regionRect.width <= tileScale &&
                                scale.getHeight() / (float) regionRect.height <= tileScale);
                    } else if (scale.getPercent() != null) {
                        float pct = scale.getPercent();
                        fits = ((pct * fullSize.width) / (float) regionRect.width <= tileScale &&
                                (pct * fullSize.height) / (float) regionRect.height <= tileScale);
                    }
                    if (fits) {
                        rf.factor = ProcessorUtil.
                                getReductionFactor(tileScale, 0).factor;
                        logger.debug("Using a {}x{} source tile ({}x reduction factor)",
                                tile.getWidth(), tile.getHeight(), rf.factor);
                        bestImage = tile;
                        break;
                    }
                }
            }
        }
        if (bestImage == null) {
            bestImage = decoder.decodeAsRenderedImage();
        }
        return bestImage;
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
            final double sourceWidth = inImage.getWidth();
            final double sourceHeight = inImage.getHeight();
            double xScale = 1.0f;
            double yScale = 1.0f;
            if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                xScale = yScale = scale.getWidth() / sourceWidth;
            } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                xScale = yScale = scale.getHeight() / sourceHeight;
            } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                xScale = scale.getWidth() / sourceWidth;
                yScale = scale.getHeight() / sourceHeight;
            } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                double hScale = scale.getWidth() / sourceWidth;
                double vScale = scale.getHeight() / sourceHeight;
                xScale = sourceWidth * Math.min(hScale, vScale);
                yScale = sourceHeight * Math.min(hScale, vScale);
            } else if (scale.getPercent() != null) {
                int reqRf = ProcessorUtil.
                        getReductionFactor(scale.getPercent(), 0).factor;
                xScale = yScale = ProcessorUtil.getScale(
                        new ReductionFactor(reqRf - rf.factor));
            }
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add((float) xScale);
            pb.add((float) yScale);
            pb.add(0.0f);
            pb.add(0.0f);
            pb.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
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
                pb.add(TransposeDescriptor.FLIP_HORIZONTAL);
                break;
            case VERTICAL:
                pb.add(TransposeDescriptor.FLIP_VERTICAL);
                break;
        }
        return JAI.create("transpose", pb);
    }

    /**
     * Writes an image to the given output stream.
     *
     * @param image Image to write
     * @param outputFormat Format of the output image
     * @param writableChannel Channel to write the image to
     * @throws IOException
     */
    public static void writeImage(RenderedOp image,
                                  OutputFormat outputFormat,
                                  WritableByteChannel writableChannel)
            throws IOException {
        switch (outputFormat) {
            case GIF:
                // TODO: this and ImageIO.write() frequently don't work
                Iterator writers = ImageIO.getImageWritersByFormatName("GIF");
                if (writers.hasNext()) {
                    // GIFWriter can't deal with a non-0,0 origin
                    ParameterBlock pb = new ParameterBlock();
                    pb.addSource(image);
                    pb.add((float) -image.getMinX());
                    pb.add((float) -image.getMinY());
                    image = JAI.create("translate", pb);

                    ImageWriter writer = (ImageWriter) writers.next();
                    ImageOutputStream os = ImageIO.
                            createImageOutputStream(writableChannel);
                    writer.setOutput(os);
                    writer.write(image);
                }
                break;
            case JP2:
                // TODO: neither this nor ImageIO.write() seem to write anything
                writers = ImageIO.getImageWritersByFormatName("JPEG2000");
                if (writers.hasNext()) {
                    ImageWriter writer = (ImageWriter) writers.next();
                    IIOImage iioImage = new IIOImage(image, null, null);
                    J2KImageWriteParam j2Param = new J2KImageWriteParam();
                    j2Param.setLossless(false);
                    j2Param.setEncodingRate(Double.MAX_VALUE);
                    j2Param.setCodeBlockSize(new int[]{128, 8});
                    j2Param.setTilingMode(ImageWriteParam.MODE_DISABLED);
                    j2Param.setProgressionType("res");
                    ImageOutputStream os = ImageIO.
                            createImageOutputStream(writableChannel);
                    writer.setOutput(os);
                    writer.write(null, iioImage, j2Param);
                }
                break;
            case JPG:
                JAI.create("encode", image.getAsBufferedImage(),
                        Channels.newOutputStream(writableChannel), "JPEG", null);
                break;
            case PNG:
                // ImageIO.write() seems to be more efficient than
                // PNGImageEncoder
                ImageIO.write(image, outputFormat.getExtension(),
                        ImageIO.createImageOutputStream(writableChannel));
                /* PNGEncodeParam pngParam = new PNGEncodeParam.RGB();
                ImageEncoder pngEncoder = ImageCodec.createImageEncoder("PNG",
                        outputStream, pngParam);
                pngEncoder.encode(image); */
                break;
            case TIF:
                // TIFFImageEncoder seems to be more efficient than
                // ImageIO.write();
                ImageEncoder tiffEnc = new TIFFImageEncoder(
                        Channels.newOutputStream(writableChannel), null);
                tiffEnc.encode(image);
                break;
        }

    }

}
