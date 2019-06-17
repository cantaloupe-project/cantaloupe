package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
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
import java.util.stream.Collectors;

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

    private TurboJPEGImageReader imageReader;

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

    TurboJpegProcessor() {
        initializeClass();
        try {
            imageReader = new TurboJPEGImageReader();
        } catch (NoClassDefFoundError ignore) {
            // This will be thrown if TurboJPEGImageReader failed to initialize,
            // which would happen if libjpeg-turbo is not available. It's
            // swallowed because this isn't the place to handle it.
        }
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
    public boolean isSeeking() {
        return false;
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
        final ReductionFactor reductionFactor = new ReductionFactor();
        final ScaleConstraint scaleConstraint = opList.getScaleConstraint();
        final TurboJPEGImageWriter writer     = new TurboJPEGImageWriter();

        try {
            imageReader.setUseFastDCT(USE_FAST_DECODE_DCT);
            writer.setUseFastDCT(USE_FAST_ENCODE_DCT);
            writer.setSubsampling(imageReader.getSubsampling());

            final Rectangle roiWithinSafeRegion = new Rectangle();
            BufferedImage image =
                    imageReader.readAsBufferedImage(roiWithinSafeRegion);

            // Apply the crop operation, if present, and retain a reference
            // to it for subsequent operations to refer to.
            Crop crop = new CropByPercent();
            for (Operation op : opList) {
                if (op instanceof Crop) {
                    crop = (Crop) op;
                    if (crop.hasEffect(fullSize, opList)) {
                        image = Java2DUtil.crop(image, crop, reductionFactor,
                                opList.getScaleConstraint());
                    }
                }
            }

            // Redactions happen immediately after cropping.
            final Set<Redaction> redactions = opList.stream()
                    .filter(op -> op instanceof Redaction)
                    .filter(op -> op.hasEffect(fullSize, opList))
                    .map(op -> (Redaction) op)
                    .collect(Collectors.toSet());
            Java2DUtil.applyRedactions(image, fullSize, crop,
                    new double[] { 1.0, 1.0 }, reductionFactor,
                    opList.getScaleConstraint(), redactions);

            for (Operation op : opList) {
                if (!op.hasEffect(fullSize, opList)) {
                    continue;
                }
                if (op instanceof Scale) {
                    image = Java2DUtil.scale(image, (Scale) op,
                            scaleConstraint, reductionFactor);
                } else if (op instanceof Transpose) {
                    image = Java2DUtil.transpose(image, (Transpose) op);
                } else if (op instanceof Rotate) {
                    image = Java2DUtil.rotate(image, (Rotate) op);
                } else if (op instanceof ColorTransform) {
                    image = Java2DUtil.transformColor(image, (ColorTransform) op);
                } else if (op instanceof Sharpen) {
                    image = Java2DUtil.sharpen(image, (Sharpen) op);
                } else if (op instanceof Overlay) {
                    Java2DUtil.applyOverlay(image, (Overlay) op);
                } else if (op instanceof Encode) {
                    Encode encode = (Encode) op;
                    writer.setQuality(encode.getQuality());
                    writer.setProgressive(encode.isInterlacing());
                }
            }

            writer.write(image, outputStream);
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public Info readInfo() throws IOException {
        return Info.builder()
                .withFormat(Format.JPG)
                .withSize(imageReader.getWidth(), imageReader.getHeight())
                .withTileSize(imageReader.getWidth(), imageReader.getHeight())
                .withNumResolutions(1)
                .withOrientation(Orientation.ROTATE_0) // TODO: fix this
                .build();
    }

}
