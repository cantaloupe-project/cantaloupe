package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Color;
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
import edu.illinois.library.cantaloupe.processor.codec.jpeg.JPEGMetadataReader;
import edu.illinois.library.cantaloupe.processor.codec.jpeg.TurboJPEGImageReader;
import edu.illinois.library.cantaloupe.processor.codec.jpeg.TurboJPEGImageWriter;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Processor using the TurboJPEG high-level API to the libjpeg-turbo native
 * library via the Java Native Interface (JNI).</p>
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
            Set.of(Format.get("jpg"));

    private static boolean isClassInitialized;

    private static final boolean USE_FAST_DECODE_DCT = true;
    private static final boolean USE_FAST_ENCODE_DCT = true;

    private static String initializationError;

    private TurboJPEGImageReader imageReader;
    private final JPEGMetadataReader metadataReader = new JPEGMetadataReader();

    private StreamFactory streamFactory;

    private static synchronized void initializeClass() {
        if (!isClassInitialized) {
            isClassInitialized = true;
            try {
                TurboJPEGImageReader.initialize();
            } catch (UnsatisfiedLinkError e) {
                initializationError = e.getMessage();
            }
        }
    }

    static synchronized void resetInitialization() {
        isClassInitialized = false;
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
        return Format.get("jpg");
    }

    @Override
    public StreamFactory getStreamFactory() {
        return streamFactory;
    }

    @Override
    public boolean isSeeking() {
        return false;
    }

    @Override
    public void setSourceFormat(Format format)
            throws SourceFormatException {
        if (!Format.get("jpg").equals(format)) {
            throw new SourceFormatException(format);
        }
    }

    @Override
    public void setStreamFactory(StreamFactory streamFactory) {
        this.streamFactory = streamFactory;
        try {
            imageReader.setSource(streamFactory.newInputStream());
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        } finally {
            try {
                metadataReader.setSource(streamFactory.newSeekableStream());
            } catch (IOException e) {
                LOGGER.warn(e.getMessage());
            }
        }
    }

    @Override
    public void process(final OperationList opList,
                        final Info info,
                        final OutputStream outputStream) throws FormatException, ProcessorException {
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

            final Encode encode = (Encode) opList.getFirst(Encode.class);
            final Color bgColor = encode.getBackgroundColor();
            writer.setQuality(encode.getQuality());
            writer.setProgressive(encode.isInterlacing());
            Metadata metadata = encode.getMetadata();
            if (metadata != null) {
                metadata.getXMP().ifPresent(writer::setXMP);
            }

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
                    image = Java2DUtil.rotate(image, (Rotate) op, bgColor);
                } else if (op instanceof ColorTransform) {
                    image = Java2DUtil.transformColor(image, (ColorTransform) op);
                } else if (op instanceof Sharpen) {
                    image = Java2DUtil.sharpen(image, (Sharpen) op);
                } else if (op instanceof Overlay) {
                    Java2DUtil.applyOverlay(image, (Overlay) op);
                }
            }

            writer.write(image, outputStream);
        } catch (SourceFormatException e) {
            throw e;
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public Info readInfo() throws IOException {
        return Info.builder()
                .withFormat(Format.get("jpg"))
                .withSize(imageReader.getWidth(), imageReader.getHeight())
                .withTileSize(imageReader.getWidth(), imageReader.getHeight())
                .withNumResolutions(1)
                .withMetadata(readMetadata())
                .build();
    }

    private Metadata readMetadata() throws IOException {
        final Metadata metadata = new Metadata();
        // EXIF
        byte[] exif = metadataReader.getEXIF();
        if (exif != null) {
            try (edu.illinois.library.cantaloupe.image.exif.Reader exifReader =
                    new edu.illinois.library.cantaloupe.image.exif.Reader()) {
                exifReader.setSource(exif);
                metadata.setEXIF(exifReader.read());
            }
        }
        // IPTC
        byte[] iptc = metadataReader.getIPTC();
        if (iptc != null) {
            try (edu.illinois.library.cantaloupe.image.iptc.Reader iptcReader =
                    new edu.illinois.library.cantaloupe.image.iptc.Reader()) {
                iptcReader.setSource(iptc);
                metadata.setIPTC(iptcReader.read());
            }
        }
        // XMP
        metadata.setXMP(metadataReader.getXMP());
        return metadata;
    }

}
