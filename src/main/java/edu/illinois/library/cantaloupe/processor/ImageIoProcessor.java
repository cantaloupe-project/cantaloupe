package edu.illinois.library.cantaloupe.processor;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.awt.Graphics2D;
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
class ImageIoProcessor implements StreamProcessor {

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
                             SourceFormat sourceFormat)
            throws ProcessorException {
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
            } catch (IOException e) {
                throw new ProcessorException(e.getMessage(), e);
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
            throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(params.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            BufferedImage image = loadImage(inputStream, sourceFormat);
            image = ProcessorUtil.cropImage(image, params.getRegion());
            image = ProcessorUtil.scaleImageWithG2d(image, params.getSize());
            image = ProcessorUtil.rotateImage(image, params.getRotation());
            image = ProcessorUtil.filterImage(image, params.getQuality());
            ProcessorUtil.outputImage(image, params.getOutputFormat(),
                    outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    private BufferedImage loadImage(ImageInputStream inputStream,
                                    SourceFormat sourceFormat)
            throws IOException, UnsupportedSourceFormatException {
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

}
