package edu.illinois.library.cantaloupe.processor;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;
import edu.illinois.library.cantaloupe.request.Rotation;
import edu.illinois.library.cantaloupe.request.Size;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Processor using the Java ImageIO framework.
 */
class ImageIoProcessor implements Processor {

    private static Logger logger = LoggerFactory.getLogger(ImageIoProcessor.class);

    private static final HashMap<SourceFormat,Set<OutputFormat>> FORMATS =
            getAvailableOutputFormats();
    private static final Set<Quality> SUPPORTED_QUALITIES = new HashSet<>();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();

    static {
        SUPPORTED_QUALITIES.add(Quality.BITONAL);
        SUPPORTED_QUALITIES.add(Quality.COLOR);
        SUPPORTED_QUALITIES.add(Quality.DEFAULT);
        SUPPORTED_QUALITIES.add(Quality.GRAY);

        SUPPORTED_FEATURES.add(ProcessorFeature.MIRRORING);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PERCENT);
        SUPPORTED_FEATURES.add(ProcessorFeature.REGION_BY_PIXELS);
        SUPPORTED_FEATURES.add(ProcessorFeature.ROTATION_ARBITRARY);
        SUPPORTED_FEATURES.add(ProcessorFeature.ROTATION_BY_90S);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_ABOVE_FULL);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_HEIGHT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_PERCENT);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH);
        SUPPORTED_FEATURES.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
    }

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by the ImageIO library.
     */
    public static HashMap<SourceFormat, Set<OutputFormat>>
    getAvailableOutputFormats() {
        final String[] readerMimeTypes = ImageIO.getReaderMIMETypes();
        final String[] writerMimeTypes = ImageIO.getWriterMIMETypes();
        final HashMap<SourceFormat,Set<OutputFormat>> map =
                new HashMap<>();
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (sourceFormat.equals(SourceFormat.TIF)) {
                // TIFF is currently disabled because it is too problematic
                // (see inline commentary in loadImage())
                continue;
            }
            Set<OutputFormat> outputFormats = new HashSet<>();
            for (int i = 0, length = readerMimeTypes.length; i < length; i++) {
                if (sourceFormat.getMediaTypes().
                        contains(new MediaType(readerMimeTypes[i].toLowerCase()))) {
                    for (OutputFormat outputFormat : OutputFormat.values()) {
                        for (i = 0, length = writerMimeTypes.length; i < length; i++) {
                            if (outputFormat.getMediaType().equals(writerMimeTypes[i].toLowerCase())) {
                                outputFormats.add(outputFormat);
                            }
                        }
                    }
                }
            }
            map.put(sourceFormat, outputFormats);
        }
        return map;
    }

    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> formats = FORMATS.get(sourceFormat);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
    }

    public Dimension getSize(ImageInputStream inputStream,
                             SourceFormat sourceFormat) throws Exception {
        // get width & height (without reading the entire image into memory)
        Iterator<ImageReader> iter = ImageIO.
                getImageReadersBySuffix(sourceFormat.getPreferredExtension());
        if (iter.hasNext()) {
            ImageReader reader = iter.next();
            int width, height;
            try {
                reader.setInput(inputStream);
                width = reader.getWidth(reader.getMinIndex());
                height = reader.getHeight(reader.getMinIndex());
            } finally {
                reader.dispose();
            }
            return new Dimension(width, height);
        }
        return null;
    }

    public Set<ProcessorFeature> getSupportedFeatures(SourceFormat sourceFormat) {
        return SUPPORTED_FEATURES;
    }

    public Set<Quality> getSupportedQualities(SourceFormat sourceFormat) {
        return SUPPORTED_QUALITIES;
    }

    public void process(Parameters params, SourceFormat sourceFormat,
                        ImageInputStream inputStream, OutputStream outputStream)
            throws Exception {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(params.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        BufferedImage image = loadImage(inputStream, sourceFormat);
        image = cropImage(image, params.getRegion());
        image = scaleImage(image, params.getSize());
        image = rotateImage(image, params.getRotation());
        image = filterImage(image, params.getQuality());
        outputImage(image, params.getOutputFormat(), outputStream);
    }

    private BufferedImage loadImage(ImageInputStream inputStream,
                                    SourceFormat sourceFormat) throws IOException {
        BufferedImage image = null;
        switch (sourceFormat) {
            case TIF:
                // Why don't we just use ImageIO.read()? Because the
                // BufferedImages it returns for TIFFs often end up with type
                // TYPE_CUSTOM, which causes many subsequent operations to fail.
                //
                // Here is strategy B: the geosolutions.it TIFFImageReader.
                // Unfortunately, this reader throws an
                // ArrayIndexOutOfBoundsException when a TIFF file contains a
                // tag value > 6. (To inspect tag values, run
                // $ tiffdump <file>.) But, it might be more memory-efficient
                // than strategy C, below, when it works.
                //
                // The Sun TIFFImageReader suffers from the same issue except it
                // throws an IllegalArgumentException instead.
                try {
                    inputStream.mark();
                    Iterator<ImageReader> it = ImageIO.
                            getImageReadersByMIMEType("image/tiff");
                    while (it.hasNext()) {
                        ImageReader reader = it.next();
                        if (!(reader instanceof it.geosolutions.imageioimpl.
                                plugins.tiff.TIFFImageReader)) {
                            continue;
                        }
                        try {
                            reader.setInput(inputStream);
                            image = reader.read(0);
                        } finally {
                            reader.dispose();
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    // ImageDecoder is our strategy C. It eats RAM like candy
                    // and never frees it, so it is basically unusable, but
                    // this will at least enable us to crank out a few images
                    // before collapsing.
                    logger.warn("Falling back to TIFF decode strategy B (get " +
                            "ready for an OutOfMemoryError!)");
                    inputStream.reset();
                    try (InputStream is = new ImageInputStreamWrapper(inputStream)) {
                        ImageDecoder dec = ImageCodec.createImageDecoder("tiff",
                                is, null);
                        RenderedImage op = dec.decodeAsRenderedImage();
                        BufferedImage rgbImage = new BufferedImage(op.getWidth(),
                                op.getHeight(), BufferedImage.TYPE_INT_RGB);
                        rgbImage.setData(op.getData());
                        image = rgbImage;
                    }
                }
                break;
            default:
                // Again, why can't we just use ImageIO.read()? Because it
                // leaks memory in a major way.
                Iterator<ImageReader> it = ImageIO.getImageReaders(inputStream);
                while (it.hasNext()) {
                    ImageReader reader = it.next();
                    try {
                        reader.setInput(inputStream);
                        image = reader.read(0);
                    } finally {
                        reader.dispose();
                    }
                }
                if (image == null) {
                    throw new UnsupportedSourceFormatException(sourceFormat);
                }
                // TYPE_CUSTOM won't work with various operations, so copy into a
                // new image of the correct type. (This is extremely expensive)
                if (image.getType() == BufferedImage.TYPE_CUSTOM) {
                    logger.debug("Redrawing image of TYPE_CUSTOM into a new image of TYPE_INT_RGB: {}",
                            image.toString());
                    BufferedImage rgbImage = new BufferedImage(image.getWidth(),
                            image.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = rgbImage.createGraphics();
                    g.drawImage(image, 0, 0, null);
                    g.dispose();
                    image = rgbImage;
                }
                break;
        }
        return image;
    }

    private BufferedImage cropImage(BufferedImage inputImage, Region region) {
        BufferedImage croppedImage;
        if (region.isFull()) {
            croppedImage = inputImage;
        } else {
            int x, y, requestedWidth, requestedHeight, width, height;
            if (region.isPercent()) {
                x = (int) Math.round((region.getX() / 100.0) *
                        inputImage.getWidth());
                y = (int) Math.round((region.getY() / 100.0) *
                        inputImage.getHeight());
                requestedWidth = (int) Math.round((region.getWidth() / 100.0) *
                        inputImage.getWidth());
                requestedHeight = (int) Math.round((region.getHeight() / 100.0) *
                        inputImage.getHeight());
            } else {
                x = Math.round(region.getX());
                y = Math.round(region.getY());
                requestedWidth = Math.round(region.getWidth());
                requestedHeight = Math.round(region.getHeight());
            }
            // BufferedImage.getSubimage() will protest if asked for more
            // width/height than is available
            width = (x + requestedWidth > inputImage.getWidth()) ?
                    inputImage.getWidth() - x : requestedWidth;
            height = (y + requestedHeight > inputImage.getHeight()) ?
                    inputImage.getHeight() - y : requestedHeight;
            croppedImage = inputImage.getSubimage(x, y, width, height);
            inputImage.flush();
        }
        return croppedImage;
    }

    private BufferedImage filterImage(BufferedImage inputImage,
                                      Quality quality) {
        BufferedImage filteredImage = inputImage;
        if (quality != Quality.COLOR && quality != Quality.DEFAULT) {
            switch (quality) {
                case GRAY:
                    filteredImage = new BufferedImage(inputImage.getWidth(),
                            inputImage.getHeight(),
                            BufferedImage.TYPE_BYTE_GRAY);
                    break;
                case BITONAL:
                    filteredImage = new BufferedImage(inputImage.getWidth(),
                            inputImage.getHeight(),
                            BufferedImage.TYPE_BYTE_BINARY);
                    break;
            }
            Graphics2D g2d = filteredImage.createGraphics();
            g2d.drawImage(inputImage, 0, 0, null);
            inputImage.flush();
        }
        return filteredImage;
    }

    private BufferedImage rotateImage(BufferedImage inputImage,
                                      Rotation rotation) {
        // do mirroring
        BufferedImage mirroredImage = inputImage;
        if (rotation.shouldMirror()) {
            AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
            tx.translate(-mirroredImage.getWidth(null), 0);
            AffineTransformOp op = new AffineTransformOp(tx,
                    AffineTransformOp.TYPE_BILINEAR);
            mirroredImage = op.filter(inputImage, null);
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
                    inputImage.getType());
            Graphics2D g2d = rotatedImage.createGraphics();
            RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHints(hints);
            g2d.drawImage(mirroredImage, tx, null);
            mirroredImage.flush();
        }
        inputImage.flush();
        return rotatedImage;
    }

    private BufferedImage scaleImage(BufferedImage inputImage, Size size) {
        // use G2D for no particular reason.
        return scaleImageWithG2d(inputImage, size);
    }

    /**
     * Scales an image using an AffineTransform.
     *
     * @param inputImage
     * @param size
     * @return
     */
    private BufferedImage scaleImageWithAffineTransform(
            BufferedImage inputImage, Size size) {
        BufferedImage scaledImage;
        if (size.getScaleMode() == Size.ScaleMode.FULL) {
            scaledImage = inputImage;
        } else {
            double xScale = 0.0f, yScale = 0.0f;
            if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_WIDTH) {
                xScale = size.getWidth() / (double) inputImage.getWidth();
                yScale = xScale;
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_HEIGHT) {
                yScale = size.getHeight() / (double) inputImage.getHeight();
                xScale = yScale;
            } else if (size.getScaleMode() == Size.ScaleMode.NON_ASPECT_FILL) {
                xScale = size.getWidth() / (double) inputImage.getWidth();
                yScale = size.getHeight() / (double) inputImage.getHeight();
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_INSIDE) {
                double hScale = (double) size.getWidth() /
                        (double) inputImage.getWidth();
                double vScale = (double) size.getHeight() /
                        (double) inputImage.getHeight();
                xScale = inputImage.getWidth() * Math.min(hScale, vScale);
                yScale = inputImage.getHeight() * Math.min(hScale, vScale);
            } else if (size.getPercent() != null) {
                xScale = size.getPercent() / 100.0;
                yScale = xScale;
            }
            int width = (int) Math.round(inputImage.getWidth() * xScale);
            int height = (int) Math.round(inputImage.getHeight() * yScale);
            scaledImage = new BufferedImage(width, height, inputImage.getType());
            AffineTransform at = new AffineTransform();
            at.scale(xScale, yScale);
            AffineTransformOp scaleOp = new AffineTransformOp(at,
                    AffineTransformOp.TYPE_BILINEAR);
            scaledImage = scaleOp.filter(inputImage, scaledImage);
            inputImage.flush();
        }
        return scaledImage;
    }

    /**
     * Scales an image using Graphics2D.
     *
     * @param inputImage
     * @param size
     * @return
     */
    private BufferedImage scaleImageWithG2d(BufferedImage inputImage,
                                            Size size) {
        BufferedImage scaledImage;
        if (size.getScaleMode() == Size.ScaleMode.FULL) {
            scaledImage = inputImage;
        } else {
            int width = 0, height = 0;
            if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_WIDTH) {
                width = size.getWidth();
                height = inputImage.getHeight() * width /
                        inputImage.getWidth();
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_HEIGHT) {
                height = size.getHeight();
                width = inputImage.getWidth() * height /
                        inputImage.getHeight();
            } else if (size.getScaleMode() == Size.ScaleMode.NON_ASPECT_FILL) {
                width = size.getWidth();
                height = size.getHeight();
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_INSIDE) {
                double hScale = (double) size.getWidth() /
                        (double) inputImage.getWidth();
                double vScale = (double) size.getHeight() /
                        (double) inputImage.getHeight();
                width = (int) Math.round(inputImage.getWidth() *
                        Math.min(hScale, vScale));
                height = (int) Math.round(inputImage.getHeight() *
                        Math.min(hScale, vScale));
            } else if (size.getPercent() != null) {
                width = (int) Math.round(inputImage.getWidth() *
                        (size.getPercent() / 100.0));
                height = (int) Math.round(inputImage.getHeight() *
                        (size.getPercent() / 100.0));
            }
            scaledImage = new BufferedImage(width, height,
                    inputImage.getType());
            Graphics2D g2d = scaledImage.createGraphics();
            RenderingHints hints = new RenderingHints(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHints(hints);
            g2d.drawImage(inputImage, 0, 0, width, height, null);
            g2d.dispose();
            inputImage.flush();
        }
        return scaledImage;
    }

    private void outputImage(BufferedImage image, OutputFormat outputFormat,
                             OutputStream outputStream) throws IOException {
        try {
            switch (outputFormat) {
                case JPG:
                    // TurboJpegImageWriter is used automatically if libjpeg-turbo
                    // is available in java.library.path:
                    // https://github.com/geosolutions-it/imageio-ext/wiki/TurboJPEG-plugin
                    float quality = Application.getConfiguration().
                            getFloat("ImageIoProcessor.jpg.quality", 0.7f);
                    Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
                    ImageWriter writer = (ImageWriter) iter.next();
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(quality);
                    param.setCompressionType("JPEG");
                    ImageOutputStream os = ImageIO.createImageOutputStream(outputStream);
                    writer.setOutput(os);
                    IIOImage iioImage = new IIOImage(image, null, null);
                    writer.write(null, iioImage, param);
                    writer.dispose();
                    break;
                default:
                    ImageIO.write(image, outputFormat.getExtension(), outputStream);
                    break;
            }
        } finally {
            image.flush();
        }
    }

}
