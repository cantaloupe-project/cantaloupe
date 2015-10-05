package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import edu.illinois.library.cantaloupe.request.Region;
import edu.illinois.library.cantaloupe.request.Size;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.awt.Rectangle;
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
        BufferedImage rgbImage = ProcessorUtil.convertToRgb(image);
        if (rgbImage != image) {
            logger.warn("Converting {} to RGB (this is very expensive)",
                    params.getIdentifier());
        }
        return rgbImage;
    }

    private BufferedImage loadTiff(ImageInputStream inputStream,
                                   Parameters params,
                                   ReductionFactor reductionFactor)
            throws IOException {
        BufferedImage image = null;
        // We can't use ImageIO.read() for two reasons: 1) the BufferedImages
        // it returns for TIFFs are often set to type TYPE_CUSTOM, which
        // causes many subsequent operations to fail; and 2) it doesn't allow
        // access to pyramidal TIFF sub-images.
        //
        // Strategy B is it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader.
        // This reader has several issues. First, it sometimes sets
        // BufferedImages to TYPE_CUSTOM as well. Second, it throws an
        // ArrayIndexOutOfBoundsException when a TIFF file contains a
        // tag value > 6. (To inspect tag values, run
        // $ tiffdump <file>.) (The Sun TIFFImageReader suffers from the same
        // issue except it throws an IllegalArgumentException instead.)
        // Finally, it renders some TIFFs with improper colors.
        try {
            Iterator<ImageReader> it = ImageIO.
                    getImageReadersByMIMEType("image/tiff");
            while (it.hasNext()) {
                ImageReader reader = it.next();
                // https://github.com/geosolutions-it/imageio-ext/blob/master/plugin/tiff/src/main/java/it/geosolutions/imageioimpl/plugins/tiff/TIFFImageReader.java
                if (reader instanceof it.geosolutions.imageioimpl.
                        plugins.tiff.TIFFImageReader) {
                    try {
                        reader.setInput(inputStream);
                        image = getSmallestUsableImage(reader, params.getRegion(),
                                params.getSize(), reductionFactor);
                    } finally {
                        reader.dispose();
                    }
                    break;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error("TIFFImageReader failed to read {}",
                    params.getIdentifier());
            throw e;
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
     * @param region Requested region
     * @param size Requested size
     * @param rf Set by reference
     * @return
     * @throws IOException
     */
    private BufferedImage getSmallestUsableImage(ImageReader reader,
                                                 Region region, Size size,
                                                 ReductionFactor rf)
            throws IOException {
        ImageReadParam param = reader.getDefaultReadParam();
        // TODO: why doesn't this work?
        //param.setDestinationType(ImageTypeSpecifier.
        //        createFromBufferedImageType(BufferedImage.TYPE_INT_RGB));
        BufferedImage bestImage = reader.read(0, param);
        if (size.getScaleMode() != Size.ScaleMode.FULL) {
            // Pyramidal TIFFs will have > 1 image, each half the dimensions of
            // the next larger. The "true" parameter tells getNumImages() to
            // scan for images, which seems to be necessary for at least some
            // files, but is expensive.
            int numImages = reader.getNumImages(false);
            if (numImages == -1) {
                numImages = reader.getNumImages(true);
            }
            if (numImages > 1) {
                logger.debug("Detected pyramidal TIFF with {} levels",
                        numImages);
                final Dimension fullSize = new Dimension(bestImage.getWidth(),
                        bestImage.getHeight());
                final Rectangle regionRect = region.getRectangle(fullSize);

                // loop through the tiles from smallest to largest to find the
                // first one that fits the requested scale
                for (int i = numImages - 1; i >= 0; i--) {
                    final BufferedImage tile = reader.read(i);
                    final double tileScale = (double) tile.getWidth() /
                            (double) fullSize.width;
                    boolean fits = false;
                    if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_WIDTH) {
                        fits = (size.getWidth() / (float) regionRect.width <= tileScale);
                    } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_HEIGHT) {
                        fits = (size.getHeight() / (float) regionRect.height <= tileScale);
                    } else if (size.getScaleMode() == Size.ScaleMode.ASPECT_FIT_INSIDE) {
                        fits = (size.getWidth() / (float) regionRect.width <= tileScale &&
                                size.getHeight() / (float) regionRect.height <= tileScale);
                    } else if (size.getScaleMode() == Size.ScaleMode.NON_ASPECT_FILL) {
                        fits = (size.getWidth() / (float) regionRect.width <= tileScale &&
                                size.getHeight() / (float) regionRect.height <= tileScale);
                    } else if (size.getPercent() != null) {
                        float pct = (size.getPercent() / 100f);
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

}
