package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Quality;
import edu.illinois.library.cantaloupe.image.Region;
import edu.illinois.library.cantaloupe.image.Rotation;
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
                                          Region region) {
        return cropImage(inImage, region, 0);
    }

    /**
     * Crops the given image taking into account a reduction factor (rf). In
     * other words, the dimensions of the input image have already been halved
     * rf times but the given region is relative to the full-sized image.
     *
     * @param inImage
     * @param region
     * @param reductionFactor
     * @return
     */
    public static BufferedImage cropImage(BufferedImage inImage,
                                          Region region, int reductionFactor) {
        BufferedImage croppedImage;
        if (region.isFull()) {
            croppedImage = inImage;
        } else {
            final double scale = getScale(reductionFactor);
            final double regionX = region.getX() * scale;
            final double regionY = region.getY() * scale;
            final double regionWidth = region.getWidth() * scale;
            final double regionHeight = region.getHeight() * scale;

            int x, y, requestedWidth, requestedHeight, croppedWidth,
                    croppedHeight;
            if (region.isPercent()) {
                x = (int) Math.round((regionX / 100.0) * inImage.getWidth());
                y = (int) Math.round((regionY / 100.0) * inImage.getHeight());
                requestedWidth = (int) Math.round((regionWidth / 100.0) *
                        inImage.getWidth());
                requestedHeight = (int) Math.round((regionHeight / 100.0) *
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
                                            Quality quality) {
        BufferedImage filteredImage = inImage;
        if (quality != Quality.COLOR && quality != Quality.DEFAULT) {
            switch (quality) {
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
            Graphics2D g2d = filteredImage.createGraphics();
            g2d.drawImage(inImage, 0, 0, null);
        }
        return filteredImage;
    }

    @SuppressWarnings({"deprecation"}) // really, JAI itself is basically deprecated
    public static RenderedOp filterImage(RenderedOp inImage,
                                         Quality quality) {
        RenderedOp filteredImage = inImage;
        if (quality != Quality.COLOR && quality != Quality.DEFAULT) {
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
            if (quality == Quality.BITONAL) {
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
                                         Rotation rotation) {
        // do mirroring
        RenderedOp mirroredImage = inImage;
        if (rotation.shouldMirror()) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inImage);
            pb.add(TransposeDescriptor.FLIP_HORIZONTAL);
            mirroredImage = JAI.create("transpose", pb);
        }
        // do rotation
        RenderedOp rotatedImage = mirroredImage;
        if (rotation.getDegrees() > 0) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(rotatedImage);
            pb.add(mirroredImage.getWidth() / 2.0f);
            pb.add(mirroredImage.getHeight() / 2.0f);
            pb.add((float) Math.toRadians(rotation.getDegrees()));
            pb.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
            rotatedImage = JAI.create("rotate", pb);
        }
        return rotatedImage;
    }

    public static BufferedImage rotateImage(BufferedImage inImage,
                                            Rotation rotation) {
        // do mirroring
        BufferedImage mirroredImage = inImage;
        if (rotation.shouldMirror()) {
            AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
            tx.translate(-mirroredImage.getWidth(null), 0);
            AffineTransformOp op = new AffineTransformOp(tx,
                    AffineTransformOp.TYPE_BILINEAR);
            mirroredImage = op.filter(inImage, null);
        }
        // do rotation
        BufferedImage rotatedImage = mirroredImage;
        if (rotation.getDegrees() > 0) {
            double radians = Math.toRadians(rotation.getDegrees());
            int sourceWidth = mirroredImage.getWidth();
            int sourceHeight = mirroredImage.getHeight();
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
            g2d.drawImage(mirroredImage, tx, null);
        }
        return rotatedImage;
    }

    public static RenderedOp scaleImage(RenderedOp inImage, Scale size) {
        RenderedOp scaledImage;
        if (size.getScaleMode() == Scale.Mode.FULL) {
            scaledImage = inImage;
        } else {
            final double sourceWidth = inImage.getWidth();
            final double sourceHeight = inImage.getHeight();
            double xScale = 1.0f;
            double yScale = 1.0f;
            if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                xScale = yScale = size.getWidth() / sourceWidth;
            } else if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                xScale = yScale = size.getHeight() / sourceHeight;
            } else if (size.getScaleMode() == Scale.Mode.NON_ASPECT_FILL) {
                xScale = size.getWidth() / sourceWidth;
                yScale = size.getHeight() / sourceHeight;
            } else if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                double hScale = size.getWidth() / sourceWidth;
                double vScale = size.getHeight() / sourceHeight;
                xScale = (sourceWidth * Math.min(hScale, vScale));
                yScale = (sourceHeight * Math.min(hScale, vScale));
            } else if (size.getPercent() != null) {
                xScale = yScale = size.getPercent() / 100.0f;
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
     * @param size Requested size ignoring any reduction factor
     * @param reductionFactor Reduction factor that has already been applied to
     *                        <code>inImage</code>
     * @return Scaled image
     */
    public static RenderedOp scaleImage(PlanarImage inImage, Scale size,
                                        int reductionFactor) {
        RenderedOp scaledImage;
        if (size.getScaleMode() == Scale.Mode.FULL) {
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
            if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                xScale = yScale = size.getWidth() / sourceWidth;
            } else if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                xScale = yScale = size.getHeight() / sourceHeight;
            } else if (size.getScaleMode() == Scale.Mode.NON_ASPECT_FILL) {
                xScale = size.getWidth() / sourceWidth;
                yScale = size.getHeight() / sourceHeight;
            } else if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                double hScale = size.getWidth() / sourceWidth;
                double vScale = size.getHeight() / sourceHeight;
                xScale = sourceWidth * Math.min(hScale, vScale);
                yScale = sourceHeight * Math.min(hScale, vScale);
            } else if (size.getPercent() != null) {
                double reqScale = size.getPercent() / 100.0f;
                int reqRf = getReductionFactor(reqScale, 0);
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
     * @param size
     * @return
     */
    public static BufferedImage scaleImageWithAffineTransform(
            BufferedImage inImage, Scale size) {
        BufferedImage scaledImage;
        if (size.getScaleMode() == Scale.Mode.FULL) {
            scaledImage = inImage;
        } else {
            double xScale = 0.0f, yScale = 0.0f;
            if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                xScale = size.getWidth() / (double) inImage.getWidth();
                yScale = xScale;
            } else if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                yScale = size.getHeight() / (double) inImage.getHeight();
                xScale = yScale;
            } else if (size.getScaleMode() == Scale.Mode.NON_ASPECT_FILL) {
                xScale = size.getWidth() / (double) inImage.getWidth();
                yScale = size.getHeight() / (double) inImage.getHeight();
            } else if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                double hScale = (double) size.getWidth() /
                        (double) inImage.getWidth();
                double vScale = (double) size.getHeight() /
                        (double) inImage.getHeight();
                xScale = inImage.getWidth() * Math.min(hScale, vScale);
                yScale = inImage.getHeight() * Math.min(hScale, vScale);
            } else if (size.getPercent() != null) {
                xScale = size.getPercent() / 100.0;
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
     * @param size
     * @return
     */
    public static BufferedImage scaleImageWithG2d(BufferedImage inImage,
                                                  Scale size) {
        return scaleImageWithG2d(inImage, size, 0);
    }

    /**
     * Scales an image using Graphics2D, taking an already-applied reduction
     * factor into account. In other words, the dimensions of the input image
     * have already been halved rf times but the given size is relative to the
     * full-sized image.
     *
     * @param inImage The input image
     * @param size Requested size ignoring any reduction factor
     * @param reductionFactor Reduction factor that has already been applied to
     *                        <code>inImage</code>
     * @return Scaled image
     */
    public static BufferedImage scaleImageWithG2d(BufferedImage inImage,
                                                  Scale size,
                                                  int reductionFactor) {
        BufferedImage scaledImage;
        if (size.getScaleMode() == Scale.Mode.FULL) {
            scaledImage = inImage;
        } else {
            final int sourceWidth = inImage.getWidth();
            final int sourceHeight = inImage.getHeight();
            int width = 0, height = 0;
            if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_WIDTH) {
                width = size.getWidth();
                height = sourceHeight * width / sourceWidth;
            } else if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_HEIGHT) {
                height = size.getHeight();
                width = sourceWidth * height / sourceHeight;
            } else if (size.getScaleMode() == Scale.Mode.NON_ASPECT_FILL) {
                width = size.getWidth();
                height = size.getHeight();
            } else if (size.getScaleMode() == Scale.Mode.ASPECT_FIT_INSIDE) {
                double hScale = (double) size.getWidth() / (double) sourceWidth;
                double vScale = (double) size.getHeight() / sourceHeight;
                width = (int) Math.round(sourceWidth *
                        Math.min(hScale, vScale));
                height = (int) Math.round(sourceHeight *
                        Math.min(hScale, vScale));
            } else if (size.getPercent() != null) {
                double reqScale = size.getPercent() / 100.0f;
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

}
