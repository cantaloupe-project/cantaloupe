package edu.illinois.library.cantaloupe.processor;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Operation;
import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Transpose;
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
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
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

    public static final String CONFIG_KEY_JPG_QUALITY = "Java2dProcessor.jpg.quality";
    public static final String CONFIG_KEY_TIF_READER = "Java2dProcessor.tif.reader";

    private static final HashMap<SourceFormat,Set<OutputFormat>> FORMATS =
            getAvailableOutputFormats();
    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = new HashSet<>();
    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = new HashSet<>();

    static {
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.BITONAL);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.COLOR);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.GRAY);
        SUPPORTED_IIIF_1_1_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.NATIVE);

        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.BITONAL);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.COLOR);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.DEFAULT);
        SUPPORTED_IIIF_2_0_QUALITIES.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.GRAY);

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
                        // TODO: not working (see inline comment in ProcessorUtil.writeImage())
                        if (outputFormat.equals(OutputFormat.JP2)) {
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
            map.put(sourceFormat, outputFormats);
        }
        return map;
    }

    @Override
    public Set<OutputFormat> getAvailableOutputFormats(SourceFormat sourceFormat) {
        Set<OutputFormat> formats = FORMATS.get(sourceFormat);
        if (formats == null) {
            formats = new HashSet<>();
        }
        return formats;
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
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
    getSupportedIiif1_1Qualities(final SourceFormat sourceFormat) {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_1_1_QUALITIES);
        }
        return qualities;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
    getSupportedIiif2_0Qualities(final SourceFormat sourceFormat) {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
                qualities = new HashSet<>();
        if (getAvailableOutputFormats(sourceFormat).size() > 0) {
            qualities.addAll(SUPPORTED_IIIF_2_0_QUALITIES);
        }
        return qualities;
    }

    @Override
    public void process(Operations ops, SourceFormat sourceFormat,
                        Dimension fullSize, InputStream inputStream,
                        OutputStream outputStream) throws ProcessorException {
        final Set<OutputFormat> availableOutputFormats =
                getAvailableOutputFormats(sourceFormat);
        if (getAvailableOutputFormats(sourceFormat).size() < 1) {
            throw new UnsupportedSourceFormatException(sourceFormat);
        } else if (!availableOutputFormats.contains(ops.getOutputFormat())) {
            throw new UnsupportedOutputFormatException();
        }

        try {
            ReductionFactor reductionFactor = new ReductionFactor();
            BufferedImage image = loadImage(inputStream, sourceFormat, ops,
                    fullSize, reductionFactor);
            for (Operation op : ops) {
                if (op instanceof Crop) {
                    image = ProcessorUtil.cropImage(image, (Crop) op,
                            reductionFactor.factor);
                } else if (op instanceof Scale) {
                    image = ProcessorUtil.scaleImageWithG2d(image, (Scale) op,
                            reductionFactor.factor);
                } else if (op instanceof Transpose) {
                    image = ProcessorUtil.transposeImage(image, (Transpose) op);
                } else if (op instanceof Rotate) {
                    image = ProcessorUtil.rotateImage(image, (Rotate) op);
                } else if (op instanceof Filter) {
                    image = ProcessorUtil.filterImage(image, (Filter) op);
                }
            }
            ProcessorUtil.writeImage(image, ops.getOutputFormat(),
                    outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    private BufferedImage loadImage(InputStream inputStream,
                                    SourceFormat sourceFormat,
                                    Operations ops,
                                    Dimension fullSize,
                                    ReductionFactor reductionFactor)
            throws IOException, ProcessorException { // TODO: move this to ProcessorUtil
        BufferedImage image = null;
        switch (sourceFormat) {
            case BMP:
                ImageInputStream iis = ImageIO.createImageInputStream(inputStream);
                image = ImageIO.read(iis);
                break;
            case TIF:
                String tiffReader = Application.getConfiguration().
                        getString(CONFIG_KEY_TIF_READER, "TIFFImageReader");
                if (tiffReader.equals("TIFFImageReader")) {
                    image = loadUsingTiffImageReader(inputStream, ops,
                            fullSize, reductionFactor);
                } else {
                    image = loadUsingTiffImageDecoder(inputStream);
                }
                break;
            default:
                Iterator<ImageReader> it = ImageIO.getImageReadersByMIMEType(
                        sourceFormat.getPreferredMediaType().toString());
                while (it.hasNext()) {
                    ImageReader reader = it.next();
                    try {
                        iis = ImageIO.createImageInputStream(inputStream);
                        reader.setInput(iis);
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
                    ops.getIdentifier());
        }
        return rgbImage;
    }

    /**
     * <p>Uses TIFFImageReader to load a TIFF from an InputStream.</p>
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
     *     <code>getSmallestUsableImage()</code>).</li>
     *     <li>It throws an ArrayIndexOutOfBoundsException when a TIFF file
     *     contains a tag value greater than 6. (To inspect tag values, run
     *     <code>$ tiffdump &lt;file&gt;</code>.) (The Sun TIFFImageReader
     *     suffers from the same issue except it throws an
     *     IllegalArgumentException instead.)</li>
     *     <li>It renders some TIFFs with improper colors.</li>
     * </ul>
     *
     * <p>Most of these are probably fixable with some clever workarounds.</p>
     *
     * <p><code>ImageIO.read()</code> would be an alternative, but it is not
     * usable because it also suffers from the <code>TYPE_CUSTOM</code> issue.
     * Also, it doesn't allow access to pyramidal TIFF sub-images, and just
     * generally doesn't provide any control over the reading process.</p>
     *
     * @param inputStream
     * @param ops
     * @param fullSize
     * @param reductionFactor
     * @return
     * @throws IOException
     * @throws ProcessorException
     * @see <a href="https://github.com/geosolutions-it/imageio-ext/blob/master/plugin/tiff/src/main/java/it/geosolutions/imageioimpl/plugins/tiff/TIFFImageReader.java">
     *     TIFFImageReader source</a>
     */
    private BufferedImage loadUsingTiffImageReader(
            InputStream inputStream, Operations ops, Dimension fullSize,
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
                ImageInputStream iis = ImageIO.createImageInputStream(inputStream);
                reader.setInput(iis);
                image = getSmallestUsableImage(reader, fullSize, crop,
                        scale, reductionFactor);
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
     * Loads a TIFF image using JAI TIFFImageDecoder. Currently not optimized
     * for pyramidal TIFFs.
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    private BufferedImage loadUsingTiffImageDecoder(InputStream inputStream)
            throws IOException {
        BufferedImage image;
        try {
            ImageDecoder dec = ImageCodec.createImageDecoder("tiff",
                    inputStream, null);
            RenderedImage op = dec.decodeAsRenderedImage();
            BufferedImage rgbImage = new BufferedImage(op.getWidth(),
                    op.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgbImage.setData(op.getData());
            image = rgbImage;
        } finally {
            inputStream.close();
        }
        return image;
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
    private BufferedImage getSmallestUsableImage(ImageReader reader,
                                                 Dimension fullSize,
                                                 Crop crop, Scale scale,
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
        if (scale.getMode() != Scale.Mode.FULL) {
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

}
