package edu.illinois.library.cantaloupe.processor;

import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.PNGEncodeParam;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;
import edu.illinois.library.cantaloupe.request.Rotation;
import edu.illinois.library.cantaloupe.request.Size;
import it.geosolutions.jaiext.JAIExt;
import org.restlet.data.MediaType;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.operator.TransposeDescriptor;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Processor using the Java Advanced Imaging (JAI) framework.
 */
class JaiProcessor implements Processor {

    private static final int JAI_TILE_SIZE = 512;
    private static final Set<Quality> SUPPORTED_QUALITIES = new HashSet<Quality>();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<ProcessorFeature>();

    private static HashMap<SourceFormat,Set<OutputFormat>> formatsMap;

    static {
        // replace the JRE JAI operations with GeoTools JAI-EXT
        JAIExt.initJAIEXT();

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
    public static HashMap<SourceFormat, Set<OutputFormat>> getFormats() {
        if (formatsMap == null) {
            final String[] readerMimeTypes = ImageIO.getReaderMIMETypes();
            final String[] writerMimeTypes = ImageIO.getWriterMIMETypes();
            formatsMap = new HashMap<SourceFormat, Set<OutputFormat>>();
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                Set<OutputFormat> outputFormats = new HashSet<OutputFormat>();

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
                formatsMap.put(sourceFormat, outputFormats);
            }
        }
        return formatsMap;
    }

    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        return getFormats().get(sourceFormat);
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
            throw new UnsupportedSourceFormatException();
        } else if (!availableOutputFormats.contains(params.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        RenderedOp image = loadRegion(inputStream, params.getRegion());
        image = scaleImage(image, params.getSize());
        image = rotateImage(image, params.getRotation());
        image = filterImage(image, params.getQuality());
        outputImage(image, params.getOutputFormat(), outputStream);
    }

    private RenderedOp loadRegion(ImageInputStream inputStream, Region region) {
        ParameterBlockJAI pbj = new ParameterBlockJAI("ImageRead");
        ImageLayout layout = new ImageLayout();
        layout.setTileWidth(JAI_TILE_SIZE);
        layout.setTileHeight(JAI_TILE_SIZE);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        pbj.setParameter("Input", inputStream);
        RenderedOp op = JAI.create("ImageRead", pbj, hints);

        RenderedOp croppedImage;
        if (region.isFull()) {
            croppedImage = op;
        } else {
            // calculate the region x, y, and actual width/height
            float x, y, requestedWidth, requestedHeight, actualWidth, actualHeight;
            if (region.isPercent()) {
                x = (float) (region.getX() / 100.0) * op.getWidth();
                y = (float) (region.getY() / 100.0) * op.getHeight();
                requestedWidth = (float) (region.getWidth() / 100.0) *
                        op.getWidth();
                requestedHeight = (float) (region.getHeight() / 100.0) *
                        op.getHeight();
            } else {
                x = region.getX();
                y = region.getY();
                requestedWidth = region.getWidth();
                requestedHeight = region.getHeight();
            }
            actualWidth = (x + requestedWidth > op.getWidth()) ?
                    op.getWidth() - x : requestedWidth;
            actualHeight = (y + requestedHeight > op.getHeight()) ?
                    op.getHeight() - y : requestedHeight;

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(op);
            pb.add(x);
            pb.add(y);
            pb.add(actualWidth);
            pb.add(actualHeight);
            croppedImage = JAI.create("crop", pb);
        }
        return croppedImage;
    }

    private RenderedOp scaleImage(RenderedOp inputImage, Size size) {
        RenderedOp scaledImage;
        if (size.getScaleMode() == Size.ScaleMode.FULL) {
            scaledImage = inputImage;
        } else {
            float xScale = 1.0f;
            float yScale = 1.0f;
            if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_WIDTH) {
                xScale = (float) size.getWidth() / inputImage.getWidth();
                yScale = xScale;
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_HEIGHT) {
                yScale = (float) size.getHeight() / inputImage.getHeight();
                xScale = yScale;
            } else if (size.getScaleMode() == Size.ScaleMode.NON_ASPECT_FILL) {
                xScale = (float) size.getWidth() / inputImage.getWidth();
                yScale = (float) size.getHeight() / inputImage.getHeight();
            } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_INSIDE) {
                double hScale = (float) size.getWidth() / inputImage.getWidth();
                double vScale = (float) size.getHeight() / inputImage.getHeight();
                xScale = (float) (inputImage.getWidth() * Math.min(hScale, vScale));
                yScale = (float) (inputImage.getHeight() * Math.min(hScale, vScale));
            } else if (size.getPercent() != null) {
                xScale = size.getPercent() / 100.0f;
                yScale = xScale;
            }
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inputImage);
            pb.add(xScale);
            pb.add(yScale);
            pb.add(0.0f);
            pb.add(0.0f);
            pb.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
            scaledImage = JAI.create("scale", pb);
        }
        return scaledImage;
    }

    private RenderedOp rotateImage(RenderedOp inputImage, Rotation rotation) {
        // do mirroring
        RenderedOp mirroredImage = inputImage;
        if (rotation.shouldMirror()) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inputImage);
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

    private RenderedOp filterImage(RenderedOp inputImage, Quality quality) {
        RenderedOp filteredImage = inputImage;
        if (quality != Quality.COLOR && quality != Quality.DEFAULT) {
            // convert to grayscale
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(inputImage);
            double[][] matrixRgb = { { 0.114, 0.587, 0.299, 0 } };
            double[][] matrixRgba = { { 0.114, 0.587, 0.299, 0, 0 } };
            if (OpImage.getExpandedNumBands(inputImage.getSampleModel(),
                    inputImage.getColorModel()) == 4) {
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

    private void outputImage(RenderedOp image, OutputFormat format,
                             OutputStream outputStream) throws IOException {
        switch (format) {
            case GIF:
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
                    // TODO: this doesn't seem to write anything
                    writer.write(image);
                }
                break;
            case JP2:
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
                    // TODO: this doesn't seem to write anything
                    writer.write(null, iioImage, j2Param);
                }
                break;
            case JPG:
                JPEGEncodeParam jParam = new JPEGEncodeParam();
                ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG",
                        outputStream, jParam);
                encoder.encode(image);
                break;
            case PNG:
                PNGEncodeParam pngParam = new PNGEncodeParam.RGB();
                ImageEncoder pngEncoder = ImageCodec.createImageEncoder("PNG",
                        outputStream, pngParam);
                pngEncoder.encode(image);
                break;
            case TIF:
                TIFFImageEncoder tiffEnc = new TIFFImageEncoder(outputStream,
                        null);
                tiffEnc.encode(image);
                break;
        }

    }

}
