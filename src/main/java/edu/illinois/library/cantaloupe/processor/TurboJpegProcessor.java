package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.overlay.Overlay;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.processor.codec.turbojpeg.TransformationNotSupportedException;
import edu.illinois.library.cantaloupe.processor.codec.turbojpeg.TurboJPEGImage;
import edu.illinois.library.cantaloupe.processor.codec.turbojpeg.TurboJPEGImageReader;
import edu.illinois.library.cantaloupe.processor.codec.turbojpeg.TurboJPEGImageWriter;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Processor using the TurboJPEG high-level API to the libjpeg-turbo native
 * library via the Java Native Interface (JNI).</p>
 *
 * <p>{@link TurboJPEGImageReader} is used to acquire a (possibly) scaled
 * region of interest that is buffered in memory. Java 2D is used for
 * post-processing steps when necessary, but otherwise, care is taken to only
 * decompress the source JPEG data when later processing steps require it.</p>
 *
 * <h1>Usage</h1>
 *
 * <p>The libjpeg-turbo shared library must be compiled with Java support (it
 * isn't by default) and present on the library path, or else the {@literal
 * -Djava.library.path} VM argument must be provided at launch, with a value of
 * the pathname of the directory containing the library. See the {@link
 * org.libjpegturbo.turbojpeg} package documentation for more info.</p>
 *
 * @author Alex Dolski UIUC
 */
