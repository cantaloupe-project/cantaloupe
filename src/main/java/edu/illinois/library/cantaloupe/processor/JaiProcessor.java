package edu.illinois.library.cantaloupe.processor;

import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codecimpl.TIFFImageEncoder;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;
import it.geosolutions.jaiext.JAIExt;
import org.restlet.data.Form;
import org.restlet.data.MediaType;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Processor using the Java Advanced Imaging (JAI) framework.
 */
class JaiProcessor implements FileProcessor, StreamProcessor {

    private static final int JAI_TILE_SIZE = 512;
    private static final Set<Quality> SUPPORTED_QUALITIES = new HashSet<>();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();

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
            formatsMap = new HashMap<>();
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                Set<OutputFormat> outputFormats = new HashSet<>();

                for (int i = 0, length = readerMimeTypes.length; i < length; i++) {
                    if (sourceFormat.getMediaTypes().
                            contains(new MediaType(readerMimeTypes[i].toLowerCase()))) {
                        for (OutputFormat outputFormat : OutputFormat.values()) {
                            if (outputFormat == OutputFormat.GIF ||
                                    outputFormat == OutputFormat.JP2) {
                                // these currently don't work (see outputImage())
                                continue;
                            }
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

    @Override
    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        return getFormats().get(sourceFormat);
    }

    @Override
    public Dimension getSize(File inputFile, SourceFormat sourceFormat)
            throws ProcessorException {
        return ProcessorUtil.getSize(inputFile, sourceFormat);
    }

    @Override
    public Dimension getSize(InputStream inputStream, SourceFormat sourceFormat)
            throws ProcessorException {
        return ProcessorUtil.getSize(inputStream, sourceFormat);
    }

    @Override
    public Set<ProcessorFeature> getSupportedFeatures(
            final SourceFormat sourceFormat) {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    @Override
    public Set<Quality> getSupportedQualities(final SourceFormat sourceFormat) {
        Set<Quality> qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_QUALITIES);
        }
        return qualities;
    }

    @Override
    public void process(Parameters params, Form urlQuery,
                        SourceFormat sourceFormat, Dimension sourceSize,
                        File inputFile, OutputStream outputStream)
            throws ProcessorException {
        doProcess(params, sourceFormat, inputFile, outputStream);
    }

    @Override
    public void process(Parameters params, Form urlQuery,
                        SourceFormat sourceFormat, Dimension fullSize,
                        InputStream inputStream, OutputStream outputStream)
            throws ProcessorException {
        doProcess(params, sourceFormat, inputStream, outputStream);
    }

    private void doProcess(Parameters params, SourceFormat sourceFormat,
                           Object input, OutputStream outputStream)
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(params.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            RenderedOp image = loadRegion(input, params.getRegion());
            image = ProcessorUtil.scaleImage(image, params.getSize());
            image = ProcessorUtil.rotateImage(image, params.getRotation());
            image = ProcessorUtil.filterImage(image, params.getQuality());
            outputImage(image, params.getOutputFormat(), outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    private RenderedOp loadRegion(Object input, Region region) {
        ParameterBlockJAI pbj = new ParameterBlockJAI("ImageRead");
        ImageLayout layout = new ImageLayout();
        layout.setTileWidth(JAI_TILE_SIZE);
        layout.setTileHeight(JAI_TILE_SIZE);
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        pbj.setParameter("Input", input);
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

    private void outputImage(RenderedOp image, OutputFormat format,
                             OutputStream outputStream) throws IOException {
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
