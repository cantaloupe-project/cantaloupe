package edu.illinois.library.cantaloupe.processor;

import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;
import edu.illinois.library.cantaloupe.Application;
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
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.TransposeDescriptor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A collection of helper methods.
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

    public static BufferedImage cropImage(BufferedImage inImage,
                                          Crop crop) {
        return cropImage(inImage, crop, 0);
    }

    /**
     * Crops the given image taking into account a reduction factor (rf). In
     * other words, the dimensions of the input image have already been halved
     * rf times but the given region is relative to the full-sized image.
     *
     * @param inImage
     * @param crop
     * @param reductionFactor
     * @return
     */
    public static BufferedImage cropImage(BufferedImage inImage,
                                          Crop crop, int reductionFactor) {
        BufferedImage croppedImage;
        if (crop.isFull()) {
            croppedImage = inImage;
        } else {
            final double scale = getScale(reductionFactor);
            final double regionX = crop.getX() * scale;
            final double regionY = crop.getY() * scale;
            final double regionWidth = crop.getWidth() * scale;
            final double regionHeight = crop.getHeight() * scale;

            int x, y, requestedWidth, requestedHeight, croppedWidth,
                    croppedHeight;
            if (crop.isPercent()) {
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
            croppedImage = inImage.getSubimage(x, y, croppedWidth, croppedHeight);
        }
        return croppedImage;
    }

    public static RenderedOp cropImage(RenderedOp inImage, Crop crop) {
        RenderedOp croppedImage;
        if (crop.isFull()) {
            croppedImage = inImage;
        } else {
            // calculate the region x, y, and actual width/height
            float x, y, requestedWidth, requestedHeight, actualWidth, actualHeight;
            if (crop.isPercent()) {
                x = crop.getX() * inImage.getWidth();
                y = crop.getY() * inImage.getHeight();
                requestedWidth = crop.getWidth() * inImage.getWidth();
                requestedHeight = crop.getHeight() * inImage.getHeight();
            } else {
                x = crop.getX();
                y = crop.getY();
                requestedWidth = crop.getWidth();
                requestedHeight = crop.getHeight();
            }
            actualWidth = (x + requestedWidth > inImage.getWidth()) ?
                    inImage.getWidth() - x : requestedWidth;
            actualHeight = (y + requestedHeight > inImage.getHeight()) ?
                    inImage.getHeight() - y : requestedHeight;

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add(x);
            pb.add(y);
            pb.add(actualWidth);
            pb.add(actualHeight);
            croppedImage = JAI.create("crop", pb);
        }
        return croppedImage;
    }

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

    @SuppressWarnings({"deprecation"}) // really, JAI itself is basically deprecated
    public static RenderedOp filterImage(RenderedOp inImage,
                                         Filter filter) {
        RenderedOp filteredImage = inImage;
        if (filter != Filter.NONE) {
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

    public static Set<OutputFormat> imageIoOutputFormats() {
        final String[] writerMimeTypes = ImageIO.getWriterMIMETypes();
        Set<OutputFormat> outputFormats = new HashSet<>();
        for (OutputFormat outputFormat : OutputFormat.values()) {
            for (String mimeType : writerMimeTypes) {
                if (outputFormat.getMediaType().equals(mimeType.toLowerCase())) {
                    outputFormats.add(outputFormat);
                }
            }
        }
        return outputFormats;
    }

    public static RenderedOp rotateImage(RenderedOp inImage,
                                         Rotate rotate) {
        RenderedOp rotatedImage = inImage;
        if (rotate.getDegrees() > 0) {
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

    public static BufferedImage rotateImage(BufferedImage inImage,
                                            Rotate rotate) {
        BufferedImage rotatedImage = inImage;
        if (rotate.getDegrees() > 0) {
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

    public static RenderedOp scaleImage(RenderedOp inImage, Scale scale) {
        RenderedOp scaledImage;
        if (scale.getMode() == Scale.Mode.FULL) {
            scaledImage = inImage;
        } else {
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
                xScale = (sourceWidth * Math.min(hScale, vScale));
                yScale = (sourceHeight * Math.min(hScale, vScale));
            } else if (scale.getPercent() != null) {
                xScale = yScale = scale.getPercent() / 100.0f;
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
     * Scales an image using JAI, taking an already-applied reduction factor
     * into account. In other words, the dimensions of the input image have
     * already been halved rf times but the given size is relative to the
     * full-sized image.
     *
     * @param inImage The input image
     * @param scale Requested size ignoring any reduction factor
     * @param reductionFactor Reduction factor that has already been applied to
     *                        <code>inImage</code>
     * @return Scaled image
     */
    public static RenderedOp scaleImage(PlanarImage inImage, Scale scale,
                                        int reductionFactor) {
        RenderedOp scaledImage;
        if (scale.getMode() == Scale.Mode.FULL) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add(1.0f);
            pb.add(1.0f);
            pb.add(0.0f);
            pb.add(0.0f);
            pb.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
            scaledImage = JAI.create("scale", pb);
        } else {
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
        BufferedImage scaledImage;
        if (scale.getMode() == Scale.Mode.FULL) {
            scaledImage = inImage;
        } else {
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
        return scaleImageWithG2d(inImage, scale, 0);
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
     * @return Scaled image
     */
    public static BufferedImage scaleImageWithG2d(BufferedImage inImage,
                                                  Scale scale,
                                                  int reductionFactor) {
        BufferedImage scaledImage;
        if (scale.getMode() == Scale.Mode.FULL) {
            scaledImage = inImage;
        } else {
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
            Graphics2D g2d = scaledImage.createGraphics();
            RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHints(hints);
            g2d.drawImage(inImage, 0, 0, width, height, null);
            g2d.dispose();
        }
        return scaledImage;
    }

    public static BufferedImage transposeImage(BufferedImage inImage,
                                               Transpose transpose) {
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        switch (transpose.getAxis()) {
            case HORIZONTAL:
                tx.translate(-inImage.getWidth(null), 0);
                break;
            case VERTICAL:
                tx.translate(0, -inImage.getHeight(null)); // TODO: test this
                break;
        }
        AffineTransformOp op = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_BILINEAR);
        return op.filter(inImage, null);
    }

    public static RenderedOp transposeImage(RenderedOp inImage,
                                            Transpose flip) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(inImage);
        switch (flip.getAxis()) {
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
     * @param outputStream Stream to which to write the image
     * @throws IOException
     */
    public static void writeImage(BufferedImage image,
                                  OutputFormat outputFormat,
                                  OutputStream outputStream)
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
                            getFloat(Java2dProcessor.CONFIG_KEY_JPG_QUALITY, 0.7f));
                    param.setCompressionType("JPEG");
                    ImageOutputStream os = ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(os);
                    IIOImage iioImage = new IIOImage(image, null, null);
                    writer.write(null, iioImage, param);
                } finally {
                    writer.dispose();
                }
                break;
            /*case PNG: // an alternative in case ImageIO.write() ever causes problems
                writer = ImageIO.getImageWritersByFormatName("png").next();
                ImageOutputStream os = ImageIO.createImageOutputStream(outputStream);
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
                            ImageOutputStream os = ImageIO.createImageOutputStream(outputStream);
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
                        outputStream);
                break;
        }
    }

    public static void writeImage(RenderedOp image, OutputFormat format,
                                  OutputStream outputStream)
            throws IOException {
        switch (format) {
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
                    ImageOutputStream os = ImageIO.createImageOutputStream(outputStream);
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
                    ImageOutputStream os = ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(os);
                    writer.write(null, iioImage, j2Param);
                }
                break;
            case JPG:
                // JPEGImageEncoder seems to be slightly more efficient than
                // ImageIO.write()
                JPEGEncodeParam jParam = new JPEGEncodeParam();
                ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG",
                        outputStream, jParam);
                encoder.encode(image);
                break;
            case PNG:
                // ImageIO.write() seems to be more efficient than
                // PNGImageEncoder
                ImageIO.write(image, format.getExtension(), outputStream);
                /* PNGEncodeParam pngParam = new PNGEncodeParam.RGB();
                ImageEncoder pngEncoder = ImageCodec.createImageEncoder("PNG",
                        outputStream, pngParam);
                pngEncoder.encode(image); */
                break;
            case TIF:
                // TIFFImageEncoder seems to be more efficient than
                // ImageIO.write();
                ImageEncoder tiffEnc = new TIFFImageEncoder(outputStream,
                        null);
                tiffEnc.encode(image);
                break;
        }

    }

}