public class TurboJpegProcessor extends AbstractProcessor
        implements StreamProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TurboJpegProcessor.class);

    private static final Set<Format> SUPPORTED_OUTPUT_FORMATS =
            Collections.unmodifiableSet(EnumSet.of(Format.JPG));

    private static final Set<ProcessorFeature> SUPPORTED_FEATURES =
            Collections.unmodifiableSet(EnumSet.of(
                    ProcessorFeature.MIRRORING,
                    ProcessorFeature.REGION_BY_PERCENT,
                    ProcessorFeature.REGION_BY_PIXELS,
                    ProcessorFeature.REGION_SQUARE,
                    ProcessorFeature.ROTATION_BY_90S,
                    ProcessorFeature.ROTATION_ARBITRARY,
                    ProcessorFeature.SIZE_ABOVE_FULL,
                    ProcessorFeature.SIZE_BY_CONFINED_WIDTH_HEIGHT,
                    ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT,
                    ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                    ProcessorFeature.SIZE_BY_HEIGHT,
                    ProcessorFeature.SIZE_BY_PERCENT,
                    ProcessorFeature.SIZE_BY_WIDTH,
                    ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));

    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
            SUPPORTED_IIIF_1_1_QUALITIES = Collections.unmodifiableSet(EnumSet.of(
            edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
            edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
            edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GREY,
            edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));

    private static final Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
            SUPPORTED_IIIF_2_0_QUALITIES = Collections.unmodifiableSet(EnumSet.of(
            edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
            edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
            edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
            edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY));

    private static final AtomicBoolean IS_CLASS_INITIALIZED = new AtomicBoolean();

    private static final boolean USE_FAST_DECODE_DCT = true;
    private static final boolean USE_FAST_ENCODE_DCT = true;

    private static String initializationError;

    private final TurboJPEGImageReader imageReader = new TurboJPEGImageReader();

    private StreamFactory streamFactory;

    private static synchronized void initializeClass() {
        if (!IS_CLASS_INITIALIZED.get()) {
            IS_CLASS_INITIALIZED.set(true);
            try {
                TurboJPEGImageReader.initialize();
            } catch (UnsatisfiedLinkError e) {
                initializationError = e.getMessage();
            }
        }
    }

    static synchronized void resetInitialization() {
        IS_CLASS_INITIALIZED.set(false);
    }

    @Override
    public void close() {
        imageReader.close();
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        return SUPPORTED_OUTPUT_FORMATS;
    }

    @Override
    public String getInitializationError() {
        initializeClass();
        return initializationError;
    }

    @Override
    public Format getSourceFormat() {
        return Format.JPG;
    }

    @Override
    public StreamFactory getStreamFactory() {
        return streamFactory;
    }

    @Override
    public Set<ProcessorFeature> getSupportedFeatures() {
        return SUPPORTED_FEATURES;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
    getSupportedIIIF1Qualities() {
        return SUPPORTED_IIIF_1_1_QUALITIES;
    }

    @Override
    public Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
    getSupportedIIIF2Qualities() {
        return SUPPORTED_IIIF_2_0_QUALITIES;
    }

    @Override
    public void setSourceFormat(Format format)
            throws UnsupportedSourceFormatException {
        if (!Format.JPG.equals(format)) {
            throw new UnsupportedSourceFormatException(format);
        }
    }

    @Override
    public void setStreamFactory(StreamFactory streamFactory) {
        this.streamFactory = streamFactory;
        try {
            imageReader.setSource(streamFactory.newInputStream());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void process(final OperationList opList,
                        final Info info,
                        final OutputStream outputStream) throws ProcessorException {
        final Dimension fullSize              = info.getSize();
        final ScaleConstraint scaleConstraint = opList.getScaleConstraint();
        final double scScale = scaleConstraint.getRational().doubleValue();

        imageReader.setUseFastDCT(USE_FAST_DECODE_DCT);

        // Initialize the reader with the desired ROI and scale.
        boolean requiresCrop = false, requiresRedaction = false,
                progressive = true;
        Rectangle roi;
        Scale scale         = null;
        Transpose transpose = null;
        Rotate rotate       = null;
        ColorTransform ctx  = null;
        Overlay overlay     = null;
        Sharpen sharpen     = null;
        int quality         = -1;
        try {
            for (Operation op : opList) {
                if (!op.hasEffect(fullSize, opList)) {
                    continue;
                }
                if (op instanceof Crop) {
                    Crop crop = (Crop) op;
                    roi = crop.getRectangle(fullSize, scaleConstraint);
                    imageReader.setRegion(roi.intX(), roi.intY(),
                            roi.intWidth(), roi.intHeight());

                    final int blockWidth  = imageReader.getBlockWidth();
                    final int blockHeight = imageReader.getBlockHeight();
                    requiresCrop = (roi.intX() % blockWidth != 0 ||
                            roi.intY() % blockHeight != 0 ||
                            roi.intWidth() % blockWidth != 0 ||
                            roi.intHeight() % blockHeight != 0);
                } else if (op instanceof Scale) {
                    scale = (Scale) op;
                } else if (op instanceof Transpose) {
                    transpose = (Transpose) op;
                } else if (op instanceof Rotate) {
                    rotate = (Rotate) op;
                } else if (op instanceof ColorTransform) {
                    if (ColorTransform.GRAY.equals(op)) {
                        try {
                            imageReader.setUseGrayscaleConversion(true);
                        } catch (TransformationNotSupportedException e) {
                            ctx = (ColorTransform) op;
                        }
                    } else {
                        ctx = (ColorTransform) op;
                    }
                } else if (op instanceof Redaction) {
                    requiresRedaction = true;
                } else if (op instanceof Overlay) {
                    overlay = (Overlay) op;
                } else if (op instanceof Sharpen) {
                    sharpen = (Sharpen) op;
                } else if (op instanceof Encode) {
                    Encode encode = (Encode) op;
                    quality       = encode.getQuality();
                    progressive   = encode.isInterlacing();
                }
            }

            // Initialize a writer.
            final TurboJPEGImageWriter writer = new TurboJPEGImageWriter();
            writer.setProgressive(progressive);
            writer.setSubsampling(imageReader.getSubsampling());
            writer.setUseFastDCT(USE_FAST_ENCODE_DCT);
            if (quality > 0) {
                writer.setQuality(quality);
            }

            if (requiresCrop || scale != null || ctx != null ||
                    requiresRedaction || transpose != null || rotate != null ||
                    overlay != null || sharpen != null) {
                // Read as a BufferedImage. roiWithinSafeRegion is the required
                // additional crop area within the returned image.
                final Rectangle roiWithinSafeRegion = new Rectangle();
                BufferedImage image =
                        imageReader.readAsBufferedImage(roiWithinSafeRegion);
                roiWithinSafeRegion.scaleX(scScale);
                roiWithinSafeRegion.scaleY(scScale);

                if (requiresCrop) {
                    LOGGER.trace("ROI coordinates are not multiples of the " +
                            "block size; additional cropping will be needed.");
                    // TJCompressor will access this BufferedImage's Raster
                    // directly, bypassing the updated crop info in the new
                    // BufferedImage. We could use TJCompressor.setSourceImage()
                    // to provide it the region we want to write, but that's
                    // harder to do after scaling and rotation etc., so for
                    // simplicity's sake, we will do a more expensive
                    // "copy-crop" here.
                    image = Java2DUtil.crop(image,
                            new Rectangle(roiWithinSafeRegion), true);
                }

                if (scale != null) {
                    LOGGER.trace("Unable to use libjpeg-turbo scaling; " +
                            "falling back to Java 2D.");
                    image = Java2DUtil.scale(image, scale, scaleConstraint,
                            new ReductionFactor());
                }

                // TODO: redactions

                if (transpose != null) {
                    image = Java2DUtil.transpose(image, transpose);
                }

                if (rotate != null) {
                    image = Java2DUtil.rotate(image, rotate);
                }

                if (ctx != null) {
                    image = Java2DUtil.transformColor(image, ctx);
                }

                if (overlay != null) {
                    Java2DUtil.applyOverlay(image, overlay);
                }

                if (sharpen != null) {
                    image = Java2DUtil.sharpen(image, sharpen);
                }

                // Write it out.
                writer.write(image, outputStream);
            } else {
                LOGGER.trace("No decompression necessary");

                // Read the compressed image region.
                TurboJPEGImage image = imageReader.read();

                // Write it.
                writer.write(image, outputStream);
            }
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public Info readInfo() throws IOException {
        return Info.builder()
                .withFormat(Format.JPG)
                .withSize(imageReader.getWidth(), imageReader.getHeight())
                // Blocks aren't really tiles, but efficiency may be improved
                // when clients request image tiles that align with the block
                // grid.
                .withTileSize(imageReader.getBlockWidth(), imageReader.getBlockHeight())
                .withNumResolutions(1)
                .withOrientation(Orientation.ROTATE_0) // TODO: fix this
                .build();
    }

}
