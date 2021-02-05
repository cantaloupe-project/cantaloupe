package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Rectangle;
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
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFacade;
import edu.illinois.library.cantaloupe.processor.codec.jpeg.TurboJPEGImageWriter;
import edu.illinois.library.cantaloupe.processor.codec.jpeg2000.JPEG2000KakaduImageReader;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import kdu_jni.KduException;
import kdu_jni.Kdu_global;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * <p>Processor using the Kakadu native library ({@literal libkdu}) via the
 * Java Native Interface (JNI).</p>
 *
 * <p>{@link JPEG2000KakaduImageReader} is used to acquire a scaled region of
 * interest that is {@link BufferedImage buffered in memory}, and Java 2D is
 * used for all post-scale processing steps. If libjpeg-turbo is {@link
 * TurboJPEGImageWriter#isTurboJPEGAvailable() available}, that is used to
 * write the result, otherwise Image I/O is used.</p>
 *
 * <h1>Usage</h1>
 *
 * <p>The Kakadu shared library and JNI binding (two separate files) must be
 * present on the library path, or else the {@literal -Djava.library.path} VM
 * argument must be provided at launch, with a value of the pathname of the
 * directory containing the library.</p>
 *
 * <h1>License</h1>
 *
 * <p>This software was developed using a Kakadu Public Service License
 * and may not be used commercially. See the
 * <a href="http://kakadusoftware.com/wp-content/uploads/2014/06/Kakadu-Licence-Terms-Feb-2018.pdf">
 * Kakadu Software License Terms and Conditions</a> for detailed terms.</p>
 *
 * @since 4.0
 * @author Alex Dolski UIUC
 */
class KakaduNativeProcessor implements FileProcessor, StreamProcessor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KakaduNativeProcessor.class);

    private static final AtomicBoolean IS_CLASS_INITIALIZED = new AtomicBoolean();

    private static String initializationError;

    private final JPEG2000KakaduImageReader reader =
            new JPEG2000KakaduImageReader();

    /**
     * Will be {@literal null} if {@link #streamFactory} isn't.
     */
    private Path sourceFile;

    /**
     * Will be {@literal null} if {@link #sourceFile} isn't.
     */
    private StreamFactory streamFactory;

    private static synchronized void initializeClass() {
        if (!IS_CLASS_INITIALIZED.get()) {
            IS_CLASS_INITIALIZED.set(true);
            try {
                Kdu_global.Kdu_get_core_version(); // call something trivial
            } catch (KduException | UnsatisfiedLinkError e) {
                initializationError = e.getMessage();
            }
        }
    }

    static synchronized void resetInitialization() {
        IS_CLASS_INITIALIZED.set(false);
    }

    @Override
    public void close() {
        reader.close();
    }

    @Override
    public Set<Format> getAvailableOutputFormats() {
        return ImageWriterFactory.supportedFormats();
    }

    @Override
    public String getInitializationError() {
        initializeClass();
        return initializationError;
    }

    @Override
    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public Format getSourceFormat() {
        return Format.get("jp2");
    }

    @Override
    public StreamFactory getStreamFactory() {
        return streamFactory;
    }

    @Override
    public boolean isSeeking() {
        return true;
    }

    @Override
    public void setSourceFile(Path file) {
        this.sourceFile = file;
        reader.setSource(file);
    }

    @Override
    public void setSourceFormat(Format format)
            throws SourceFormatException {
        if (!supportsSourceFormat(format)) {
            throw new SourceFormatException(format);
        }
    }

    @Override
    public void setStreamFactory(StreamFactory streamFactory) {
        this.streamFactory = streamFactory;
        try {
            reader.setSource(streamFactory.newSeekableStream());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public boolean supportsSourceFormat(Format format) {
        return Format.get("jp2").equals(format);
    }

    @Override
    public void process(final OperationList opList,
                        final Info info,
                        final OutputStream outputStream) throws FormatException, ProcessorException {
        final Rectangle roi       = getRegionBounds(opList, info.getSize());
        final Scale scaleOp       = (Scale) opList.getFirst(Scale.class);
        final ReductionFactor reductionFactor = new ReductionFactor();
        final double[] diffScales = new double[] { 1.0, 1.0 };

        try {
            final BufferedImage image = reader.readRegion(
                    roi, scaleOp, opList.getScaleConstraint(),
                    reductionFactor, diffScales);
            postProcess(image, opList, diffScales, info, reductionFactor,
                    outputStream);
        } catch (SourceFormatException e) {
            throw e;
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * Computes the effective size of a region of interest, excluding any scale
     * operations, in order to select the best decomposition level.
     */
    private static Rectangle getRegionBounds(OperationList opList,
                                             Dimension fullSize) {
        Rectangle regionRect = new Rectangle(0, 0,
                fullSize.width(), fullSize.height());
        Crop crop = (Crop) opList.getFirst(Crop.class);
        if (crop != null && crop.hasEffect(fullSize, opList)) {
            regionRect = crop.getRectangle(
                    fullSize, opList.getScaleConstraint());
        }
        return regionRect;
    }

    /**
     * @param image           Scaled image region to process.
     * @param opList          List of operations to apply to {@literal image}.
     * @param diffScales      Differential X and Y scales that have already
     *                        been applied to {@literal image}.
     * @param info            Information about the source image.
     * @param reductionFactor May be {@literal null}.
     * @param outputStream    Stream to write the resulting image to.
     */
    private void postProcess(BufferedImage image,
                             final OperationList opList,
                             final double[] diffScales,
                             final Info info,
                             ReductionFactor reductionFactor,
                             final OutputStream outputStream) throws IOException {
        if (reductionFactor == null) {
            reductionFactor = new ReductionFactor();
        }

        final Dimension fullSize = info.getSize();

        // Apply redactions.
        final Set<Redaction> redactions = opList.stream()
                .filter(op -> op instanceof Redaction)
                .filter(op -> op.hasEffect(fullSize, opList))
                .map(op -> (Redaction) op)
                .collect(Collectors.toSet());
        if (!redactions.isEmpty()) {
            Crop crop = (Crop) opList.getFirst(Crop.class);
            if (crop == null) {
                crop = new CropByPercent();
            }
            Java2DUtil.applyRedactions(
                    image, fullSize, crop, diffScales, reductionFactor,
                    opList.getScaleConstraint(), redactions);
        }

        // Apply remaining operations.
        for (Operation op : opList) {
            if (op.hasEffect(fullSize, opList)) {
                if (op instanceof Transpose) {
                    image = Java2DUtil.transpose(image, (Transpose) op);
                } else if (op instanceof Rotate) {
                    image = Java2DUtil.rotate(image, (Rotate) op);
                } else if (op instanceof ColorTransform) {
                    image = Java2DUtil.transformColor(image, (ColorTransform) op);
                } else if (op instanceof Sharpen) {
                    image = Java2DUtil.sharpen(image, (Sharpen) op);
                } else if (op instanceof Overlay) {
                    Java2DUtil.applyOverlay(image, (Overlay) op);
                }
            }
        }

        // Write the result.
        final Encode encode = (Encode) opList.getFirst(Encode.class);
        ImageWriterFacade.write(image, encode, outputStream);
    }

    @Override
    public Info readInfo() throws IOException {
        return Info.builder()
                .withFormat(Format.get("jp2"))
                .withSize(reader.getWidth(), reader.getHeight())
                .withTileSize(reader.getTileWidth(), reader.getTileHeight())
                .withNumResolutions(reader.getNumDecompositionLevels() + 1)
                .withMetadata(readMetadata())
                .build();
    }

    private Metadata readMetadata() throws IOException {
        final Metadata metadata = new Metadata();
        // EXIF
        byte[] bytes = reader.getEXIF();
        if (bytes != null) {
            try (edu.illinois.library.cantaloupe.image.exif.Reader reader =
                         new edu.illinois.library.cantaloupe.image.exif.Reader()) {
                reader.setSource(bytes);
                metadata.setEXIF(reader.read());
            }
        }
        // IPTC
        bytes = reader.getIPTC();
        if (bytes != null) {
            try (edu.illinois.library.cantaloupe.image.iptc.Reader iptcReader =
                         new edu.illinois.library.cantaloupe.image.iptc.Reader()) {
                iptcReader.setSource(bytes);
                metadata.setIPTC(iptcReader.read());
            }
        }
        // XMP
        metadata.setXMP(reader.getXMP());
        return metadata;
    }

}
