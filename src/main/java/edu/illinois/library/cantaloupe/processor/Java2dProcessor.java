package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Size;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Processor using the Java2D framework.
 */
class Java2dProcessor implements StreamProcessor {

    /**
     * Used to return a reduction factor from loadTiff() by reference.
     */
    private class ReductionFactor {
        public int factor = 0;
    }

    private static Logger logger = LoggerFactory.getLogger(Java2dProcessor.class);

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
        return ProcessorUtil.getSize(inputStream, sourceFormat);
    }

    public Set<ProcessorFeature> getSupportedFeatures(
            final SourceFormat sourceFormat) {
        Set<ProcessorFeature> features = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            features.addAll(SUPPORTED_FEATURES);
        }
        return features;
    }

    public Set<Quality> getSupportedQualities(final SourceFormat sourceFormat) {
        Set<Quality> qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_QUALITIES);
        }
        return qualities;
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
            ReductionFactor reductionFactor = new ReductionFactor();
            BufferedImage image = loadImage(inputStream, sourceFormat, params,
                    reductionFactor);
            image = ProcessorUtil.cropImage(image, params.getRegion(),
                    reductionFactor.factor);
            image = ProcessorUtil.scaleImageWithG2d(image, params.getSize(),
                    reductionFactor.factor);
            image = ProcessorUtil.rotateImage(image, params.getRotation());
            image = ProcessorUtil.filterImage(image, params.getQuality());
            ProcessorUtil.writeImage(image, params.getOutputFormat(),
                    outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    private BufferedImage loadImage(ImageInputStream inputStream,
                                    SourceFormat sourceFormat,
                                    Parameters params,
                                    ReductionFactor reductionFactor)
            throws IOException, UnsupportedSourceFormatException {
        BufferedImage image = null;
        switch (sourceFormat) {
            case BMP:
                image = ImageIO.read(inputStream);
                break;
            case TIF:
                image = loadTiff(inputStream, params, reductionFactor);
                break;
            default:
                Iterator<ImageReader> it = ImageIO.getImageReadersByMIMEType(
                        sourceFormat.getPreferredMediaType().toString());
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
                break;
        }
        return ProcessorUtil.convertToRgb(image);
    }

    private BufferedImage loadTiff(ImageInputStream inputStream,
                                   Parameters params,
                                   ReductionFactor reductionFactor)
            throws IOException {
        BufferedImage image = null;
        // We can't use ImageIO.read() for two reasons: 1) the BufferedImages
        // it returns for TIFFs are often set to type TYPE_CUSTOM, which
        // causes many subsequent operations to fail; and 2) it doesn't allow
        // access to pyramidal TIFF images.
        //
        // Strategy B is it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader.
        // Unfortunately, this reader throws an
        // ArrayIndexOutOfBoundsException when a TIFF file contains a
        // tag value > 6. (To inspect tag values, run
        // $ tiffdump <file>.)
        //
        // The Sun TIFFImageReader suffers from the same issue except it
        // throws an IllegalArgumentException instead.
        try {
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
                    image = getSmallestFittingImage(reader, params.getSize(),
                            reductionFactor);
                } finally {
                    reader.dispose();
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("TIFFImageReader failed to read {}",
                    params.getIdentifier());
        }

        /*
        // Strategy C: the JAI TIFFImageDecoder. Unfortunately, it suffers from
        // a similar issue as TIFFImageReader, which is an inability to
        // decode certain TIFFs properly - they get vertical B&W
        // stripes. tiffdump doesn't provide any clues.
        try (InputStream is = new ImageInputStreamWrapper(inputStream)) {
            ImageDecoder dec = ImageCodec.createImageDecoder("tiff",
                    is, null);
            RenderedImage op = dec.decodeAsRenderedImage();
            BufferedImage rgbImage = new BufferedImage(op.getWidth(),
                    op.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.setData(op.getData());
            image = rgbImage;
        }*/
        return image;
    }

    /**
     * Returns the smallest image fitting the requested size from the given
     * reader. Useful for e.g. pyramidal TIFF.
     *
     * @param reader ImageReader with input source already set
     * @param size Requested size
     * @param rf Set by reference
     * @return
     * @throws IOException
     */
    private BufferedImage getSmallestFittingImage(ImageReader reader, Size size,
                                                  ReductionFactor rf)
            throws IOException {
        BufferedImage bestImage = reader.read(0);
        if (size.getScaleMode() != Size.ScaleMode.FULL) {
            // pyramidal TIFFs will have > 1 image, each half the dimensions of
            // the next larger
            int numImages = reader.getNumImages(true);
            if (numImages > 1) {
                logger.debug("Detected pyramidal TIFF with {} levels",
                        numImages);
                // one or the other may be null
                Integer requestedWidth, requestedHeight;
                if (size.getPercent() != null) {
                    requestedWidth = Math.round(bestImage.getWidth() *
                            (size.getPercent() / 100f));
                    requestedHeight = Math.round(bestImage.getHeight() *
                            (size.getPercent() / 100f));
                } else {
                    requestedWidth = size.getWidth();
                    requestedHeight = size.getHeight();
                }

                // loop through the tiles from smallest to largest to find the
                // first one that fits the requested scale
                for (int i = numImages - 1; i >= 0; i--) {
                    final BufferedImage tile = reader.read(i);
                    boolean fits = false;
                    if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_WIDTH &&
                            requestedWidth <= tile.getWidth()) {
                        fits = true;
                    } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_HEIGHT &&
                                    requestedHeight <= tile.getHeight()) {
                        fits = true;
                    } else if (requestedWidth <= tile.getWidth() &&
                                    requestedHeight <= tile.getHeight()) {
                        fits = true;
                    }
                    if (fits) {
                        double tileScale = (double) tile.getWidth() /
                                (double) bestImage.getWidth();
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

}
