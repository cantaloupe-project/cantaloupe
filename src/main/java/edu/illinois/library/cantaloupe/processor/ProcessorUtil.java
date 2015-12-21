package edu.illinois.library.cantaloupe.processor;

import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Crop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A collection of helper methods for reading, writing, and performing
 * operations on images using the Java 2D, ImageIO, and JAI frameworks.
 */
abstract class ProcessorUtil {

    private static Logger logger = LoggerFactory.getLogger(ProcessorUtil.class);

    /**
     * <p>BufferedImages with a type of <code>TYPE_CUSTOM</code> won't work
     * with various operations, so this method copies them into a new image of
     * type RGB.</p>
     *
     * <p>This is extremely expensive and should be avoided if possible.</p>
     *
     * @param inImage Image to convert
     * @return A new BufferedImage of type RGB, or the input image if it
     * already is RGB
     */
    public static BufferedImage convertToRgb(BufferedImage inImage) {
        BufferedImage outImage = inImage;
        if (inImage != null && inImage.getType() == BufferedImage.TYPE_CUSTOM) {
            outImage = new BufferedImage(inImage.getWidth(),
                    inImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = outImage.createGraphics();
            g.drawImage(inImage, 0, 0, null);
            g.dispose();
        }
        return outImage;
    }

    /**
     * @param inImage Image to crop
     * @param crop Crop operation
     * @return Cropped image, or the input image if the given operation is a
     * no-op.
     */
    public static BufferedImage cropImage(BufferedImage inImage,
                                          Crop crop) {
        return cropImage(inImage, crop, 0);
    }

    /**
     * Crops the given image taking into account a reduction factor
     * (<code>reductionFactor</code>). In other words, the dimensions of the
     * input image have already been halved <code>reductionFactor</code> times
     * but the given region is relative to the full-sized image.
     *
     * @param inImage Image to crop
     * @param crop Crop operation
     * @param reductionFactor Number of times the dimensions of
     *                        <code>inImage</code> have already been halved
     *                        relative to the full-sized version
     * @return Cropped image, or the input image if the given operation is a
     * no-op.
     */
    public static BufferedImage cropImage(BufferedImage inImage,
                                          Crop crop,
                                          int reductionFactor) {
        BufferedImage croppedImage = inImage;
        if (!crop.isNoOp()) {
            final double scale = getScale(reductionFactor);
            final double regionX = crop.getX() * scale;
            final double regionY = crop.getY() * scale;
            final double regionWidth = crop.getWidth() * scale;
            final double regionHeight = crop.getHeight() * scale;

            int x, y, requestedWidth, requestedHeight, croppedWidth,
                    croppedHeight;
            if (crop.getUnit().equals(Crop.Unit.PERCENT)) {
                x = (int) Math.round(regionX * inImage.getWidth());
                y = (int) Math.round(regionY * inImage.getHeight());
                requestedWidth = (int) Math.round(regionWidth  *
                        inImage.getWidth());
                requestedHeight = (int) Math.round(regionHeight *
                        inImage.getHeight());
            } else {
                x = (int) Math.round(regionX);
                y = (int) Math.round(regionY);
                requestedWidth = (int) Math.round(regionWidth);
                requestedHeight = (int) Math.round(regionHeight);
            }
            // BufferedImage.getSubimage() will protest if asked for more
            // width/height than is available
            croppedWidth = (x + requestedWidth > inImage.getWidth()) ?
                    inImage.getWidth() - x : requestedWidth;
            croppedHeight = (y + requestedHeight > inImage.getHeight()) ?
                    inImage.getHeight() - y : requestedHeight;
            croppedImage = inImage.getSubimage(x, y, croppedWidth,
                    croppedHeight);
        }
        return croppedImage;
    }

    /**
     * @param inImage Image to crop
     * @param crop Crop operation
     * @return Cropped image, or the input image if the given operation is a
     * no-op.
     */
    public static RenderedOp cropImage(RenderedOp inImage, Crop crop) {
        return cropImage(inImage, crop, 0);
    }

    /**
     * Crops the given image taking into account a reduction factor
     * (<code>reductionFactor</code>). In other words, the dimensions of the
     * input image have already been halved <code>reductionFactor</code> times
     * but the given region is relative to the full-sized image.
     *
     * @param inImage Image to crop
     * @param crop Crop operation
     * @param reductionFactor Number of times the dimensions of
     *                        <code>inImage</code> have already been halved
     *                        relative to the full-sized version
     * @return Cropped image, or the input image if the given operation is a
     * no-op.
     */
    public static RenderedOp cropImage(RenderedOp inImage,
                                       Crop crop,
                                       int reductionFactor) {
        RenderedOp croppedImage = inImage;
        if (!crop.isNoOp()) {
            // calculate the region x, y, and actual width/height
            final double scale = getScale(reductionFactor);
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
    public static BufferedImage filterImage(BufferedImage inImage,
                                            Filter filter) {
        BufferedImage filteredImage = inImage;
        switch (filter) {
            case GRAY:
                filteredImage = new BufferedImage(inImage.getWidth(),
                        inImage.getHeight(),
                        BufferedImage.TYPE_BYTE_GRAY);
                break;
            case BITONAL:
                filteredImage = new BufferedImage(inImage.getWidth(),
                        inImage.getHeight(),
                        BufferedImage.TYPE_BYTE_BINARY);
                break;
        }
        if (filteredImage != inImage) {
            Graphics2D g2d = filteredImage.createGraphics();
            g2d.drawImage(inImage, 0, 0, null);
        }
        return filteredImage;
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
     * Gets a reduction factor where the corresponding scale is 1/(2^rf).
     *
     * @param scalePercent Scale percentage between 0 and 1
     * @param maxFactor 0 for no max
     * @return
     * @see #getScale
     */
    public static int getReductionFactor(double scalePercent, int maxFactor) {
        if (maxFactor == 0) {
            maxFactor = 999999;
        }
        short factor = 0;
        double nextPct = 0.5f;
        while (scalePercent <= nextPct && factor < maxFactor) {
            nextPct /= 2.0f;
            factor++;
        }
        return factor;
    }

    /**
     * @param reductionFactor Reduction factor (0 for no reduction)
     * @return Scale corresponding to the given reduction factor (1/(2^rf)).
     * @see #getReductionFactor
     */
    public static double getScale(int reductionFactor) {
        double scale = 1f;
        for (int i = 0; i < reductionFactor; i++) {
            scale /= 2;
        }
        return scale;
    }

    /**
     * Efficiently reads the width & height of an image without reading the
     * entire image into memory.
     *
     * @param inputFile
     * @param sourceFormat
     * @return Dimensions in pixels
     * @throws ProcessorException
     */
    public static Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException {
        return doGetSize(inputFile, sourceFormat);
    }

    /**
     * Efficiently reads the width & height of an image without reading the
     * entire image into memory.
     *
     * @param inputStream
     * @param sourceFormat
     * @return Dimensions in pixels
     * @throws ProcessorException
     */
    public static Dimension getSize(InputStream inputStream,
                                    SourceFormat sourceFormat)
            throws ProcessorException {
        return doGetSize(inputStream, sourceFormat);
    }

    /**
     * @param input Object that can be passed to
     * {@link ImageIO#createImageInputStream(Object)}
     * @param sourceFormat
     * @return
     * @throws ProcessorException
     */
    private static Dimension doGetSize(Object input, SourceFormat sourceFormat)
            throws ProcessorException {
        Iterator<ImageReader> iter = ImageIO.
                getImageReadersBySuffix(sourceFormat.getPreferredExtension());
        if (iter.hasNext()) {
            ImageReader reader = iter.next();
            int width, height;
            try {
                reader.setInput(ImageIO.createImageInputStream(input));
                width = reader.getWidth(reader.getMinIndex());
                height = reader.getHeight(reader.getMinIndex());
            } catch (IOException e) {
                throw new ProcessorException(e.getMessage(), e);
            } finally {
                reader.dispose();
            }
            return new Dimension(width, height);
        }
        return null;
    }

    /**
     * @return Set of all output formats supported by ImageIO.
     */
    public static Set<OutputFormat> imageIoOutputFormats() {
        final String[] writerMimeTypes = ImageIO.getWriterMIMETypes();
        final Set<OutputFormat> outputFormats = new HashSet<>();
        for (OutputFormat outputFormat : OutputFormat.values()) {
            for (String mimeType : writerMimeTypes) {
                if (outputFormat.getMediaType().equals(mimeType.toLowerCase())) {
                    outputFormats.add(outputFormat);
                }
            }
        }
        return outputFormats;
    }

    public static BufferedImage readImage(ReadableByteChannel readableChannel)
            throws IOException {
        return ProcessorUtil.convertToRgb(ImageIO.read(
                ImageIO.createImageInputStream(readableChannel)));
    }

    public static BufferedImage readImage(File inputFile,
                                          SourceFormat sourceFormat,
                                          OperationList ops,
                                          Dimension fullSize,
                                          ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return doReadImageWithImageIo(inputFile, sourceFormat, ops, fullSize,
                reductionFactor);
    }

    public static BufferedImage readImage(InputStream inputStream,
                                          SourceFormat sourceFormat,
                                          OperationList ops,
                                          Dimension fullSize,
                                          ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return doReadImageWithImageIo(inputStream, sourceFormat, ops, fullSize,
                reductionFactor);
    }

    /**
     * @param readableChannel
     * @return
     */
    public static RenderedImage readImageWithJai(
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
    public static RenderedImage readImageWithJai(File inputFile,
                                                 SourceFormat sourceFormat,
                                                 OperationList ops,
                                                 Dimension fullSize,
                                                 ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return doReadImageWithJai(inputFile, sourceFormat, ops, fullSize,
                reductionFactor);
    }

    /**
     * @param inputStream
     * @param sourceFormat
     * @param ops
     * @param fullSize
     * @param reductionFactor
     * @return The read image. Use {@link #reformatImage} to convert into a
     * RenderedOp.
     * @throws IOException
     * @throws ProcessorException
     */
    public static RenderedImage readImageWithJai(InputStream inputStream,
                                                 SourceFormat sourceFormat,
                                                 OperationList ops,
                                                 Dimension fullSize,
                                                 ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        return doReadImageWithJai(inputStream, sourceFormat, ops, fullSize,
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
     * @throws ProcessorException
     */
    private static BufferedImage doReadImageWithImageIo(
            Object inputSource,
            SourceFormat sourceFormat,
            OperationList ops,
            Dimension fullSize,
            ReductionFactor reductionFactor)
            throws IOException, ProcessorException {
        BufferedImage image = null;
        switch (sourceFormat) {
            case BMP:
                ImageInputStream iis = ImageIO.createImageInputStream(inputSource);
                image = ImageIO.read(iis);
                break;
            case TIF:
                String tiffReader = Application.getConfiguration().
                        getString(Java2dProcessor.TIF_READER_CONFIG_KEY,
                                "TIFFImageReader");
                if (tiffReader.equals("TIFFImageReader")) {
                    image = readWithTiffImageReader(inputSource, ops,
                            fullSize, reductionFactor);
                } else {
                    RenderedImage ri = readWithTiffImageDecoder(inputSource,
                            ops, fullSize, reductionFactor);
                    image = new BufferedImage(ri.getWidth(),
                            ri.getHeight(), BufferedImage.TYPE_INT_RGB);
                    image.setData(ri.getData());
                }
                break;
            default:
                Iterator<ImageReader> it = ImageIO.getImageReadersByMIMEType(
                        sourceFormat.getPreferredMediaType().toString());
                while (it.hasNext()) {
                    ImageReader reader = it.next();
                    try {
                        iis = ImageIO.createImageInputStream(inputSource);
                        reader.setInput(iis);
                        image = reader.read(0);
                    } finally {
                        reader.dispose();
                    }
                }
                break;
        }
        if (image == null) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        }
        BufferedImage rgbImage = ProcessorUtil.convertToRgb(image);
        if (rgbImage != image) {
            logger.warn("Converting {} to RGB (this is very expensive)",
                    ops.getIdentifier());
        }
        return rgbImage;
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
    private static RenderedImage doReadImageWithJai(Object inputSource,
                                                    SourceFormat sourceFormat,
                                                    OperationList ops,
                                                    Dimension fullSize,
                                                    ReductionFactor reductionFactor)
            throws IOException, UnsupportedSourceFormatException {
        RenderedImage image;
        switch (sourceFormat) {
            case TIF:
                image = readWithTiffImageDecoder(inputSource, ops, fullSize,
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
    private static RenderedImage readWithTiffImageDecoder(
            Object inputSource, OperationList ops, Dimension fullSize,
            ReductionFactor reductionFactor) throws IOException {
        RenderedImage image = null;
        try {
            ImageDecoder dec;
            if (inputSource instanceof InputStream) {
                dec = ImageCodec.createImageDecoder("tiff",
                        (InputStream) inputSource, null);
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
     * <p>Uses TIFFImageReader to load a TIFF.</p>
     *
     * <p>The TIFFImageReader class currently being used,
     * it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader, has several
     * issues:</p>
     *
     * <ul>
     *     <li>It sometimes sets BufferedImages to <code>TYPE_CUSTOM</code>,
     *     which necessitates an expensive redraw into a new BufferedImage of
     *     <code>TYPE_RGB</code>. Though, we seem to be able to work around
     *     this (see inline commentary in
     *     {@link #getSmallestUsableImage}).</li>
     *     <li>It throws an ArrayIndexOutOfBoundsException when a TIFF file
     *     contains a tag value greater than 6. (To inspect tag values, run
     *     <code>$ tiffdump &lt;file&gt;</code>.) (The Sun TIFFImageReader
     *     suffers from the same issue except it throws an
     *     IllegalArgumentException instead.)</li>
     *     <li>It renders some TIFFs with improper colors.</li>
     * </ul>
     *
     * <p><code>ImageIO.read()</code> would be an alternative, but it is not
     * usable because it also suffers from the <code>TYPE_CUSTOM</code> issue.
     * Also, it doesn't allow access to pyramidal TIFF sub-images, and just
     * generally doesn't provide any control over the reading process.</p>
     *
     * @param inputSource {@link InputStream} or {@link File}
     * @param ops
     * @param fullSize
     * @param reductionFactor
     * @return
     * @throws IOException
     * @throws ProcessorException
     * @throws IllegalArgumentException
     * @see <a href="https://github.com/geosolutions-it/imageio-ext/blob/master/plugin/tiff/src/main/java/it/geosolutions/imageioimpl/plugins/tiff/TIFFImageReader.java">
     *     TIFFImageReader source</a>
     */
    private static BufferedImage readWithTiffImageReader(
            Object inputSource, OperationList ops, Dimension fullSize,
            ReductionFactor reductionFactor) throws IOException,
            ProcessorException {
        BufferedImage image = null;
        try {
            Iterator<ImageReader> it = ImageIO.
                    getImageReadersByMIMEType("image/tiff");
            ImageReader reader = it.next();
            // TODO: figure out why it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader is not found in the packaged jar
            try {
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
                ImageInputStream iis = ImageIO.createImageInputStream(inputSource);
                reader.setInput(iis);
                image = getSmallestUsableImage(reader, fullSize, crop, scale,
                        reductionFactor);
            } finally {
                reader.dispose();
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("TIFFImageReader failed to read {}",
                    ops.getIdentifier());
            throw e;
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
                logger.debug("Detected pyramidal TIFF with {} levels",
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
                        rf.factor = ProcessorUtil.getReductionFactor(tileScale, 0);
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
     * Returns the smallest image fitting the requested size from the given
     * reader. Useful for e.g. pyramidal TIFF.
     *
     * @param reader ImageReader with input source already set
     * @param fullSize
     * @param crop Requested crop
     * @param scale Requested scale
     * @param rf Set by reference
     * @return
     * @throws IOException
     */
    private static BufferedImage getSmallestUsableImage(ImageReader reader,
                                                        Dimension fullSize,
                                                        Crop crop,
                                                        Scale scale,
                                                        ReductionFactor rf)
            throws IOException {
        // The goal here is to get a BufferedImage of TYPE_INT_RGB rather
        // than TYPE_CUSTOM, which would need to be redrawn into a new
        // BufferedImage of TYPE_INT_RGB at huge expense. For an explanation of
        // this strategy:
        // https://lists.apple.com/archives/java-dev/2005/Apr/msg00456.html
        ImageReadParam param = reader.getDefaultReadParam();
        BufferedImage bestImage = new BufferedImage(fullSize.width,
                fullSize.height, BufferedImage.TYPE_INT_RGB);
        param.setDestination(bestImage);
        // An alternative would apparently be to use setDestinationType() and
        // then allow ImageReader.read() to create the BufferedImage itself.
        // But, that results in, "Destination type from ImageReadParam does not
        // match!" during writing.
        // param.setDestinationType(ImageTypeSpecifier.
        //        createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
        reader.read(0, param);
        if (!scale.isNoOp()) {
            // Pyramidal TIFFs will have > 1 image, each half the dimensions of
            // the next larger. The "true" parameter tells getNumImages() to
            // scan for images, which seems to be necessary for at least some
            // files, but is a little bit expensive.
            int numImages = reader.getNumImages(false);
            if (numImages == -1) {
                numImages = reader.getNumImages(true);
            }
            if (numImages > 1) {
                logger.debug("Detected pyramidal TIFF with {} levels",
                        numImages);
                final Rectangle regionRect = crop.getRectangle(fullSize);

                // Loop through the tiles from smallest to largest to find the
                // first one that fits the requested scale
                for (int i = numImages - 1; i >= 0; i--) {
                    final BufferedImage tile = reader.read(i);
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
                        rf.factor = ProcessorUtil.getReductionFactor(tileScale, 0);
                        logger.debug("Using a {}x{} source tile ({}x reduction factor)",
                                tile.getWidth(), tile.getHeight(), rf.factor);
                        bestImage = tile;
                        break;
                    }
                }
            }
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
     * @return Rotated image, or the input image if the given rotation is a
     * no-op.
     */
    public static BufferedImage rotateImage(BufferedImage inImage,
                                            Rotate rotate) {
        BufferedImage rotatedImage = inImage;
        if (!rotate.isNoOp()) {
            double radians = Math.toRadians(rotate.getDegrees());
            int sourceWidth = inImage.getWidth();
            int sourceHeight = inImage.getHeight();
            int canvasWidth = (int) Math.round(Math.abs(sourceWidth *
                    Math.cos(radians)) + Math.abs(sourceHeight *
                    Math.sin(radians)));
            int canvasHeight = (int) Math.round(Math.abs(sourceHeight *
                    Math.cos(radians)) + Math.abs(sourceWidth *
                    Math.sin(radians)));

            // note: operations happen in reverse order of declaration
            AffineTransform tx = new AffineTransform();
            // 3. translate the image to the center of the "canvas"
            tx.translate(canvasWidth / 2, canvasHeight / 2);
            // 2. rotate it
            tx.rotate(radians);
            // 1. translate the image so that it is rotated about the center
            tx.translate(-sourceWidth / 2, -sourceHeight / 2);

            rotatedImage = new BufferedImage(canvasWidth, canvasHeight,
                    inImage.getType());
            Graphics2D g2d = rotatedImage.createGraphics();
            RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHints(hints);
            g2d.drawImage(inImage, tx, null);
        }
        return rotatedImage;
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
        return scaleImage(inImage, scale, 0);
    }

    /**
     * Scales an image using JAI, taking an already-applied reduction factor
     * into account. (In other words, the dimensions of the input image have
     * already been halved <code>reductionFactor</code> times but the given
     * size is relative to the full-sized image.)
     *
     * @param inImage Image to scale
     * @param scale Requested size ignoring any reduction factor
     * @param reductionFactor Reduction factor that has already been applied to
     *                        <code>inImage</code>
     * @return Scaled image, or the input image if the given scale is a no-op.
     */
    public static RenderedOp scaleImage(RenderedOp inImage, Scale scale,
                                        int reductionFactor) {
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
                int reqRf = getReductionFactor(scale.getPercent(), 0);
                xScale = yScale = getScale(reqRf - reductionFactor);
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
     * Scales an image using an AffineTransform.
     *
     * @param inImage
     * @param scale
     * @return
     */
    public static BufferedImage scaleImageWithAffineTransform(
            BufferedImage inImage, Scale scale) {
        BufferedImage scaledImage = inImage;
        if (!scale.isNoOp()) {
            double xScale = 0.0f, yScale = 0.0f;
            if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                xScale = scale.getWidth() / (double) inImage.getWidth();
                yScale = xScale;
            } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                yScale = scale.getHeight() / (double) inImage.getHeight();
                xScale = yScale;
            } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                xScale = scale.getWidth() / (double) inImage.getWidth();
                yScale = scale.getHeight() / (double) inImage.getHeight();
            } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                double hScale = (double) scale.getWidth() /
                        (double) inImage.getWidth();
                double vScale = (double) scale.getHeight() /
                        (double) inImage.getHeight();
                xScale = inImage.getWidth() * Math.min(hScale, vScale) / 100f;
                yScale = inImage.getHeight() * Math.min(hScale, vScale) / 100f;
            } else if (scale.getPercent() != null) {
                xScale = scale.getPercent();
                yScale = xScale;
            }
            int width = (int) Math.round(inImage.getWidth() * xScale);
            int height = (int) Math.round(inImage.getHeight() * yScale);
            scaledImage = new BufferedImage(width, height, inImage.getType());
            AffineTransform at = new AffineTransform();
            at.scale(xScale, yScale);
            AffineTransformOp scaleOp = new AffineTransformOp(at,
                    AffineTransformOp.TYPE_BILINEAR);
            scaledImage = scaleOp.filter(inImage, scaledImage);
        }
        return scaledImage;
    }

    /**
     * Scales an image using Graphics2D.
     *
     * @param inImage
     * @param scale
     * @return
     */
    public static BufferedImage scaleImageWithG2d(BufferedImage inImage,
                                                  Scale scale) {
        return scaleImageWithG2d(inImage, scale, 0, false);
    }

    /**
     * Scales an image using Graphics2D.
     *
     * @param inImage
     * @param scale
     * @param highQuality
     * @return
     */
    public static BufferedImage scaleImageWithG2d(BufferedImage inImage,
                                                  Scale scale,
                                                  boolean highQuality) {
        return scaleImageWithG2d(inImage, scale, 0, highQuality);
    }

    /**
     * Scales an image using Graphics2D, taking an already-applied reduction
     * factor into account. In other words, the dimensions of the input image
     * have already been halved rf times but the given size is relative to the
     * full-sized image.
     *
     * @param inImage The input image
     * @param scale Requested size ignoring any reduction factor
     * @param reductionFactor Reduction factor that has already been applied to
     *                        <code>inImage</code>
     * @param highQuality
     * @return Scaled image
     */
    public static BufferedImage scaleImageWithG2d(BufferedImage inImage,
                                                  Scale scale,
                                                  int reductionFactor,
                                                  boolean highQuality) {
        BufferedImage scaledImage = inImage;
        if (!scale.isNoOp()) {
            final int sourceWidth = inImage.getWidth();
            final int sourceHeight = inImage.getHeight();
            int width = 0, height = 0;
            if (scale.getMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                width = scale.getWidth();
                height = sourceHeight * width / sourceWidth;
            } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                height = scale.getHeight();
                width = sourceWidth * height / sourceHeight;
            } else if (scale.getMode() == Scale.Mode.NON_ASPECT_FILL) {
                width = scale.getWidth();
                height = scale.getHeight();
            } else if (scale.getMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                double hScale = (double) scale.getWidth() / (double) sourceWidth;
                double vScale = (double) scale.getHeight() / (double) sourceHeight;
                width = (int) Math.round(sourceWidth *
                        Math.min(hScale, vScale));
                height = (int) Math.round(sourceHeight *
                        Math.min(hScale, vScale));
            } else if (scale.getPercent() != null) {
                double reqScale = scale.getPercent();
                int reqRf = getReductionFactor(reqScale, 0);
                double pct = getScale(reqRf - reductionFactor);
                width = (int) Math.round(sourceWidth * pct);
                height = (int) Math.round(sourceHeight * pct);
            }
            scaledImage = new BufferedImage(width, height,
                    inImage.getType());

            final Graphics2D g2d = scaledImage.createGraphics();
            // The "non-high-quality" technique results in images that are
            // noticeably pixelated.
            // See: https://community.oracle.com/docs/DOC-983611
            // http://stackoverflow.com/a/34266703/177529
            if (highQuality) {
                g2d.drawImage(
                        inImage.getScaledInstance(width, height, Image.SCALE_SMOOTH),
                        0, 0, width, height, null);
            } else {
                final RenderingHints hints = new RenderingHints(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHints(hints);
                g2d.drawImage(inImage, 0, 0, width, height, null);
            }
            g2d.dispose();
        }
        return scaledImage;
    }

    /**
     * @param inImage Image to transpose.
     * @param transpose The transpose operation.
     * @return Transposed image, or the input image if the given transpose
     * operation is a no-op.
     */
    public static BufferedImage transposeImage(BufferedImage inImage,
                                               Transpose transpose) {
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        switch (transpose) {
            case HORIZONTAL:
                tx.translate(-inImage.getWidth(null), 0);
                break;
            case VERTICAL:
                tx.translate(0, -inImage.getHeight(null));
                break;
        }
        AffineTransformOp op = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_BILINEAR);
        return op.filter(inImage, null);
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
    public static void writeImage(BufferedImage image,
                                  OutputFormat outputFormat,
                                  WritableByteChannel writableChannel)
            throws IOException {
        switch (outputFormat) {
            case JPG:
                // JPEG doesn't support alpha, so convert to RGB or else the
                // client will interpret as CMYK
                if (image.getColorModel().hasAlpha()) {
                    logger.warn("Converting RGBA BufferedImage to RGB (this is very expensive)");
                    BufferedImage rgbImage = new BufferedImage(
                            image.getWidth(), image.getHeight(),
                            BufferedImage.TYPE_INT_RGB);
                    rgbImage.createGraphics().drawImage(image, null, 0, 0);
                    image = rgbImage;
                }
                // TurboJpegImageWriter is used automatically if libjpeg-turbo
                // is available in java.library.path:
                // https://github.com/geosolutions-it/imageio-ext/wiki/TurboJPEG-plugin
                Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
                ImageWriter writer = (ImageWriter) iter.next();
                try {
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(Application.getConfiguration().
                            getFloat(Java2dProcessor.JPG_QUALITY_CONFIG_KEY, 0.7f));
                    param.setCompressionType("JPEG");
                    ImageOutputStream os = ImageIO.createImageOutputStream(writableChannel);
                    writer.setOutput(os);
                    IIOImage iioImage = new IIOImage(image, null, null);
                    writer.write(null, iioImage, param);
                } finally {
                    writer.dispose();
                }
                break;
            /*case PNG: // an alternative in case ImageIO.write() ever causes problems
                writer = ImageIO.getImageWritersByFormatName("png").next();
                ImageOutputStream os = ImageIO.createImageOutputStream(writableChannel);
                writer.setOutput(os);
                writer.write(image);
                break;*/
          /*  case TIF: TODO: this doesn't write anything
                Iterator<ImageWriter> it = ImageIO.
                        getImageWritersByMIMEType("image/tiff");
                while (it.hasNext()) {
                    writer = it.next();
                    if (writer instanceof it.geosolutions.imageioimpl.
                            plugins.tiff.TIFFImageWriter) {
                        try {
                            ImageWriteParam param = writer.getDefaultWriteParam();
                            param.setDestinationType(ImageTypeSpecifier.
                                    createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
                            ImageOutputStream os = ImageIO.createImageOutputStream(writableChannel);
                            writer.setOutput(os);
                            IIOImage iioImage = new IIOImage(image, null, null);
                            writer.write(null, iioImage, param);
                        } finally {
                            writer.dispose();
                        }
                        break;
                    }
                }
                break; */
            default:
                // TODO: jp2 doesn't seem to work
                ImageIO.write(image, outputFormat.getExtension(),
                        ImageIO.createImageOutputStream(writableChannel));
                break;
        }
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
                // JPEGImageEncoder seems to be slightly more efficient than
                // ImageIO.write()
                JPEGEncodeParam jParam = new JPEGEncodeParam();
                ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG",
                        Channels.newOutputStream(writableChannel), jParam);
                encoder.encode(image);
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
